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
    private int codeRetourPgDump;
    private int codeRetourPsql;
    private BackupService service;

    @BeforeEach
    void setUp() throws IOException {
        documents = Files.createDirectories(temp.resolve("uploads"));
        sauvegardes = temp.resolve("backups");
        service = new BackupService() {
            @Override
            int executer(List<String> commande, Path journal) throws IOException {
                commandes.add(commande);
                boolean pgDump = commande.get(0).equals("pg_dump");
                int code = pgDump ? codeRetourPgDump : codeRetourPsql;
                if (code != 0) {
                    Files.writeString(journal, "erreur simulée de " + commande.get(0));
                } else if (pgDump) {
                    Files.writeString(Paths.get(commande.get(commande.indexOf("-f") + 1)),
                            "CREATE TABLE public.frida (\n    id bigint\n);\n");
                }
                return code;
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

    private List<String> commande(String programme) {
        return commandes.stream().filter(c -> c.get(0).equals(programme)).findFirst().orElseThrow();
    }

    @Test
    void creation_BaseEtDocuments_AuFormatDeSauvegarderBat() throws Exception {
        fichier(documents, "dossiers/fiche_1/01_en/acte.png", "image");

        BackupInfo info = service.createBackup(false);

        Path dossier = sauvegardes.resolve(info.getFileName());
        assertTrue(info.getFileName().startsWith("frida_backup_"), info.getFileName());
        assertFalse(info.isAutomatique());
        assertFalse(info.isAvantRestauration());
        assertTrue(info.isDocumentsInclus());
        assertTrue(Files.isRegularFile(dossier.resolve("database.sql")));
        assertEquals("image", Files.readString(dossier.resolve("uploads/dossiers/fiche_1/01_en/acte.png")));
        assertTrue(commande("pg_dump").containsAll(List.of("--clean", "--if-exists")));
        try (Stream<Path> contenu = Files.list(sauvegardes)) {
            assertEquals(1, contenu.count(), "aucun dossier temporaire ne doit rester");
        }
    }

    @Test
    void deuxSauvegardesDansLaMemeSeconde_NomsDistincts() throws Exception {
        String premiere = service.createBackup(false).getFileName();
        String seconde = service.createBackup(false).getFileName();

        assertNotEquals(premiere, seconde);
        assertEquals(2, noms().size());
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
        codeRetourPgDump = 1;

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
        sauvegardeExistante("frida_avant_restauration_20250102_0900", 80_000);

        int supprimees = service.nettoyerSauvegardesAutomatiques(2);

        assertEquals(2, supprimees);
        assertEquals(List.of("frida_auto_20260904_080000", "frida_auto_20260903_080000",
                "frida_avant_restauration_20250102_0900", "frida_backup_20250101_0900"), noms());
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
    void restauration_EtatActuelSauvegardeAvant_PuisBaseEnUneTransaction() throws Exception {
        sauvegardeExistante("frida_backup_20260911_1833", 60);
        fichier(documents, "dossiers/fiche_1/acte.png", "image actuelle");

        String securite = service.restoreBackup("frida_backup_20260911_1833");

        assertTrue(securite.startsWith("frida_avant_restauration_"), securite);
        assertEquals("image actuelle",
                Files.readString(sauvegardes.resolve(securite).resolve("uploads/dossiers/fiche_1/acte.png")));
        assertEquals("pg_dump", commandes.get(0).get(0), "l'état actuel est sauvegardé avant toute modification");
        List<String> psql = commandes.get(1);
        assertEquals("psql", psql.get(0));
        assertTrue(psql.containsAll(List.of("--single-transaction", "ON_ERROR_STOP=1")), psql.toString());
        assertTrue(service.listBackups().stream()
                .anyMatch(b -> b.getFileName().equals(securite) && b.isAvantRestauration() && !b.isAutomatique()));
    }

    @Test
    void restauration_DocumentsRemisALIdentique_AjoutsRetiresMaisConservesDansLaSecurite() throws Exception {
        Path dossier = sauvegardeExistante("frida_backup_20260911_1833", 60);
        fichier(dossier, "uploads/dossiers/fiche_1/acte.png", "image sauvegardée");
        fichier(dossier, "uploads/dossiers/fiche_supprimee/acte.png", "fiche supprimée depuis");
        fichier(documents, "dossiers/fiche_1/acte.png", "image modifiée depuis");
        fichier(documents, "dossiers/fiche_1/ajout.png", "fichier ajouté depuis");
        fichier(documents, "dossiers/fiche_ajoutee/acte.png", "fiche ajoutée depuis");

        String securite = service.restoreBackup("frida_backup_20260911_1833");

        assertEquals("image sauvegardée", Files.readString(documents.resolve("dossiers/fiche_1/acte.png")));
        assertEquals("fiche supprimée depuis", Files.readString(documents.resolve("dossiers/fiche_supprimee/acte.png")));
        assertFalse(Files.exists(documents.resolve("dossiers/fiche_1/ajout.png")));
        assertFalse(Files.exists(documents.resolve("dossiers/fiche_ajoutee")));
        Path copieSecurite = sauvegardes.resolve(securite).resolve("uploads");
        assertEquals("fiche ajoutée depuis", Files.readString(copieSecurite.resolve("dossiers/fiche_ajoutee/acte.png")));
        assertEquals("fichier ajouté depuis", Files.readString(copieSecurite.resolve("dossiers/fiche_1/ajout.png")));
    }

    @Test
    void restauration_SauvegardeSansDocuments_DocumentsNonTouches() throws Exception {
        sauvegardeExistante("frida_backup_20260911_1833", 60);
        fichier(documents, "dossiers/fiche_1/acte.png", "image actuelle");

        service.restoreBackup("frida_backup_20260911_1833");

        assertEquals("image actuelle", Files.readString(documents.resolve("dossiers/fiche_1/acte.png")));
    }

    @Test
    void restauration_SauvegardesRangeesDansLesDocuments_JamaisRetirees() throws Exception {
        Path sousDocuments = documents.resolve("backups");
        configurer(sousDocuments);
        Path dossier = fichier(sousDocuments, "frida_backup_20260911_1833/database.sql", "CREATE TABLE public.frida (\n);\n").getParent();
        fichier(dossier, "uploads/dossiers/fiche_1/acte.png", "image sauvegardée");
        fichier(sousDocuments, "frida_backup_20260910_0900/database.sql", "autre sauvegarde");
        fichier(documents, "db_backups/backup_ancien.dump", "ancien format");

        service.restoreBackup("frida_backup_20260911_1833");

        assertTrue(Files.exists(sousDocuments.resolve("frida_backup_20260910_0900/database.sql")));
        assertTrue(Files.exists(documents.resolve("db_backups/backup_ancien.dump")));
        assertEquals("image sauvegardée", Files.readString(documents.resolve("dossiers/fiche_1/acte.png")));
    }

    @Test
    void restauration_EchecDeLaBase_RienNeChange_NiSauvegardeDeSecuriteLaissee() throws Exception {
        Path dossier = sauvegardeExistante("frida_backup_20260911_1833", 60);
        fichier(dossier, "uploads/dossiers/fiche_1/acte.png", "image sauvegardée");
        fichier(documents, "dossiers/fiche_1/acte.png", "image actuelle");
        fichier(documents, "dossiers/fiche_ajoutee/acte.png", "fiche ajoutée depuis");
        codeRetourPsql = 3;

        Exception e = assertThrows(IllegalStateException.class,
                () -> service.restoreBackup("frida_backup_20260911_1833"));

        assertTrue(e.getMessage().contains("rien n'a été modifié"), e.getMessage());
        assertEquals("image actuelle", Files.readString(documents.resolve("dossiers/fiche_1/acte.png")));
        assertTrue(Files.exists(documents.resolve("dossiers/fiche_ajoutee/acte.png")));
        assertEquals(List.of("frida_backup_20260911_1833"), noms());
    }

    @Test
    void restauration_EchecDeLaSauvegardeDeSecurite_RienNestTouche() throws Exception {
        Path dossier = sauvegardeExistante("frida_backup_20260911_1833", 60);
        fichier(dossier, "uploads/dossiers/fiche_1/acte.png", "image sauvegardée");
        fichier(documents, "dossiers/fiche_1/acte.png", "image actuelle");
        codeRetourPgDump = 1;

        assertThrows(IllegalStateException.class, () -> service.restoreBackup("frida_backup_20260911_1833"));

        assertTrue(commandes.stream().noneMatch(c -> c.get(0).equals("psql")), "la base n'est pas touchée");
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
