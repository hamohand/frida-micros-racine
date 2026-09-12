package com.muhend.backendai.service;

import com.muhend.backendai.dto.BackupInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Sauvegardes de la base de données et des documents.
 *
 * Une sauvegarde est un dossier {@code <dossier des sauvegardes>/<nom>/} contenant
 * {@code database.sql} (pg_dump en SQL) et {@code uploads/} (copie des documents) : le format
 * de sauvegarder.bat et restaurer.bat de l'installation notaire. Les sauvegardes faites depuis
 * l'écran et celles des scripts sont donc les mêmes, visibles des deux côtés.
 */
@Profile("!calc-only")
@Service
@Slf4j
public class BackupService {

    static final String FICHIER_BASE = "database.sql";
    static final String DOSSIER_DOCUMENTS = "uploads";
    static final String PREFIXE_MANUELLE = "frida_backup_";
    static final String PREFIXE_AUTOMATIQUE = "frida_auto_";
    static final String PREFIXE_AVANT_RESTAURATION = "frida_avant_restauration_";

    /** Ancien dossier des sauvegardes .dump de l'écran (avant le 2026-09-12) : jamais recopié. */
    private static final String ANCIEN_DOSSIER_DUMPS = "db_backups";
    private static final String SUFFIXE_EN_COURS = ".en-cours";

