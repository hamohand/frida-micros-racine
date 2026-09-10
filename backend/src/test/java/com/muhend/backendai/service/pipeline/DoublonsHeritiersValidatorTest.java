package com.muhend.backendai.service.pipeline;

import com.muhend.backendai.calculs.exception.InvalidFamilyCompositionException;
import com.muhend.backendai.entities.DefuntEntity;
import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.entities.HeritierEntity;
import com.muhend.backendai.entities.IdentitesEntity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DoublonsHeritiersValidatorTest {

    private static final String NIN_A = "109871234567891234";
    private static final String NIN_B = "209871234567895678";
    private static final String NIN_DEFUNT = "119501234567890001";

    private final DoublonsHeritiersValidator validator =
            new DoublonsHeritiersValidator(new NinValidationService());

    @Test
    void heritiersDistincts_AucuneErreur() {
        FridaEntity frida = fiche(NIN_DEFUNT, heritier("03", NIN_A, "Ali"), heritier("03", NIN_B, "Amina"));

        assertDoesNotThrow(() -> validator.verifier(frida));
    }

    @Test
    void memeNinPourDeuxHeritiers_CalculRefuse() {
        FridaEntity frida = fiche(NIN_DEFUNT, heritier("03", NIN_A, "Ali"), heritier("03", NIN_A, "Omar"));

        InvalidFamilyCompositionException ex = assertThrows(InvalidFamilyCompositionException.class,
                () -> validator.verifier(frida));

        assertTrue(ex.getMessage().contains("Ali") && ex.getMessage().contains("Omar"), ex.getMessage());
        assertTrue(ex.getMessage().contains("1234"), ex.getMessage());
        assertFalse(ex.getMessage().contains(NIN_A), "le NIN complet ne doit pas figurer dans le message");
    }

    @Test
    void defuntParmiSesPropresHeritiers_CalculRefuse() {
        FridaEntity frida = fiche(NIN_A, heritier("03", NIN_A, "Ali"));

        InvalidFamilyCompositionException ex = assertThrows(InvalidFamilyCompositionException.class,
                () -> validator.verifier(frida));

        assertTrue(ex.getMessage().contains("le défunt"), ex.getMessage());
    }

    @Test
    void ninAbsentsOuInvalides_SontIgnores() {
        // Un échec de lecture ne doit pas bloquer le calcul
        FridaEntity frida = fiche(null,
                heritier("03", null, "Ali"),
                heritier("03", "", "Omar"),
                heritier("03", "123", "Karim"),
                heritier("03", "123", "Samir"));

        assertDoesNotThrow(() -> validator.verifier(frida));
    }

    @Test
    void memeNinEcritDifferemment_EstReconnu() {
        // Même normalisation que NinValidationService : espaces retirés
        FridaEntity frida = fiche(NIN_DEFUNT,
                heritier("03", NIN_A, "Ali"),
                heritier("05", "1098 7123 4567 8912 34", "Ali saisi à la main"));

        assertThrows(InvalidFamilyCompositionException.class, () -> validator.verifier(frida));
    }

    @Test
    void ficheSansDefuntNiHeritiers_AucuneErreur() {
        assertDoesNotThrow(() -> validator.verifier(new FridaEntity()));
    }

    // ---- Utilitaires ----

    private static FridaEntity fiche(String ninDefunt, HeritierEntity... heritiers) {
        FridaEntity frida = new FridaEntity();
        DefuntEntity defunt = new DefuntEntity();
        defunt.setIdentite(identite(ninDefunt, "Defunt"));
        frida.setDefunt(defunt);
        frida.setHeritiers(new ArrayList<>(List.of(heritiers)));
        return frida;
    }

    private static HeritierEntity heritier(String numParente, String nin, String prenom) {
        HeritierEntity h = new HeritierEntity();
        h.setNumParente(numParente);
        h.setIdentite(identite(nin, prenom));
        return h;
    }

    private static IdentitesEntity identite(String nin, String prenom) {
        IdentitesEntity id = new IdentitesEntity();
        id.setNin(nin);
        id.setPrenom(prenom);
        return id;
    }
}
