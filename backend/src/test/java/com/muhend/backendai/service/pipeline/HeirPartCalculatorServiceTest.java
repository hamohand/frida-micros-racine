package com.muhend.backendai.service.pipeline;

import com.muhend.backendai.calculs.enums.HeirType;
import com.muhend.backendai.calculs.model.FamilyRequest;
import com.muhend.backendai.calculs.model.Fraction;
import com.muhend.backendai.calculs.model.Heritier;
import com.muhend.backendai.calculs.service.CalculPartsService;
import com.muhend.backendai.entities.CalculEntity;
import com.muhend.backendai.entities.DefuntEntity;
import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.entities.IdentitesEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeirPartCalculatorServiceTest {

    @Mock
    private CalculPartsService calculPartsService;

    @InjectMocks
    private HeirPartCalculatorService calculatorService;

    private TraitementContext ctx;
    private List<Heritier> mockResponse;

    @Captor
    private ArgumentCaptor<FamilyRequest> requestCaptor;

    @BeforeEach
    void setUp() {
        ctx = new TraitementContext();
        ctx.setNumFrida("123456789");

        // Configuration d'un défunt (homme)
        FridaEntity fridaEntity = new FridaEntity();
        DefuntEntity defunt = new DefuntEntity();
        IdentitesEntity identiteDefunt = new IdentitesEntity();
        identiteDefunt.setSexe("ذكر"); // Homme
        defunt.setIdentite(identiteDefunt);
        fridaEntity.setDefunt(defunt);
        ctx.setFicheFrida(fridaEntity);

        // Simulation d'une famille: 1 épouse, 2 garçons, 1 fille
        ctx.setNbConjoints(1);
        ctx.setNbGarcons(2);
        ctx.setNbFilles(1);
        // Pas de parents/fratrie pour simplifier ce test

        // Configuration de la réponse mockée
        Heritier conjoint = new Heritier(HeirType.SPOUSE, new Fraction(5, 40));
        Heritier garcon = new Heritier(HeirType.SON, new Fraction(14, 40));
        Heritier fille = new Heritier(HeirType.DAUGHTER, new Fraction(7, 40));

        mockResponse = List.of(conjoint, garcon, fille);
    }

    @Test
    void calculerParts_ShouldCallServiceWithCorrectParameters() {
        when(calculPartsService.calculParts(any(FamilyRequest.class))).thenReturn(mockResponse);

        calculatorService.calculerParts(ctx);

        verify(calculPartsService).calculParts(requestCaptor.capture());
        FamilyRequest request = requestCaptor.getValue();

        assertEquals("M", request.getSexeDefunt());
        assertEquals(1, request.getNbConjoints());
        assertEquals(2, request.getNbGarcons());
        assertEquals(1, request.getNbFilles());
        assertFalse(request.isPereVivant());
        assertFalse(request.isMereVivante());
        assertEquals(0, request.getNbFreres());
        assertEquals(0, request.getNbSoeurs());
    }

    @Test
    void calculerParts_ShouldMapResponseToCalculEntity() {
        when(calculPartsService.calculParts(any(FamilyRequest.class))).thenReturn(mockResponse);

        CalculEntity result = calculatorService.calculerParts(ctx);

        assertNotNull(result);
        assertEquals("123456789", result.getNumFrida());
        assertEquals(1, result.getNbConjoints());
        assertEquals(2, result.getNbGarcons());
        assertEquals(1, result.getNbFilles());
        
        assertEquals(40, result.getDenominateur());
        assertEquals(5, result.getNumerateurConjoint());
        assertEquals(14, result.getNumerateurGarcons());
        assertEquals(7, result.getNumerateurFilles());
    }

    @Test
    void calculerParts_WhenServiceThrowsException_ShouldWrapAndRethrow() {
        when(calculPartsService.calculParts(any(FamilyRequest.class)))
                .thenThrow(new RuntimeException("Moteur Down"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            calculatorService.calculerParts(ctx);
        });

        assertTrue(exception.getMessage().contains("Erreur calcul interne"));
        assertEquals("Moteur Down", exception.getCause().getMessage());
    }

    @Test
    void calculerCoefficient_ShouldCalculateCorrectPercentage() {
        assertEquals(0.13f, calculatorService.calculerCoefficient(5, 40));
        assertEquals(0.35f, calculatorService.calculerCoefficient(14, 40));
        assertEquals(0.18f, calculatorService.calculerCoefficient(7, 40));
    }

    @Test
    void calculerCoefficient_WhenDenominatorIsZero_ShouldReturnZero() {
        assertEquals(0f, calculatorService.calculerCoefficient(5, 0));
    }
}
