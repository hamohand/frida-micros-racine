package com.muhend.backendai.service.pipeline;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NinValidationServiceTest {

    private final NinValidationService ninValidationService = new NinValidationService();

    @Test
    void testCleanAndValidate_ValidNin() {
        // Le NIN de l'utilisateur qui a été testé sur la CNI
        String validNin = "109560504000021800";
        assertEquals(validNin, ninValidationService.cleanAndValidate(validNin));
    }

    @Test
    void testCleanAndValidate_CleanableErrors() {
        // Erreurs OCR classiques : 'O' au lieu de '0', espaces, tirets
        String ocrErrorNin = " 10956O5Q40-00021800 ";
        String expectedNin = "109560504000021800";
        
        assertEquals(expectedNin, ninValidationService.cleanAndValidate(ocrErrorNin));
    }
    
    @Test
    void testCleanAndValidate_CleanableLetters() {
        // I, l -> 1, S -> 5, Z -> 2, B -> 8
        String ocrErrorNin = "lO9S60S040000Z1B00";
        String expectedNin = "109560504000021800";
        
        assertEquals(expectedNin, ninValidationService.cleanAndValidate(ocrErrorNin));
    }

    @Test
    void testCleanAndValidate_InvalidLength() {
        // Trop court (17 chiffres)
        String shortNin = "10956050400002180";
        assertNull(ninValidationService.cleanAndValidate(shortNin));

        // Trop long (19 chiffres)
        String longNin = "1095605040000218001";
        assertNull(ninValidationService.cleanAndValidate(longNin));
    }

    @Test
    void testCleanAndValidate_UnrecoverableCharacters() {
        // Contient des lettres non corrigeables (ex: X, Y, M)
        String invalidNin = "10956X504000021800";
        assertNull(ninValidationService.cleanAndValidate(invalidNin));
    }

    @Test
    void testCleanAndValidate_NullOrEmpty() {
        assertNull(ninValidationService.cleanAndValidate(null));
        assertNull(ninValidationService.cleanAndValidate(""));
        assertNull(ninValidationService.cleanAndValidate("   "));
    }
}