    /** Ni séparateur ni point en tête : un nom ne peut pas sortir du dossier des sauvegardes. */
    private static final Pattern NOM_VALIDE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,150}");
    private static final Pattern CREATION_OBJET = Pattern.compile(
            "^CREATE (TABLE|SEQUENCE|VIEW|MATERIALIZED VIEW) (public\\.(?:\"[^\"]+\"|[A-Za-z0-9_]+))");
    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Value("${ROOT_PATH:/app/uploads}")
    private String rootPath;

    @Value("${app.backup.path:}")
    private String backupPathOverride;

    @Value("${DB_HOST:db}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private String dbPort;

    @Value("${DB_NAME:fridaocrdb}")
    private String dbName;

    @Value("${DB_USER:postgres}")
    private String dbUser;

    @Value("${DB_PASSWORD:password}")
    private String dbPassword;

    /** Création, restauration et suppression ne se chevauchent jamais. */
    private final Object verrou = new Object();

    private Path getBackupDir() {
        Path path = (backupPathOverride != null && !backupPathOverride.isBlank())
                ? Paths.get(backupPathOverride)
                : Paths.get(rootPath, "backups");
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            log.error("Impossible de créer le dossier des sauvegardes {}", path, e);
        }
        return path;
    }

    public List<BackupInfo> listBackups() {
        try (Stream<Path> dossiers = Files.list(getBackupDir())) {
            return dossiers
                    .filter(Files::isDirectory)
                    .filter(d -> NOM_VALIDE.matcher(d.getFileName().toString()).matches())
                    .filter(d -> Files.isRegularFile(d.resolve(FICHIER_BASE)))
                    .map(this::lireInfo)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(BackupInfo::getCreatedAt).reversed())
                    .toList();
        } catch (IOException e) {
            log.error("Erreur lors du listage des sauvegardes", e);
            return new ArrayList<>();
        }
    }

    /** Date de la sauvegarde la plus récente, qu'elle vienne de l'écran, du backend ou de sauvegarder.bat. */
    public Optional<OffsetDateTime> derniereSauvegarde() {
        return listBackups().stream().map(BackupInfo::getCreatedAt).max(Comparator.naturalOrder());
    }

    public BackupInfo createBackup(boolean automatique) throws Exception {
        return creer(automatique ? PREFIXE_AUTOMATIQUE : PREFIXE_MANUELLE);
    }

    private BackupInfo creer(String prefixe) throws Exception {
        synchronized (verrou) {
            Path racine = getBackupDir();
            String horodatage = LocalDateTime.now().format(HORODATAGE);
            String nom = prefixe + horodatage;
            // Deux sauvegardes dans la même seconde (ex. restauration juste après une autre)
            for (int rang = 2; Files.exists(racine.resolve(nom)) || Files.exists(racine.resolve("." + nom + SUFFIXE_EN_COURS)); rang++) {
                nom = prefixe + horodatage + "_" + rang;
            }
            Path cible = racine.resolve(nom);
            // Écrite sous un nom masqué puis renommée : jamais listée à moitié faite
            Path enCours = racine.resolve("." + nom + SUFFIXE_EN_COURS);
            Files.createDirectories(enCours);
            try {
                Path journal = Files.createTempFile("frida-sauvegarde-", ".log");
                try {
                    int code = executer(List.of("pg_dump", "-h", dbHost, "-p", dbPort, "-U", dbUser, "-d", dbName,
                            "--clean", "--if-exists", "-f", enCours.resolve(FICHIER_BASE).toString()), journal);
                    if (code != 0) {
                        throw new IllegalStateException("pg_dump a échoué (code " + code + ") : " + finJournal(journal));
                    }
                } finally {
                    Files.deleteIfExists(journal);
                }
                copierDocuments(Paths.get(rootPath), enCours.resolve(DOSSIER_DOCUMENTS), racine);
                renommer(enCours, cible);
            } catch (Exception e) {
                try {
                    supprimerDossier(enCours);
                } catch (IOException suppression) {
                    e.addSuppressed(suppression);
                }
                throw e;
            }
            log.info("Sauvegarde {} créée", nom);
            return lireInfo(cible);
        }
    }

    /**
     * Revient à l'état de la sauvegarde : base et documents à l'identique. Les dossiers créés ou
     * modifiés depuis sont donc retirés ; pour que rien ne soit perdu, l'état actuel est d'abord
     * sauvegardé ({@code frida_avant_restauration_...}), et restaurer celle-ci annule l'opération.
     *
     * @return nom de la sauvegarde de sécurité
     */
    public String restoreBackup(String nom) throws Exception {
        synchronized (verrou) {
            Path dossier = localiser(nom);
            Path base = dossier.resolve(FICHIER_BASE);

            // 1. État actuel mis de côté : si cela échoue, rien n'est modifié
            String securite = creer(PREFIXE_AVANT_RESTAURATION).getFileName();

            // 2. Base, en une seule transaction : à la moindre erreur, elle reste telle qu'avant
            Path prelude = Files.createTempFile("frida-restauration-", ".sql");
            Path journal = Files.createTempFile("frida-restauration-", ".log");
            try {
                Files.writeString(prelude, construirePrelude(base), StandardCharsets.UTF_8);
                int code = executer(List.of("psql", "-X", "-q", "-h", dbHost, "-p", dbPort, "-U", dbUser,
                        "-d", dbName, "-v", "ON_ERROR_STOP=1", "--single-transaction",
                        "-f", prelude.toString(), "-f", base.toString()), journal);
                if (code != 0) {
                    String cause = finJournal(journal);
                    // Rien n'a changé : la sauvegarde de sécurité n'a pas lieu d'être
                    supprimerDossier(getBackupDir().resolve(securite));
                    throw new IllegalStateException("Restauration de la base refusée, rien n'a été modifié : " + cause);
                }
            } finally {
                Files.deleteIfExists(prelude);
                Files.deleteIfExists(journal);
            }

            // 3. Documents à l'identique de la sauvegarde, si elle en contient
            Path documents = dossier.resolve(DOSSIER_DOCUMENTS);
            if (Files.isDirectory(documents)) {
                try {
                    synchroniserDocuments(documents, Paths.get(rootPath), getBackupDir());
                } catch (IOException e) {
                    throw new IllegalStateException("Base restaurée, mais la remise en place des documents a échoué ("
                            + e.getMessage() + "). L'état précédent est dans la sauvegarde " + securite + ".", e);
                }
            }
            log.info("Sauvegarde {} restaurée (base{}), état précédent dans {}",
                    nom, Files.isDirectory(documents) ? " et documents" : "", securite);
            return securite;
        }
    }

    public void deleteBackup(String nom) throws IOException {
        synchronized (verrou) {
            supprimerDossier(localiser(nom));
            log.info("Sauvegarde {} supprimée", nom);
        }
    }

    /**
     * Supprime les sauvegardes automatiques au-delà des {@code aConserver} plus récentes.
     * Les sauvegardes manuelles (écran ou sauvegarder.bat) ne sont jamais supprimées.
     */
    public int nettoyerSauvegardesAutomatiques(int aConserver) {
        synchronized (verrou) {
            List<BackupInfo> automatiques = listBackups().stream().filter(BackupInfo::isAutomatique).toList();
            int garde = Math.min(Math.max(aConserver, 1), automatiques.size());
            int supprimees = 0;
            for (BackupInfo ancienne : automatiques.subList(garde, automatiques.size())) {
                try {
                    supprimerDossier(getBackupDir().resolve(ancienne.getFileName()));
                    supprimees++;
                    log.info("Ancienne sauvegarde automatique supprimée : {}", ancienne.getFileName());
                } catch (IOException e) {
                    log.error("Impossible de supprimer la sauvegarde {}", ancienne.getFileName(), e);
                }
            }
            return supprimees;
        }
    }

    /**
     * Dossier d'une sauvegarde existante.
     *
     * @throws IllegalArgumentException nom invalide (séparateur, point en tête...)
     * @throws NoSuchElementException   aucune sauvegarde de ce nom
     */
    public Path localiser(String nom) {
        if (nom == null || !NOM_VALIDE.matcher(nom).matches()) {
            throw new IllegalArgumentException("Nom de sauvegarde invalide : " + nom);
        }
        Path racine = getBackupDir().toAbsolutePath().normalize();
        Path dossier = racine.resolve(nom).normalize();
        if (!racine.equals(dossier.getParent()) || !Files.isRegularFile(dossier.resolve(FICHIER_BASE))) {
            throw new NoSuchElementException("Sauvegarde introuvable : " + nom);
        }
        return dossier;
    }

    /** Informations d'une sauvegarde existante (mêmes contrôles que {@link #localiser}). */
    public BackupInfo informations(String nom) {
        BackupInfo info = lireInfo(localiser(nom));
        if (info == null) {
            throw new NoSuchElementException("Sauvegarde illisible : " + nom);
        }
        return info;
    }

    /** Écrit la sauvegarde en .zip dans le flux (téléchargement). */
    public void zipper(Path dossier, OutputStream sortie) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(sortie);
        try (Stream<Path> chemins = Files.walk(dossier)) {
            for (Path chemin : (Iterable<Path>) chemins::iterator) {
                if (!Files.isRegularFile(chemin)) {
                    continue;
                }
                String entree = dossier.getFileName() + "/" + dossier.relativize(chemin).toString().replace('\\', '/');
                zip.putNextEntry(new ZipEntry(entree));
                Files.copy(chemin, zip);
                zip.closeEntry();
            }
        }
        zip.finish();
    }

    /**
     * SQL exécuté avant la sauvegarde : supprime les tables, séquences et vues qu'elle recrée, et
     * elles seules. Une sauvegarde de sauvegarder.bat (sans --clean) se restaure ainsi dans la base
     * en marche, et une table ajoutée depuis (ex. parametres) est conservée.
     */
    String construirePrelude(Path base) throws IOException {
        Set<String> suppressions = new LinkedHashSet<>();
        try (BufferedReader lecteur = new BufferedReader(
                new InputStreamReader(Files.newInputStream(base), StandardCharsets.UTF_8))) {
            boolean donneesCopy = false;
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                if (donneesCopy) {
                    donneesCopy = !ligne.equals("\\.");
                    continue;
                }
                if (ligne.startsWith("COPY ") && ligne.endsWith("FROM stdin;")) {
                    donneesCopy = true;
                    continue;
                }
                Matcher creation = CREATION_OBJET.matcher(ligne);
                if (creation.find()) {
                    suppressions.add("DROP " + creation.group(1) + " IF EXISTS " + creation.group(2) + " CASCADE;");
                }
            }
        }
        StringBuilder sql = new StringBuilder("SET client_min_messages = warning;\n");
        suppressions.forEach(suppression -> sql.append(suppression).append('\n'));
        return sql.toString();
    }

    /** Lance pg_dump ou psql ; la sortie va dans un journal pour que le processus ne bloque jamais. */
    int executer(List<String> commande, Path journal) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(commande);
        pb.environment().put("PGPASSWORD", dbPassword);
        pb.redirectErrorStream(true);
        pb.redirectOutput(journal.toFile());
        return pb.start().waitFor();
    }

    private BackupInfo lireInfo(Path dossier) {
        String nom = dossier.getFileName().toString();
        try {
            return BackupInfo.builder()
                    .fileName(nom)
                    .sizeBytes(taille(dossier))
                    .createdAt(OffsetDateTime.ofInstant(
                            Files.getLastModifiedTime(dossier.resolve(FICHIER_BASE)).toInstant(),
                            ZoneId.systemDefault()))
                    .automatique(nom.startsWith(PREFIXE_AUTOMATIQUE))
                    .avantRestauration(nom.startsWith(PREFIXE_AVANT_RESTAURATION))
                    .documentsInclus(Files.isDirectory(dossier.resolve(DOSSIER_DOCUMENTS)))
                    .build();
        } catch (IOException e) {
            log.warn("Sauvegarde {} illisible, ignorée", nom, e);
            return null;
        }
    }

    private static long taille(Path dossier) throws IOException {
        try (Stream<Path> fichiers = Files.walk(dossier)) {
            return fichiers.filter(Files::isRegularFile).mapToLong(f -> f.toFile().length()).sum();
        }
    }

    /** Copie récursive, sans le dossier {@code exclu} (les sauvegardes elles-mêmes) ni l'ancien db_backups. */
    private static void copierDocuments(Path source, Path destination, Path exclu) throws IOException {
        if (!Files.isDirectory(source)) {
            return;
        }
        Path depart = source.toAbsolutePath().normalize();
        Path dossierExclu = exclu == null ? null : exclu.toAbsolutePath().normalize();
        Files.walkFileTree(depart, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dossier, BasicFileAttributes attributs) throws IOException {
                boolean ancienDossierDumps = depart.equals(dossier.getParent())
                        && dossier.getFileName().toString().equals(ANCIEN_DOSSIER_DUMPS);
                if (dossier.equals(dossierExclu) || ancienDossierDumps) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Files.createDirectories(destination.resolve(depart.relativize(dossier).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path fichier, BasicFileAttributes attributs) throws IOException {
                Files.copy(fichier, destination.resolve(depart.relativize(fichier).toString()),
                        StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Rend {@code destination} identique à {@code source} : copie, puis retrait de ce qui n'existe pas
     * dans la source. Le dossier {@code exclu} (les sauvegardes, s'il est dans les documents) et
     * l'ancien db_backups ne sont jamais retirés.
     */
    private static void synchroniserDocuments(Path source, Path destination, Path exclu) throws IOException {
        copierDocuments(source, destination, null);
        Path origine = source.toAbsolutePath().normalize();
        Path cible = destination.toAbsolutePath().normalize();
        Path dossierExclu = exclu.toAbsolutePath().normalize();
        Files.walkFileTree(cible, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dossier, BasicFileAttributes attributs) throws IOException {
                if (dossier.equals(cible)) {
                    return FileVisitResult.CONTINUE;
                }
                boolean ancienDossierDumps = cible.equals(dossier.getParent())
                        && dossier.getFileName().toString().equals(ANCIEN_DOSSIER_DUMPS);
                if (dossier.equals(dossierExclu) || ancienDossierDumps) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (!Files.isDirectory(origine.resolve(cible.relativize(dossier).toString()))) {
                    supprimerDossier(dossier);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path fichier, BasicFileAttributes attributs) throws IOException {
                if (!Files.exists(origine.resolve(cible.relativize(fichier).toString()))) {
                    Files.delete(fichier);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void renommer(Path source, Path cible) throws IOException {
        try {
            Files.move(source, cible, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, cible);
        }
    }

    private static void supprimerDossier(Path dossier) throws IOException {
        if (!Files.exists(dossier)) {
            return;
        }
        try (Stream<Path> chemins = Files.walk(dossier)) {
            for (Path chemin : (Iterable<Path>) chemins.sorted(Comparator.reverseOrder())::iterator) {
                Files.delete(chemin);
            }
        }
    }

    private static String finJournal(Path journal) {
        try {
            String contenu = new String(Files.readAllBytes(journal), StandardCharsets.UTF_8).strip();
            return contenu.length() > 800 ? "…" + contenu.substring(contenu.length() - 800) : contenu;
        } catch (IOException e) {
            return "(journal illisible)";
        }
    }
}
