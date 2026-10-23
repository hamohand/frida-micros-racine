package com.muhend.backendai.config.security;

import com.muhend.backendai.service.demo.DemoFamilleService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DemoBrideeGuardFilterTest {

    private DemoBrideeGuardFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new DemoBrideeGuardFilter();
        ReflectionTestUtils.setField(filter, "demoBridee", true);
        ReflectionTestUtils.setField(filter, "maxDossiersTotal", 5);
        ReflectionTestUtils.setField(filter, "maxDossiersParIp", 3);
        filter.reinitialiserCompteurs();
        chain = mock(FilterChain.class);
    }

    @Test
    void laisseToutPasserQuandDemoBrideeDesactivee() throws Exception {
        ReflectionTestUtils.setField(filter, "demoBridee", false);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/auth/users");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(chain, times(1)).doFilter(req, res);
        assertEquals(200, res.getStatus());
    }

    @Test
    void bloqueRoutesSensiblesEnModeDemoBridee() throws Exception {
        String[][] routesBloquees = {
                {"GET", "/api/auth/users"},
                {"POST", "/api/auth/register"},
                {"GET", "/api/backups"},
                {"POST", "/api/backups"},
                {"GET", "/api/archives"},
                {"GET", "/api/telechargements/abc"},
                {"GET", "/api/license/status"},
                {"POST", "/api/license/activate"},
                {"PUT", "/api/parametres"},
                {"POST", "/api/pdfs/ocr-cni-front"},
                {"POST", "/api/pdfs/mobile-mrz"},
                {"GET", "/api/nfc-session/s1/stream"},
                {"DELETE", "/api/frida/2026-001"},
                {"DELETE", "/api/brouillons/42"},
                {"DELETE", "/api/folders/clear-latest"},
                {"GET", "/swagger-ui.html"},
                {"GET", "/v3/api-docs"}
        };

        for (String[] route : routesBloquees) {
            MockHttpServletRequest req = new MockHttpServletRequest(route[0], route[1]);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, chain);
            assertEquals(403, res.getStatus(), "Attendu 403 pour " + route[0] + " " + route[1]);
            assertTrue(res.getContentAsString().contains("démonstration"));
        }
        verifyNoInteractions(chain);
    }

    @Test
    void verifieEmpreinteFichiersFamille() {
        DemoFamilleService service = new DemoFamilleService();
        service.init();

        assertEquals(15, service.listerFichiers().size());

        byte[] vraiActe = service.lireFichier("epoux_deces.png");
        assertNotNull(vraiActe);
        MockMultipartFile fichierFamille = new MockMultipartFile("files", "epoux_deces.png", "image/png", vraiActe);
        assertTrue(service.estFichierAutorise(fichierFamille));

        MockMultipartFile fichierPersonnel = new MockMultipartFile("files", "mon_fichier.png", "image/png",
                "contenu_personnel_interdit".getBytes(StandardCharsets.UTF_8));
        assertFalse(service.estFichierAutorise(fichierPersonnel));
    }

    @Test
    void plafonneCreationDossiersParIpEtAuTotal() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/folders/create");
            req.addHeader("X-Forwarded-For", "1.2.3.4");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, chain);
            assertEquals(200, res.getStatus());
        }

        MockHttpServletRequest reqMemeIp = new MockHttpServletRequest("POST", "/api/folders/create");
        reqMemeIp.addHeader("X-Forwarded-For", "1.2.3.4");
        MockHttpServletResponse resMemeIp = new MockHttpServletResponse();
        filter.doFilter(reqMemeIp, resMemeIp, chain);
        assertEquals(429, resMemeIp.getStatus());

        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest reqAutreIp = new MockHttpServletRequest("POST", "/api/demo/creer-dossier-exemple");
            reqAutreIp.addHeader("X-Forwarded-For", "5.6.7.8");
            MockHttpServletResponse resAutreIp = new MockHttpServletResponse();
            filter.doFilter(reqAutreIp, resAutreIp, chain);
            assertEquals(200, resAutreIp.getStatus());
        }

        MockHttpServletRequest reqPlafondGlobal = new MockHttpServletRequest("POST", "/api/folders/create");
        reqPlafondGlobal.addHeader("X-Forwarded-For", "9.9.9.9");
        MockHttpServletResponse resPlafondGlobal = new MockHttpServletResponse();
        filter.doFilter(reqPlafondGlobal, resPlafondGlobal, chain);
        assertEquals(429, resPlafondGlobal.getStatus());
    }
}
