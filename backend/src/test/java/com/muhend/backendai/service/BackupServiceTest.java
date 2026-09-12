package com.muhend.backendai.service;

import com.muhend.backendai.dto.BackupInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sauvegardes au format de sauvegarder.bat : {@code <nom>/database.sql} + {@code <nom>/uploads}.
 * pg_dump et psql sont simulés : on vérifie les commandes lancées et les fichiers produits.
 */
class BackupServiceTest {

    @TempDir
    Path temp;

    private Path documents;
    private Path sauvegardes;
    private final List<List<String>> commandes = new ArrayList<>();
    private int codeRetour;
    private BackupService service;

    @BeforeEach
    void setUp() throws IOException {
        documents = Files.createDirectories(temp.resolve("uploads"));
        sauvegardes = temp.resolve("backups");
        service = new BackupService() {
            @Override
            int executer(List<String> commande, Path journal) throws IOException {
                commandes.add(commande);
                if (codeRetour != 0) {
                    Files.writeString(journal, "erreur simulée de " + commande.get(0));
                } else if (commande.get(0).equals("pg_dump")) {
                    Files.writeString(Paths.get(commande.get(commande.indexOf("-f") + 1)),
                            "CREATE TABLE public.frida (\n    id bigint\n);\n");
                }
                return codeRetour;
            }
        };
        configurer(sauvegardes);
    }

    private void configurer(Path racineSauvegardes) {
        ReflectionTestUtils.setField(service, "rootPath", documents.toString());
        ReflectionTestUtils.setField(service, "backupPathOverride", racineSauvegardes.toString());
        ReflectionTestUtils.setField(service, "dbHost", "db");
        ReflectionTestUtils.setField(service, "dbPort", "5432");
        ReflectionTestUtils.setField(service, "dbName", "frida_db");
        ReflectionTestUtils.setField(service, "dbUser", "frida");
        ReflectionTestUtils.setField(service, "dbPassword", "secret");
    }

    private static Path fichier(Path racine, String cheminRelatif, String contenu) throws IOException {
        Path f = racine.resolve(cheminRelatif);
        Files.createDirectories(f.getParent());
        return Files.writeString(f, contenu);
    }

    private Path sauvegardeExistante(String nom, long ageSecondes) throws IOException {
        Path base = fichier(sauvegardes, nom + "/database.sql", "CREATE TABLE public.frida (\n);\n");
        Files.setLastModifiedTime(base, FileTime.from(Instant.now().minusSeconds(ageSecondes)));
        return base.getParent();
    }

    private List<String> noms() {
        return service.listBackups().stream().map(BackupInfo::getFileName).toList();
    }

    @Test
    void creation_BaseEtDocuments_AuFormatDeSauvegarderBat() throws Exception {
        fichier(documents, "dossiers/fiche_1/01_en/acte.png", "image");

        BackupInfo info = service.createBackup(false);

        Path dossier = sauvegardes.resolve(info.getFileName());
        assertTrue(info.getFileName().startsWith("frida_backup_"), info.getFileName());
        assertFalse(info.isAutomatique());
        assertTrue(info.isDocumentsInclus());
        assertTrue(Files.isRegularFile(dossier.resolve("database.sql")));
        assertEquals("image", Files.readString(dossier.resolve("uploads/dossiers/fiche_1/01_en/acte.png")));
        assertTrue(commandes.get(0).containsAll(List.of("pg_dump", "--clean", "--if-exists")));
        try (Stream<Path> contenu = Files.list(sauvegardes)) {
            assertEquals(1, contenu.count(), "aucun dossier temporaire ne doit rester");
        }
    }

    @Test
    void creation_SauvegardesRangeesDansLesDocuments_NonRecopiees() throws Exception {
        // Poste de développement : sans BACKUP_PATH, les sauvegardes sont dans ROOT_PATH/backups
        Path sousDocuments = documents.resolve("backups");
        configurer(sousDocuments);
        fichier(documents, "dossiers/fiche_1/acte.png", "image");
        fichier(documents, "db_backups/backup_ancien.dump", "ancien format");
        fichier(sousDocuments, "frida_backup_20260101_000000/database.sql", "ancienne");

        BackupInfo info = service.createBackup(true);

        Path copie = sousDocuments.resolve(info.getFileName()).resolve("uploads");
        assertTrue(info.isAutomatique());
        assertTrue(info.getFileName().startsWith("frida_auto_"), info.getFileName());
        assertTrue(Files.exists(copie.resolve("dossiers/fiche_1/acte.png")));
        assertFalse(Files.exists(copie.resolve("backups")), "les sauvegardes ne se recopient pas elles-mêmes");
        assertFalse(Files.exists(copie.resolve("db_backups")));
    }

    @Test
    void echecDePgDump_AucuneSauvegardeLaissee() {
        codeRetour = 1;

        Exception e = assertThrows(IllegalStateException.class, () -> service.createBackup(false));

        assertTrue(e.getMessage().contains("erreur simulée de pg_dump"), e.getMessage());
        assertTrue(service.listBackups().isEmpty());
        assertEquals(0, sauvegardes.toFile().list().length);
    }

