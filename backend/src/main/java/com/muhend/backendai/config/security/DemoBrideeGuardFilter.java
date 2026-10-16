package com.muhend.backendai.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtre de sécurité pour la version de démonstration publique bridée (DEMO_BRIDEE=true).
 * Bloque l'administration, les sauvegardes/archives, l'envoi de fichiers personnels,
 * le NFC, la suppression et Swagger, et plafonne la création quotidienne de dossiers.
 */
@Profile("!calc-only")
@Component
public class DemoBrideeGuardFilter extends OncePerRequestFilter {

    static final String MESSAGE_FONCTION_DESACTIVEE =
            "Fonction désactivée dans la version de démonstration.";
    static final String MESSAGE_QUOTA_ATTEINT =
            "Limite de création de dossiers atteinte pour aujourd'hui dans la démonstration (3 par visiteur, 30 au total). Vous pouvez consulter et tester les dossiers existants.";

    @Value("${app.demo-bridee:false}")
    private boolean demoBridee;

    @Value("${app.demo-bridee.max-dossiers-total:30}")
    private int maxDossiersTotal;

    @Value("${app.demo-bridee.max-dossiers-par-ip:3}")
    private int maxDossiersParIp;

    private volatile LocalDate jourCourant = LocalDate.now();
    private final AtomicInteger totalDossiersJour = new AtomicInteger(0);
    private final Map<String, AtomicInteger> dossiersParIpJour = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!demoBridee || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod().toUpperCase();
        String path = normaliserChemin(request.getRequestURI());

        if (estRouteBloquee(method, path)) {
            ecrireJson(response, HttpServletResponse.SC_FORBIDDEN, MESSAGE_FONCTION_DESACTIVEE);
            return;
        }

        if (estCreationDossier(method, path)) {
            rafraichirJourSiNecessaire();
            String ip = extraireIpClient(request);
            AtomicInteger compteurIp = dossiersParIpJour.computeIfAbsent(ip, k -> new AtomicInteger(0));

            if (totalDossiersJour.get() >= maxDossiersTotal || compteurIp.get() >= maxDossiersParIp) {
                ecrireJson(response, 429, MESSAGE_QUOTA_ATTEINT);
                return;
            }

            totalDossiersJour.incrementAndGet();
            compteurIp.incrementAndGet();
            try {
                filterChain.doFilter(request, response);
            } finally {
                if (response.getStatus() >= 400) {
                    totalDossiersJour.decrementAndGet();
                    compteurIp.decrementAndGet();
                }
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean estRouteBloquee(String method, String path) {
        // Administration des utilisateurs
        if (path.equals("/api/auth/register") || path.equals("/api/auth/users")) {
            return true;
        }
        // Sauvegardes, archives, téléchargements et licence
        if (estSousChemin(path, "/api/backups")
                || estSousChemin(path, "/api/archives")
                || estSousChemin(path, "/api/telechargements")
                || estSousChemin(path, "/api/license")) {
            return true;
        }
        // Modification des paramètres (la lecture GET reste permise)
        if (estSousChemin(path, "/api/parametres") && !"GET".equals(method)) {
            return true;
        }
        // Envoi de fichiers depuis le poste du visiteur et OCR sur pièces envoyées
        if (path.equals("/api/files/upload")
                || path.equals("/api/pdfs/ocr-cni-front")
                || path.equals("/api/pdfs/mobile-mrz")
                || path.equals("/api/pdfs/extraire-noms-qr")) {
            return true;
        }
        // Sessions NFC / scanner mobile
        if (estSousChemin(path, "/api/nfc-session")) {
            return true;
        }
        // Suppressions (dossiers Frida, brouillons, nettoyage du dernier dossier)
        if ("DELETE".equals(method) && (estSousChemin(path, "/api/frida")
                || estSousChemin(path, "/api/brouillons")
                || estSousChemin(path, "/api/folders"))) {
            return true;
        }
        // Documentation Swagger / OpenAPI
        if (path.equals("/swagger-ui.html")
                || estSousChemin(path, "/swagger-ui")
                || estSousChemin(path, "/v3/api-docs")
                || estSousChemin(path, "/api-docs")) {
            return true;
        }
        return false;
    }

    private boolean estCreationDossier(String method, String path) {
        return "POST".equals(method) && (path.equals("/api/folders/create")
                || path.equals("/api/demo/creer-dossier-exemple"));
    }

    private boolean estSousChemin(String path, String prefixe) {
        return path.equals(prefixe) || path.startsWith(prefixe + "/");
    }

    private String normaliserChemin(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "/";
        }
        if (uri.length() > 1 && uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    private synchronized void rafraichirJourSiNecessaire() {
        LocalDate aujourdHui = LocalDate.now();
        if (!aujourdHui.equals(jourCourant)) {
            jourCourant = aujourdHui;
            totalDossiersJour.set(0);
            dossiersParIpJour.clear();
        }
    }

    private String extraireIpClient(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private void ecrireJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String echappe = message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"message\":\"" + echappe + "\",\"error\":\"" + echappe + "\"}");
    }

    /** Réinitialise les compteurs de quotas (utile pour les tests). */
    public void reinitialiserCompteurs() {
        jourCourant = LocalDate.now();
        totalDossiersJour.set(0);
        dossiersParIpJour.clear();
    }
}
