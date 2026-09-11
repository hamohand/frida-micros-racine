package com.muhend.backendai.service;

import com.muhend.backendai.dto.OcrCorrectionFieldDto;
import com.muhend.backendai.entities.DefuntEntity;
import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.entities.IdentitesEntity;
import com.muhend.backendai.repository.CalculRepo;
import com.muhend.backendai.repository.DefuntRepo;
import com.muhend.backendai.repository.FridaRepo;
import com.muhend.backendai.repository.HeritierRepo;
import com.muhend.backendai.repository.IdentitesRepo;
import com.muhend.backendai.service.pipeline.HeirPartCalculatorService;
import com.muhend.backendai.service.pipeline.MrzService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Écran de revue : la vérification phonétique à la volée ne doit signaler une incohérence
 * que si le service de translittération a réellement répondu.
 */
@ExtendWith(MockitoExtension.class)
class FridaServiceChampsSuspectsTest {

    @Mock private FridaRepo fridaRepo;
    @Mock private DefuntRepo defuntRepo;
    @Mock private CalculRepo calculRepo;
    @Mock private HeirPartCalculatorService heirPartCalculatorService;
    @Mock private HeritierRepo heritierRepo;
    @Mock private IdentitesRepo identitesRepo;
    @Mock private MrzService mrzService;

    private FridaService fridaService;

    @BeforeEach
    void setUp() {
        fridaService = new FridaService(fridaRepo, defuntRepo, calculRepo, heirPartCalculatorService,
                heritierRepo, identitesRepo, mrzService);

        IdentitesEntity identite = new IdentitesEntity();
        identite.setNom("منصوري");
        identite.setPrenom("أحمد");
        identite.setLatines("MANSOURI");
        identite.setPrenomLatines("AHMED");
        identite.setConfidencesJson("{\"nom\":1.0,\"prenom\":1.0}");
        DefuntEntity defunt = new DefuntEntity();
        defunt.setIdentite(identite);
        FridaEntity frida = new FridaEntity();
        frida.setDefunt(defunt);

        when(fridaRepo.findByNumFrida("N1")).thenReturn(Optional.of(frida));
        when(heritierRepo.listeHeritiers("N1")).thenReturn(List.of());
    }

    @Test
    void serviceIndisponible_NomEtPrenomNonSuspects() {
        // Cas réel du 2026-09-11 : la route de translittération répond 404
        when(mrzService.verifierTranslitteration(anyString(), anyString()))
                .thenReturn(MrzService.PhoneticResult.indisponible());

        List<OcrCorrectionFieldDto> champs = fridaService.getChampsSuspects("N1");

        for (String nomChamp : List.of("nom", "prenom")) {
            OcrCorrectionFieldDto champ = champ(champs, nomChamp);
            assertFalse(champ.isSuspect(), nomChamp + " ne doit pas être suspect : " + champ.getValidationReason());
            assertEquals(1.0, champ.getConfiance());
            assertFalse(champ.getValidationReason().contains("Incohérence"), champ.getValidationReason());
        }
    }

    @Test
    void vraiDesaccord_ToujoursSignale() {
        when(mrzService.verifierTranslitteration(anyString(), anyString()))
                .thenReturn(new MrzService.PhoneticResult(false, 35.0, "tst", "test"));

        List<OcrCorrectionFieldDto> champs = fridaService.getChampsSuspects("N1");

        OcrCorrectionFieldDto nom = champ(champs, "nom");
        assertTrue(nom.isSuspect());
        assertTrue(nom.getValidationReason().contains("Incohérence"), nom.getValidationReason());
    }

    private static OcrCorrectionFieldDto champ(List<OcrCorrectionFieldDto> champs, String nom) {
        return champs.stream().filter(c -> nom.equals(c.getChamp())).findFirst()
                .orElseThrow(() -> new AssertionError("champ absent : " + nom));
    }
}