    @Test
    void liste_VoitLesSauvegardesDeSauvegarderBat_EtIgnoreLeReste() throws IOException {
        sauvegardeExistante("frida_backup_20260911_1833", 60);   // nom donné par sauvegarder.bat
        Files.createDirectories(sauvegardes.resolve("dossier_sans_base"));
        fichier(sauvegardes, ".frida_backup_20260912_101010.en-cours/database.sql", "partielle");
        fichier(sauvegardes, "backup_2026-09-11_16-33-26.dump", "ancien format");

        List<BackupInfo> liste = service.listBackups();

        assertEquals(List.of("frida_backup_20260911_1833"), liste.stream().map(BackupInfo::getFileName).toList());
        assertFalse(liste.get(0).isDocumentsInclus());
        assertFalse(liste.get(0).isAutomatique());
    }

    @Test
    void nomsDangereuxOuInconnus_Refuses() {
        for (String nom : new String[]{"..", "../etc", ".cache", "a/b", "a\\b", ""}) {
            assertThrows(IllegalArgumentException.class, () -> service.localiser(nom), nom);
        }
        assertThrows(NoSuchElementException.class, () -> service.localiser("frida_backup_inexistante"));
    }

    @Test
    void nettoyage_GardeLesManuellesEtLesPlusRecentesAutomatiques() throws IOException {
        sauvegardeExistante("frida_auto_20260901_080000", 40_000);
        sauvegardeExistante("frida_auto_20260902_080000", 30_000);
        sauvegardeExistante("frida_auto_20260903_080000", 20_000);
        sauvegardeExistante("frida_auto_20260904_080000", 10_000);
        sauvegardeExistante("frida_backup_20250101_0900", 90_000);

        int supprimees = service.nettoyerSauvegardesAutomatiques(2);

        assertEquals(2, supprimees);
        assertEquals(List.of("frida_auto_20260904_080000", "frida_auto_20260903_080000", "frida_backup_20250101_0900"),
                noms());
    }

    @Test
    void derniereSauvegarde_LaPlusRecenteToutesOrigines() throws IOException {
        assertTrue(service.derniereSauvegarde().isEmpty());
        sauvegardeExistante("frida_auto_20260901_080000", 50_000);
        sauvegardeExistante("frida_backup_20260902_0800", 100);

        assertTrue(service.derniereSauvegarde().orElseThrow().toInstant().isAfter(Instant.now().minusSeconds(200)));
    }

    @Test
    void prelude_SupprimeLesSeulsObjetsRecreesParLaSauvegarde() throws IOException {
        Path base = fichier(temp, "dump.sql", String.join("\n",
                "\\restrict cle",
                "CREATE SEQUENCE public.frida_seq",
                "CREATE TABLE public.frida (",
                "CREATE TABLE public.\"Mixte\" (",
                "CREATE VIEW public.vue AS",
                "COPY public.frida (id, note) FROM stdin;",
                "CREATE TABLE public.parametres (",   // une donnée de COPY, pas une instruction
                "\\.",
                ""));

        String prelude = service.construirePrelude(base);

        assertTrue(prelude.contains("DROP SEQUENCE IF EXISTS public.frida_seq CASCADE;"), prelude);
        assertTrue(prelude.contains("DROP TABLE IF EXISTS public.frida CASCADE;"), prelude);
        assertTrue(prelude.contains("DROP TABLE IF EXISTS public.\"Mixte\" CASCADE;"), prelude);
        assertTrue(prelude.contains("DROP VIEW IF EXISTS public.vue CASCADE;"), prelude);
        assertFalse(prelude.contains("parametres"), prelude);
    }

    @Test
    void restauration_UneSeuleTransaction_PuisDocuments() throws Exception {
        Path dossier = sauvegardeExistante("frida_backup_20260911_1833", 60);
        fichier(dossier, "uploads/dossiers/fiche_1/acte.png", "image sauvegardée");

        service.restoreBackup("frida_backup_20260911_1833");

        List<String> psql = commandes.get(0);
        assertEquals("psql", psql.get(0));
        assertTrue(psql.containsAll(List.of("--single-transaction", "ON_ERROR_STOP=1")), psql.toString());
        assertEquals(dossier.resolve("database.sql").toString(), psql.get(psql.size() - 1));
        assertEquals("image sauvegardée", Files.readString(documents.resolve("dossiers/fiche_1/acte.png")));
    }

    @Test
    void restauration_EchecDeLaBase_DocumentsIntacts() throws Exception {
        Path dossier = sauvegardeExistante("frida_backup_20260911_1833", 60);
        fichier(dossier, "uploads/dossiers/fiche_1/acte.png", "image sauvegardée");
        fichier(documents, "dossiers/fiche_1/acte.png", "image actuelle");
        codeRetour = 3;

        Exception e = assertThrows(IllegalStateException.class,
                () -> service.restoreBackup("frida_backup_20260911_1833"));

        assertTrue(e.getMessage().contains("rien n'a été modifié"), e.getMessage());
        assertEquals("image actuelle", Files.readString(documents.resolve("dossiers/fiche_1/acte.png")));
    }

    @Test
    void suppression_EffaceLeDossierEntier() throws Exception {
        Path dossier = sauvegardeExistante("frida_backup_20260911_1833", 60);
        fichier(dossier, "uploads/dossiers/fiche_1/acte.png", "image");

        service.deleteBackup("frida_backup_20260911_1833");

        assertFalse(Files.exists(dossier));
    }
}
