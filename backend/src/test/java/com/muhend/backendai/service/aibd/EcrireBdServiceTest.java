package com.muhend.backendai.service.aibd;

import com.muhend.backendai.client.ocr.dto.OcrEntityDefinitionDto;
import com.muhend.backendai.dto.DocumentInfo;
import com.muhend.backendai.entities.*;
import com.muhend.backendai.enums.DocumentType;
import com.muhend.backendai.enums.HeirCategory;
import com.muhend.backendai.repository.*;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EcrireBdServiceTest {

    @Mock private LectureAiService lectureAiService;
    @Mock private OcrMappingService ocrMappingService;
    @Mock private FridaIdentifierService fridaIdentifierService;
    @Mock private HeirPartCalculatorService heirPartCalculatorService;

    @Mock private IdentitesRepo identitesRepo;
    @Mock private FridaRepo fridaRepo;
    @Mock private HeritierRepo heritierRepo;
    @Mock private DefuntRepo defuntRepo;
    @Mock private CalculRepo calculRepo;
    @Mock private TemoinRepo temoinRepo;

    @InjectMocks
    private EcrireBdService ecrireBdService;

    private String folderPath = "/frida-storage/test_folder";
    private Map<Path, DocumentInfo> fileDocInfoMap;
    private List<Path> pdfFiles;
    private LectureAiService.FolderScanResult scanResult;

    @BeforeEach
    void setUp() throws Exception {
        fileDocInfoMap = new HashMap<>();
        pdfFiles = new ArrayList<>();
        scanResult = new LectureAiService.FolderScanResult();

        // Initialisation manuelle du Semaphore pour le test unitaire
        org.springframework.test.util.ReflectionTestUtils.setField(ecrireBdService, "maxParallelFolders", 2);
        ecrireBdService.init();

        // Préparation du dossier mocké
        List<String> mockNumParente = List.of("1", "2", "3");
        scanResult.getTableauNumParente().addAll(mockNumParente);
        
        // Configuration lenient pour le cas où le test "WhenNoFiles" n'utilise pas le paramètre
        lenient().when(lectureAiService.listFolderContents(anyString())).thenReturn(scanResult);
    }

    @Test
    void traiterExtraitsNaissance_ShouldProcessFilesAndSaveToDb() throws Exception {
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
        when(ocrMappingService.getOrCacheEntityDef(any(), any())).thenReturn(entityDef);

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

        // Mock Calculs
        CalculEntity mockCalcul = new CalculEntity();
        mockCalcul.setDenominateur(8);
        mockCalcul.setNumerateurConjoint(1);
        mockCalcul.setNumerateurGarcons(7); // Pour simplifier
        when(heirPartCalculatorService.calculerParts(any())).thenReturn(mockCalcul);
        when(heirPartCalculatorService.calculerCoefficient(anyInt(), anyInt())).thenReturn(0.125f);

        // ---- Act ----
        FridaEntity result = ecrireBdService.traiterExtraitsNaissance(folderPath, "rapide");

        // ---- Assert ----
        assertNotNull(result, "La fiche Frida ne doit pas être null");
        assertEquals("FRIDA-12345", result.getNumFrida());

        // Vérification des appels de sauvegarde Identites
        verify(identitesRepo, times(3)).save(any(IdentitesEntity.class));
        
        // Vérification des appels de sauvegarde Defunt et Heritiers
        verify(defuntRepo, times(1)).save(any(DefuntEntity.class));
        verify(heritierRepo, times(2)).save(any(HeritierEntity.class)); // 1 conjoint, 1 enfant
        
        // Vérification de la création de la fiche finale
        verify(fridaRepo, times(1)).save(any(FridaEntity.class));
        
        // Vérification de l'appel au service de calcul
        verify(heirPartCalculatorService, times(1)).calculerParts(any(TraitementContext.class));
        verify(calculRepo, times(1)).save(any(CalculEntity.class));
    }

    @Test
    void traiterExtraitsNaissance_WhenNoFiles_ShouldReturnNull() throws Exception {
        scanResult.getPdfFiles().clear();

        FridaEntity result = ecrireBdService.traiterExtraitsNaissance(folderPath, "rapide");

        assertNull(result);
        verify(identitesRepo, never()).save(any());
        verify(fridaRepo, never()).save(any());
    }
}
