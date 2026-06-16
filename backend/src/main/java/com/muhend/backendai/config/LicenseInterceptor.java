package com.muhend.backendai.config;

import com.muhend.backendai.service.LicenseValidationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LicenseInterceptor implements HandlerInterceptor {

    @Autowired
    private LicenseValidationService licenseService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // On ignore les routes liées à la vérification ou activation de licence
        if (path.startsWith("/api/license")) {
            return true;
        }

        // On vérifie la validité
        if (!licenseService.isLicenseValid()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Licence invalide, manquante ou expiée.\", \"code\": \"LICENSE_REQUIRED\"}");
            return false;
        }

        return true;
    }
}
