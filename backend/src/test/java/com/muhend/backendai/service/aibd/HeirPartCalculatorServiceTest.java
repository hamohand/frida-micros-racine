package com.muhend.backendai.service.aibd;

import com.muhend.backendai.client.calculs.CalculsApiClient;
import com.muhend.backendai.client.calculs.dto.CalculFractionDto;
import com.muhend.backendai.client.calculs.dto.CalculHeritierDto;
import com.muhend.backendai.client.calculs.dto.CalculRequestDto;
import com.muhend.backendai.client.calculs.dto.CalculResponseDto;
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
    private CalculsApiClient calculsApiClient;

    @InjectMocks
    private HeirPartCalculatorService calculatorService;

    private TraitementContext ctx;
    private CalculResponseDto mockResponse;

    @Captor
    private ArgumentCaptor<CalculRequestDto> requestCaptor;

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

        // Configuration de la réponse mockée de l'API
        mockResponse = new CalculResponseDto();
        mockResponse.setDenominateurCommun(40);
        
        CalculHeritierDto conjointDto = new CalculHeritierDto("conjoint survivant / épouse", new CalculFractionDto(5, 40));
        CalculHeritierDto garconDto = new CalculHeritierDto("2 fils (garçons)", new CalculFractionDto(14, 40)); // chaque garçon aura 14
        CalculHeritierDto filleDto = new CalculHeritierDto("fille", new CalculFractionDto(7, 40));

        mockResponse.setHeritiers(List.of(conjointDto, garconDto, filleDto));
    }

    @Test
    void calculerParts_ShouldCallApiWithCorrectParameters() {
        when(calculsApiClient.calculerParts(any(CalculRequestDto.class))).thenReturn(mockResponse);

        calculatorService.calculerParts(ctx);

        verify(calculsApiClient).calculerParts(requestCaptor.capture());
        CalculRequestDto request = requestCaptor.getValue();

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
        when(calculsApiClient.calculerParts(any(CalculRequestDto.class))).thenReturn(mockResponse);

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
    void calculerParts_WhenApiThrowsException_ShouldWrapAndRethrow() {
        when(calculsApiClient.calculerParts(any(CalculRequestDto.class)))
                .thenThrow(new RuntimeException("API Down"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            calculatorService.calculerParts(ctx);
        });

        assertTrue(exception.getMessage().contains("Erreur microservice calculs"));
        assertEquals("API Down", exception.getCause().getMessage());
    }

    @Test
    void calculerCoefficient_ShouldCalculateCorrectPercentage() {
        // Test cas normal: 5/40 = 0.125 = 12.5% -> retour attendu 0.13 (arrondi à 2 décimales)
        // La méthode float Math.round(0.125 * 100) / 100.0f donne 0.13f
        assertEquals(0.13f, calculatorService.calculerCoefficient(5, 40));
        
        // 14/40 = 0.35 -> retour 0.35
        assertEquals(0.35f, calculatorService.calculerCoefficient(14, 40));
        
        // 7/40 = 0.175 -> retour 0.18 (arrondi)
        assertEquals(0.18f, calculatorService.calculerCoefficient(7, 40));
    }

    @Test
    void calculerCoefficient_WhenDenominatorIsZero_ShouldReturnZero() {
        assertEquals(0f, calculatorService.calculerCoefficient(5, 0));
    }
}
