package com.muhend.backendai.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @org.springframework.beans.factory.annotation.Value("${CORS_ORIGINS:http://localhost:4200,http://localhost:3000}")
    private String corsOrigins;

    @org.springframework.beans.factory.annotation.Value("${CORS_ALLOW_CREDENTIALS:true}")
    private boolean corsAllowCredentials;

    // Optionnel : absent en profil calc-only (LicenseInterceptor est guardé)
    @org.springframework.beans.factory.annotation.Autowired
    private ObjectProvider<LicenseInterceptor> licenseInterceptorProvider;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // allowedOriginPatterns supporte le wildcard "*" même avec credentials=true
        registry.addMapping("/**")
                .allowedOriginPatterns(corsOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(corsAllowCredentials);
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        LicenseInterceptor interceptor = licenseInterceptorProvider.getIfAvailable();
        if (interceptor != null) {
            registry.addInterceptor(interceptor).addPathPatterns("/api/**");
        }
    }
}