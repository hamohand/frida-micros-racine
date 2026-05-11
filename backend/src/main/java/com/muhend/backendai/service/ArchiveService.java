package com.muhend.backendai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.muhend.backendai.dto.ArchiveInfo;
import com.muhend.backendai.dto.FridaDetailsDTO;
import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.repository.FridaRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class ArchiveService {

    @Value("${ROOT_PATH}")
    private String rootPath;

    @Value("${app.archive.path:}")
    private String archivePathOverride;

    @Value("${app.archive.threshold-months:6}")
    private int archiveThresholdMonths;

    private final FridaRepo fridaRepo;

    private static final String ARCHIVE_DIR = "archives";
    private static final String METADATA_FILE = "frida_data.json";

    public ArchiveService(FridaRepo fridaRepo) {
        this.fridaRepo = fridaRepo;
    }

    /**
     * Retourne le chemin du dossier d'archives.
     * Utilise ARCHIVE_PATH s'il est défini, sinon ROOT_PATH/archives
     */
    private Path getArchiveDir() {
        Path path;
        if (archivePathOverride != null && !archivePathOverride.isBlank()) {
            path = Paths.get(archivePathOverride);
        } else {
            path = Paths.get(rootPath, ARCHIVE_DIR);
        }
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            log.error("Impossible de créer le dossier d'archives: {}", path, e);
        }
        return path;
    }

    /**
     * Liste toutes les archives disponibles.
     */
    public List<ArchiveInfo> listArchives() {
        try (Stream<Path> paths = Files.list(getArchiveDir())) {
            return paths
                    .filter(p -> p.toString().endsWith(".zip"))
                    .map(this::readArchiveInfo)
                    .filter(Objects::nonNull)
                    .sorted((a, b) -> b.getDateArchivage().compareTo(a.getDateArchivage()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Erreur lors du listage des archives", e);
            return new ArrayList<>();
        }
    }

    /**
     * Liste les dossiers Frida éligibles à l'archivage (plus anciens que le seuil).
     */
    public List<FridaDetailsDTO> getArchivableFridas() {
        LocalDate threshold = LocalDate.now().minusMonths(archiveThresholdMonths);
        return fridaRepo.findAllFridas().stream()
                .filter(f -> f.getDateCreation() != null && f.getDateCreation().isBefore(threshold))
                .collect(Collectors.toList());
    }

    /**
     * Archive un dossier Frida : exporte les données + fichiers OCR dans un .zip,
     * puis supprime le dossier de la base active.
     */
    @Transactional
    public ArchiveInfo archiveFrida(String numFrida) throws Exception {
        // 1. Charger le dossier complet
        FridaEntity frida = fridaRepo.findByNumFrida(numFrida)
                .orElseThrow(() -> new IllegalArgumentException("Frida introuvable: " + numFrida));

        // 2. Préparer le nom de l'archive
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String defuntNom = frida.getDefunt() != null && frida.getDefunt().getIdentite() != null
                ? frida.getDefunt().getIdentite().getNom() : "inconnu";
        String safeNom = defuntNom.replaceAll("[^a-zA-Z0-9\\u0600-\\u06FF]", "_");
        String archiveFileName = String.format("archive_%s_%s_%s.zip", numFrida, safeNom, timestamp);
        Path archiveFile = getArchiveDir().resolve(archiveFileName);

        // 3. Chercher le dossier OCR associé (par nom du défunt)
        Path ocrFolder = findOcrFolder(frida);

        // 4. Créer l'archive ZIP
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(archiveFile.toFile()))) {
            // Ajouter les données JSON
            zos.putNextEntry(new ZipEntry(METADATA_FILE));
            byte[] jsonBytes = mapper.writeValueAsBytes(frida);
            zos.write(jsonBytes);
            zos.closeEntry();

            // Ajouter les fichiers OCR si trouvés
            boolean hasFiles = false;
            if (ocrFolder != null && Files.exists(ocrFolder)) {
                hasFiles = true;
                addFolderToZip(zos, ocrFolder, "fichiers/");
            }

            log.info("Archive créée: {} (fichiers OCR: {})", archiveFileName, hasFiles);
        }

        // 5. Supprimer le dossier OCR du stockage actif
        if (ocrFolder != null && Files.exists(ocrFolder)) {
            deleteDirectory(ocrFolder);
            log.info("Dossier OCR supprimé: {}", ocrFolder);
        }

        // 6. Supprimer de la base de données active
        fridaRepo.delete(frida);
        log.info("Frida {} supprimée de la base active", numFrida);

        // 7. Retourner les infos
        File file = archiveFile.toFile();
        return ArchiveInfo.builder()
                .fileName(archiveFileName)
                .numFrida(numFrida)
                .nomDefunt(defuntNom)
                .prenomDefunt(frida.getDefunt() != null && frida.getDefunt().getIdentite() != null
                        ? frida.getDefunt().getIdentite().getPrenom() : "")
                .dateCreationFrida(frida.getDateCreation())
                .dateArchivage(LocalDateTime.now())
                .sizeBytes(file.length())
                .includesFiles(ocrFolder != null)
                .build();
    }

    /**
     * Restaure un dossier Frida depuis une archive vers la base active.
     */
    @Transactional
    public void restoreFromArchive(String archiveFileName) throws Exception {
        Path archiveFile = getArchiveDir().resolve(archiveFileName);
        if (!Files.exists(archiveFile)) {
            throw new IllegalArgumentException("Archive introuvable: " + archiveFileName);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archiveFile.toFile()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(METADATA_FILE)) {
                    // Restaurer les données en base
                    byte[] jsonBytes = zis.readAllBytes();
                    FridaEntity frida = mapper.readValue(jsonBytes, FridaEntity.class);

                    // Vérifier qu'elle n'existe pas déjà
                    if (fridaRepo.findByNumFrida(frida.getNumFrida()).isPresent()) {
                        throw new IllegalStateException(
                                "Le dossier Frida " + frida.getNumFrida() + " existe déjà dans la base active. "
                                + "Supprimez-le d'abord ou renommez-le.");
                    }

                    // Nettoyer les IDs pour insertion comme nouvelles entités
                    clearIds(frida);
                    fridaRepo.save(frida);
                    log.info("Frida {} restaurée en base", frida.getNumFrida());

                } else if (entry.getName().startsWith("fichiers/")) {
                    // Restaurer les fichiers OCR
                    String relativePath = entry.getName().substring("fichiers/".length());
                    if (!relativePath.isEmpty()) {
                        Path targetFile = Paths.get(rootPath).resolve(relativePath);
                        if (entry.isDirectory()) {
                            Files.createDirectories(targetFile);
                        } else {
                            Files.createDirectories(targetFile.getParent());
                            Files.copy(zis, targetFile, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
                zis.closeEntry();
            }
        }

        log.info("Archive {} restaurée avec succès", archiveFileName);
    }

    /**
     * Supprime une archive.
     */
    public void deleteArchive(String archiveFileName) throws IOException {
        Path archiveFile = getArchiveDir().resolve(archiveFileName);
        if (Files.exists(archiveFile)) {
            Files.delete(archiveFile);
            log.info("Archive supprimée: {}", archiveFileName);
        }
    }

    /**
     * Retourne le chemin de téléchargement d'une archive.
     */
    public Path getArchiveFilePath(String archiveFileName) {
        return getArchiveDir().resolve(archiveFileName);
    }

    /**
     * Archivage automatique : archive tous les dossiers plus vieux que le seuil.
     * Retourne le nombre de dossiers archivés.
     */
    @Transactional
    public int autoArchive() {
        List<FridaDetailsDTO> archivable = getArchivableFridas();
        int count = 0;
        for (FridaDetailsDTO dto : archivable) {
            try {
                archiveFrida(dto.getNumFrida());
                count++;
            } catch (Exception e) {
                log.error("Erreur lors de l'archivage automatique de {}", dto.getNumFrida(), e);
            }
        }
        log.info("Archivage automatique terminé: {} dossier(s) archivé(s)", count);
        return count;
    }

    // ============ Méthodes utilitaires ============

    /**
     * Cherche le dossier OCR associé à un dossier Frida.
     * Convention de nommage: nomprenom + date (format yyyyMMdd)
     */
    private Path findOcrFolder(FridaEntity frida) {
        if (frida.getDefunt() == null || frida.getDefunt().getIdentite() == null) {
            return null;
        }
        String nom = frida.getDefunt().getIdentite().getNom();
        String prenom = frida.getDefunt().getIdentite().getPrenom();
        if (nom == null || prenom == null) return null;

        String folderPrefix = (nom + prenom).toLowerCase().replaceAll("\\s+", "");
        Path rootDir = Paths.get(rootPath);

        try (Stream<Path> paths = Files.list(rootDir)) {
            return paths
                    .filter(Files::isDirectory)
                    .filter(p -> {
                        String dirName = p.getFileName().toString().toLowerCase();
                        return dirName.startsWith(folderPrefix);
                    })
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            log.warn("Impossible de chercher le dossier OCR pour {}", nom, e);
            return null;
        }
    }

    /**
     * Ajoute récursivement un dossier dans un ZIP.
     */
    private void addFolderToZip(ZipOutputStream zos, Path folder, String prefix) throws IOException {
        String folderName = folder.getFileName().toString();
        try (Stream<Path> paths = Files.walk(folder)) {
            paths.forEach(path -> {
                try {
                    String entryName = prefix + folderName + "/" +
                            folder.relativize(path).toString().replace("\\", "/");
                    if (Files.isDirectory(path)) {
                        zos.putNextEntry(new ZipEntry(entryName + "/"));
                        zos.closeEntry();
                    } else {
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    }
                } catch (IOException e) {
                    log.error("Erreur lors de l'ajout au ZIP: {}", path, e);
                }
            });
        }
    }

    /**
     * Supprime récursivement un dossier.
     */
    private void deleteDirectory(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.error("Erreur lors de la suppression: {}", path, e);
                        }
                    });
        }
    }

    /**
     * Remet les IDs à null pour forcer la création de nouvelles entités lors de la restauration.
     */
    private void clearIds(FridaEntity frida) {
        frida.setId(null);
        if (frida.getDefunt() != null) {
            frida.getDefunt().setId(null);
            if (frida.getDefunt().getIdentite() != null) {
                frida.getDefunt().getIdentite().setId(null);
            }
        }
        if (frida.getCalcul() != null) {
            frida.getCalcul().setId(null);
        }
        if (frida.getHeritiers() != null) {
            frida.getHeritiers().forEach(h -> {
                h.setId(null);
                if (h.getIdentite() != null) h.getIdentite().setId(null);
            });
        }
        if (frida.getTemoins() != null) {
            frida.getTemoins().forEach(t -> {
                t.setId(null);
                if (t.getIdentite() != null) t.getIdentite().setId(null);
            });
        }
    }

    /**
     * Lit les métadonnées d'un fichier archive sans tout décompresser.
     */
    private ArchiveInfo readArchiveInfo(Path archivePath) {
        File file = archivePath.toFile();
        String fileName = file.getName();

        // Extraire les métadonnées du nom de fichier: archive_NUMFRIDA_NOM_TIMESTAMP.zip
        String[] parts = fileName.replace("archive_", "").replace(".zip", "").split("_", 3);
        String numFrida = parts.length > 0 ? parts[0] : "?";
        String nom = parts.length > 1 ? parts[1] : "?";

        return ArchiveInfo.builder()
                .fileName(fileName)
                .numFrida(numFrida)
                .nomDefunt(nom)
                .dateArchivage(LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(file.lastModified()),
                        ZoneId.systemDefault()))
                .sizeBytes(file.length())
                .build();
    }
}
