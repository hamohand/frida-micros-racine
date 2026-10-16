package com.muhend.backendai.config.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/files/upload");
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
                {"POST", "/api/files/upload"},
                {"POST", "/api/pdfs/ocr-cni-front"},
                {"POST", "/api/pdfs/extraire-noms-qr"},
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
    void autoriseConsultationEtCalculEnModeDemoBridee() throws Exception {
        String[][] routesAutorisees = {
                {"GET", "/api/parametres"},
                {"GET", "/api/frida/all"},
                {"GET", "/api/frida/2026-001/pdf"},
                {"POST", "/api/calculs/simuler"},
                {"GET", "/api/pdfs/lireai-ecrirebd"}
        };

        for (String[] route : routesAutorisees) {
            MockHttpServletRequest req = new MockHttpServletRequest(route[0], route[1]);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, chain);
            assertEquals(200, res.getStatus(), "Attendu 200 pour " + route[0] + " " + route[1]);
        }
        verify(chain, times(routesAutorisees.length)).doFilter(any(), any());
    }

    @Test
    void plafonneCreationDossiersParIpEtAuTotal() throws Exception {
        // 3 créations autorisées pour la même IP (1.2.3.4)
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/folders/create");
            req.addHeader("X-Forwarded-For", "1.2.3.4");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, chain);
            assertEquals(200, res.getStatus());
        }

        // 4e création depuis la même IP -> 429
        MockHttpServletRequest reqMemeIp = new MockHttpServletRequest("POST", "/api/folders/create");
        reqMemeIp.addHeader("X-Forwarded-For", "1.2.3.4");
        MockHttpServletResponse resMemeIp = new MockHttpServletResponse();
        filter.doFilter(reqMemeIp, resMemeIp, chain);
        assertEquals(429, resMemeIp.getStatus());

        // Une autre IP (5.6.7.8) peut encore créer 2 dossiers avant d'atteindre le plafond global (5)
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest reqAutreIp = new MockHttpServletRequest("POST", "/api/demo/creer-dossier-exemple");
            reqAutreIp.addHeader("X-Forwarded-For", "5.6.7.8");
            MockHttpServletResponse resAutreIp = new MockHttpServletResponse();
            filter.doFilter(reqAutreIp, resAutreIp, chain);
            assertEquals(200, resAutreIp.getStatus());
        }

        // 6e création globale -> 429 même pour une nouvelle IP
        MockHttpServletRequest reqPlafondGlobal = new MockHttpServletRequest("POST", "/api/folders/create");
        reqPlafondGlobal.addHeader("X-Forwarded-For", "9.9.9.9");
        MockHttpServletResponse resPlafondGlobal = new MockHttpServletResponse();
        filter.doFilter(reqPlafondGlobal, resPlafondGlobal, chain);
        assertEquals(429, resPlafondGlobal.getStatus());
    }
}
