package com.muhend.backendai.service;

import com.muhend.backendai.entities.ParametreEntity;
import com.muhend.backendai.repository.ParametreRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParametreServiceTest {

    @Mock
    private ParametreRepo parametreRepo;

    private ParametreService parametreService;

    @BeforeEach
    void setUp() {
        parametreService = new ParametreService(parametreRepo);
    }

    @Test
    void verificationPhonetique_DesactiveeParDefaut() {
        when(parametreRepo.findById(ParametreService.VERIFICATION_PHONETIQUE)).thenReturn(Optional.empty());

        assertFalse(parametreService.isVerificationPhonetiqueActive());
    }

    @Test
    void verificationPhonetique_LitLaValeurEnregistree() {
        when(parametreRepo.findById(ParametreService.VERIFICATION_PHONETIQUE))
                .thenReturn(Optional.of(new ParametreEntity(ParametreService.VERIFICATION_PHONETIQUE, "true")));

        assertTrue(parametreService.isVerificationPhonetiqueActive());
    }

    @Test
    void verificationPhonetique_EnregistreLaValeur() {
        parametreService.setVerificationPhonetiqueActive(true);

        ArgumentCaptor<ParametreEntity> captor = ArgumentCaptor.forClass(ParametreEntity.class);
        verify(parametreRepo).save(captor.capture());
        assertEquals(ParametreService.VERIFICATION_PHONETIQUE, captor.getValue().getCle());
        assertEquals("true", captor.getValue().getValeur());
    }
}
