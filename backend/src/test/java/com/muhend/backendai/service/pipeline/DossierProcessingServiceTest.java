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
        when(ocrMappingService.processFile(eq(fileDefunt), any(), eq(DocumentType.EXTRAIT_NAISSANCE), eq("rapide"))).thenReturn(idDefunt);

        IdentitesEntity idConjoint = new IdentitesEntity();
        idConjoint.setSexe("أنثى"); // Femme
        when(ocrMappingService.processFile(eq(fileConjoint), any(), eq(DocumentType.CNI), eq("rapide"))).thenReturn(idConjoint);

        IdentitesEntity idGarcon = new IdentitesEntity();
        idGarcon.setSexe("ذكر"); // Garçon
        when(ocrMappingService.processFile(eq(fileGarcon), any(), eq(DocumentType.EXTRAIT_NAISSANCE), eq("rapide"))).thenReturn(idGarcon);

        // Mock Identifiant
        when(fridaIdentifierService.genererIdentifiant(anyString())).thenReturn("FRIDA-12345");

        // ---- Act ----
        FridaEntity result = dossierProcessingService.traiterExtraitsNaissance(folderPath, "rapide");

        // ---- Assert ----
        assertNotNull(result, "La fiche Frida ne doit pas être null");
        assertEquals("FRIDA-12345", result.getNumFrida());

        // Vérification : sauvegarderDocument appelé pour chaque fichier (3 fois)
        verify(fridaPersistenceService, times(3)).sauvegarderDocument(
                any(TraitementContext.class), any(IdentitesEntity.class),
                any(HeirCategory.class), anyInt());

        // Vérification : brouillon sauvegardé une fois
        verify(fridaPersistenceService, times(1)).sauvegarderBrouillonFrida(any(TraitementContext.class));
    }

    @Test
    void traiterExtraitsNaissance_WhenNoFiles_ShouldReturnNull() throws Exception {
        scanResult.getPdfFiles().clear();

        FridaEntity result = dossierProcessingService.traiterExtraitsNaissance(folderPath, "rapide");

        assertNull(result);
        verify(fridaPersistenceService, never()).sauvegarderDocument(any(), any(), any(), anyInt());
        verify(fridaPersistenceService, never()).sauvegarderBrouillonFrida(any());
    }
}
