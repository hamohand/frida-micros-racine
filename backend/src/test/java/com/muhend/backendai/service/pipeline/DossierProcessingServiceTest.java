package com.muhend.backendai.service.pipeline;

import com.muhend.backendai.service.dossier.FolderService;

import com.muhend.backendai.client.ocr.dto.OcrEntityDefinitionDto;
import com.muhend.backendai.dto.DocumentInfo;
import com.muhend.backendai.entities.*;
import com.muhend.backendai.enums.DocumentType;
import com.muhend.backendai.enums.HeirCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DossierProcessingServiceTest {

    @Mock private FolderService folderService;
    @Mock private OcrMappingService ocrMappingService;
    @Mock private FridaIdentifierService fridaIdentifierService;
    @Mock private FridaPersistenceService fridaPersistenceService;

    @InjectMocks
    private DossierProcessingService dossierProcessingService;

    private String folderPath = "/frida-storage/test_folder";
    private Map<Path, DocumentInfo> fileDocInfoMap;
    private List<Path> pdfFiles;
    private FolderService.FolderScanResult scanResult;

    @BeforeEach
    void setUp() throws Exception {
        fileDocInfoMap = new HashMap<>();
        pdfFiles = new ArrayList<>();
        scanResult = new FolderService.FolderScanResult();

        // Initialisation manuelle du Semaphore pour le test unitaire
        org.springframework.test.util.ReflectionTestUtils.setField(dossierProcessingService, "maxParallelFolders", 2);
        dossierProcessingService.init();

        // Préparation du dossier mocké
        List<String> mockNumParente = List.of("1", "2", "3");
        scanResult.getTableauNumParente().addAll(mockNumParente);
        
        // Configuration lenient pour le cas où le test "WhenNoFiles" n'utilise pas le paramètre
        lenient().when(folderService.listFolderContents(anyString())).thenReturn(scanResult);

        // mergeOcrAndNfc (ajoute par 9654f25) n'etait pas simule : un mock renvoie null
        // par defaut, et plus aucun document n'etait sauvegarde.
        lenient().when(ocrMappingService.mergeOcrAndNfc(any(), any()))
                .thenAnswer(inv -> inv.getArgument(0) != null ? inv.getArgument(0) : inv.getArgument(1));
    }

    @Test
    void traiterExtraitsNaissance_ShouldProcessFilesAndSave() throws Exception {
        // ---- Arrange ----
        // Fichier 1: Défunt
        Path fileDefunt = Paths.get(folderPath, "1_en", "doc.pdf");
        DocumentInfo docDefunt = new DocumentInfo(HeirCategory.DEFUNT, DocumentType.EXTRAIT_NAISSANCE);
        fileDocInfoMap.put(fileDefunt, docDefunt);
        pdfFiles.add(fileDefunt);

        // Fichier 2: Conjoint
        Path fileConjoint = Paths.get(folderPath, "2_cni", "doc.pdf");
        DocumentInfo docConjoint = new DocumentInfo(HeirCategory.CONJOINT, DocumentType.CNI);
        fileDocInfoMap.put(fileConjoint, docConjoint);
        pdfFiles.add(fileConjoint);

        // Fichier 3: Fils (Garçon)
        Path fileGarcon = Paths.get(folderPath, "3_en", "doc.pdf");
        DocumentInfo docGarcon = new DocumentInfo(HeirCategory.ENFANT, DocumentType.EXTRAIT_NAISSANCE);
        fileDocInfoMap.put(fileGarcon, docGarcon);
        pdfFiles.add(fileGarcon);

        scanResult.getFileDocumentInfoMap().putAll(fileDocInfoMap);
        scanResult.getPdfFiles().addAll(pdfFiles);

        // Mocks OCR Mapping
        OcrEntityDefinitionDto entityDef = new OcrEntityDefinitionDto();
        when(ocrMappingService.getOrCacheEntityDef(any(), any(), any())).thenReturn(entityDef);

        // Mock Identités
        IdentitesEntity idDefunt = new IdentitesEntity();
        idDefunt.setSexe("ذكر"); // Homme
        idDefunt.setDateNaissance(LocalDate.of(1950, 1, 1));
        when(ocrMappingService.processFile(eq(fileDefunt), any(), any(), eq(DocumentType.EXTRAIT_NAISSANCE), eq("rapide"))).thenReturn(idDefunt);

        IdentitesEntity idConjoint = new IdentitesEntity();
        idConjoint.setSexe("أنثى"); // Femme
        when(ocrMappingService.processFile(eq(fileConjoint), any(), any(), eq(DocumentType.CNI), eq("rapide"))).thenReturn(idConjoint);

        IdentitesEntity idGarcon = new IdentitesEntity();
        idGarcon.setSexe("ذكر"); // Garçon
        when(ocrMappingService.processFile(eq(fileGarcon), any(), any(), eq(DocumentType.EXTRAIT_NAISSANCE), eq("rapide"))).thenReturn(idGarcon);

        // Mock Identifiant
        when(fridaIdentifierService.genererIdentifiant(anyString())).thenReturn("FRIDA-12345");

        // ---- Act ----
        FridaEntity result = dossierProcessingService.traiterExtraitsNaissance(folderPath, "rapide");

        // ---- Assert ----
        assertNotNull(result, "La fiche Frida ne doit pas être null");
        // Le numero Frida est porte par le contexte. FridaPersistenceService, simule ici,
        // le recopie ensuite sur la fiche : le lire sur la fiche revenait a tester le mock.
        ArgumentCaptor<TraitementContext> ctxCaptor = ArgumentCaptor.forClass(TraitementContext.class);
        verify(fridaPersistenceService).sauvegarderBrouillonFrida(ctxCaptor.capture());
        assertEquals("FRIDA-12345", ctxCaptor.getValue().getNumFrida());

        // Vérification : sauvegarderDocument appelé pour chaque fichier (3 fois)
        verify(fridaPersistenceService, times(3)).sauvegarderDocument(
                any(TraitementContext.class), any(IdentitesEntity.class),
                any(HeirCategory.class), anyString());

        // Vérification : brouillon sauvegardé une fois
        verify(fridaPersistenceService, times(1)).sauvegarderBrouillonFrida(any(TraitementContext.class));
    }

    @Test
    void traiterExtraitsNaissance_WhenNoFiles_ShouldReturnNull() throws Exception {
        scanResult.getPdfFiles().clear();

        FridaEntity result = dossierProcessingService.traiterExtraitsNaissance(folderPath, "rapide");

        assertNull(result);
        verify(fridaPersistenceService, never()).sauvegarderDocument(any(), any(), any(), anyString());
        verify(fridaPersistenceService, never()).sauvegarderBrouillonFrida(any());
    }

    // ------------------------------------------------------------------
    // Non-regression du 2026-07-02 (commit 9654f25) : les fichiers etaient
    // regroupes par categorie, si bien que plusieurs enfants deposes dans le
    // meme dossier "03_en_..." s'ecrasaient et qu'un seul etait traite.
    // Fils et filles partagent ce dossier : le sexe vient du QR code.
    // ------------------------------------------------------------------

    @Test
    void deuxFilsEtUneFilleDansLaMemeCategorie_LesTroisEnfantsSontTraites() throws Exception {
        Path defunt = ajouterFichier("1_en_en_01", "100_defunt.jpg", HeirCategory.DEFUNT, DocumentType.EXTRAIT_NAISSANCE);
        Path fils1 = ajouterFichier("03_en_en_01", "200_fils1.jpg", HeirCategory.ENFANT, DocumentType.EXTRAIT_NAISSANCE);
        Path fils2 = ajouterFichier("03_en_en_01", "201_fils2.jpg", HeirCategory.ENFANT, DocumentType.EXTRAIT_NAISSANCE);
        Path fille = ajouterFichier("03_en_en_01", "202_fille.jpg", HeirCategory.ENFANT, DocumentType.EXTRAIT_NAISSANCE);
        publierScan();
        simulerDefinitionEtIdentifiant();

        IdentitesEntity idFille = identite("أنثى");
        lenient().when(ocrMappingService.processFile(eq(defunt), isNull(), any(), any(), eq("rapide"))).thenReturn(identite("ذكر"));
        lenient().when(ocrMappingService.processFile(eq(fils1), isNull(), any(), any(), eq("rapide"))).thenReturn(identite("ذكر"));
        lenient().when(ocrMappingService.processFile(eq(fils2), isNull(), any(), any(), eq("rapide"))).thenReturn(identite("ذكر"));
        lenient().when(ocrMappingService.processFile(eq(fille), isNull(), any(), any(), eq("rapide"))).thenReturn(idFille);

        dossierProcessingService.traiterExtraitsNaissance(folderPath, "rapide");

        verify(fridaPersistenceService, times(3)).sauvegarderDocument(
                any(TraitementContext.class), any(IdentitesEntity.class), eq(HeirCategory.ENFANT), eq("03"));
        verify(fridaPersistenceService).sauvegarderDocument(
                any(TraitementContext.class), same(idFille), eq(HeirCategory.ENFANT), eq("03"));
    }

    @Test
    void deuxCniAvecVersoDansLaMemeCategorie_ChaqueVersoRejointSonRecto() throws Exception {
        Path rectoA = ajouterFichier("03_cni_cni_01", "300_cniA.jpg", HeirCategory.ENFANT, DocumentType.CNI);
        Path rectoB = ajouterFichier("03_cni_cni_01", "301_cniB.jpg", HeirCategory.ENFANT, DocumentType.CNI);
        // Ordre volontairement croise : l'appariement ne doit pas dependre de l'ordre
        Path versoB = ajouterFichier("03_cni_cni_01_verso", "302_cniB_verso.png", HeirCategory.ENFANT, DocumentType.CNI);
        Path versoA = ajouterFichier("03_cni_cni_01_verso", "303_cniA_verso.png", HeirCategory.ENFANT, DocumentType.CNI);
        publierScan();
        simulerDefinitionEtIdentifiant();
        lenient().when(ocrMappingService.processFile(any(), any(), any(), any(), eq("rapide"))).thenReturn(identite("ذكر"));

        dossierProcessingService.traiterExtraitsNaissance(folderPath, "rapide");

        verify(ocrMappingService).processFile(eq(rectoA), eq(versoA), any(), eq(DocumentType.CNI), eq("rapide"));
        verify(ocrMappingService).processFile(eq(rectoB), eq(versoB), any(), eq(DocumentType.CNI), eq("rapide"));
        verify(fridaPersistenceService, times(2)).sauvegarderDocument(
                any(TraitementContext.class), any(IdentitesEntity.class), eq(HeirCategory.ENFANT), eq("03"));
    }

    @Test
    void versoAmbiguAvecPlusieursRectos_AucunAppariementAuHasard() throws Exception {
        Path rectoA = ajouterFichier("03_cni_cni_01", "400_scan1.jpg", HeirCategory.ENFANT, DocumentType.CNI);
        Path rectoB = ajouterFichier("03_cni_cni_01", "401_scan2.jpg", HeirCategory.ENFANT, DocumentType.CNI);
        ajouterFichier("03_cni_cni_01_verso", "402_photo_verso.png", HeirCategory.ENFANT, DocumentType.CNI);
        publierScan();
        simulerDefinitionEtIdentifiant();
        lenient().when(ocrMappingService.processFile(any(), any(), any(), any(), eq("rapide"))).thenReturn(identite("ذكر"));

        dossierProcessingService.traiterExtraitsNaissance(folderPath, "rapide");

        // Mieux vaut perdre une lecture MRZ que lire la CNI d'une autre personne
        verify(ocrMappingService).processFile(eq(rectoA), isNull(), any(), any(), eq("rapide"));
        verify(ocrMappingService).processFile(eq(rectoB), isNull(), any(), any(), eq("rapide"));
        verify(fridaPersistenceService, times(2)).sauvegarderDocument(
                any(TraitementContext.class), any(IdentitesEntity.class), eq(HeirCategory.ENFANT), eq("03"));
    }

    @Test
    void unSeulRectoEtUnSeulVersoDansLaCategorie_ApparieMemeSiLesNomsDifferent() throws Exception {
        // Cas des fichiers deposes avant que le verso soit nomme d'apres son recto
        Path recto = ajouterFichier("02_cni_cni_01", "500_scan.jpg", HeirCategory.CONJOINT, DocumentType.CNI);
        Path verso = ajouterFichier("02_cni_cni_01_verso", "501_autre_verso.png", HeirCategory.CONJOINT, DocumentType.CNI);
        publierScan();
        simulerDefinitionEtIdentifiant();
        lenient().when(ocrMappingService.processFile(any(), any(), any(), any(), eq("rapide"))).thenReturn(identite("أنثى"));

        dossierProcessingService.traiterExtraitsNaissance(folderPath, "rapide");

        verify(ocrMappingService).processFile(eq(recto), eq(verso), any(), eq(DocumentType.CNI), eq("rapide"));
        verify(fridaPersistenceService, times(1)).sauvegarderDocument(
                any(TraitementContext.class), any(IdentitesEntity.class), eq(HeirCategory.CONJOINT), eq("02"));
    }

    @Test
    void dumpNfcSeul_EstTraiteCommeUnePersonne() throws Exception {
        Path nfc = ajouterFichier("02_cni_cni_01", "600_nfc_dump_123.json", HeirCategory.CONJOINT, DocumentType.CNI);
        publierScan();
        simulerDefinitionEtIdentifiant();
        lenient().when(ocrMappingService.processFile(eq(nfc), isNull(), isNull(), any(), eq("rapide"))).thenReturn(identite("أنثى"));

        dossierProcessingService.traiterExtraitsNaissance(folderPath, "rapide");

        verify(fridaPersistenceService, times(1)).sauvegarderDocument(
                any(TraitementContext.class), any(IdentitesEntity.class), eq(HeirCategory.CONJOINT), eq("02"));
    }

    // ---- Utilitaires ----

    private Path ajouterFichier(String dossier, String nom, HeirCategory categorie, DocumentType type) {
        Path p = Paths.get(folderPath, dossier, nom);
        fileDocInfoMap.put(p, new DocumentInfo(categorie, type));
        pdfFiles.add(p);
        return p;
    }

    private void publierScan() {
        scanResult.getFileDocumentInfoMap().putAll(fileDocInfoMap);
        scanResult.getPdfFiles().addAll(pdfFiles);
    }

    private void simulerDefinitionEtIdentifiant() {
        lenient().when(ocrMappingService.getOrCacheEntityDef(any(), any(), any())).thenReturn(new OcrEntityDefinitionDto());
        lenient().when(fridaIdentifierService.genererIdentifiant(anyString())).thenReturn("FRIDA-TEST");
    }

    private static IdentitesEntity identite(String sexe) {
        IdentitesEntity id = new IdentitesEntity();
        id.setSexe(sexe);
        return id;
    }
}
