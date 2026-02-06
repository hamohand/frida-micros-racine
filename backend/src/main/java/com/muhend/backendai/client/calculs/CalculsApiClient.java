package com.muhend.backendai.client.calculs;

import com.muhend.backendai.client.calculs.dto.CalculRequestDto;
import com.muhend.backendai.client.calculs.dto.CalculResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class CalculsApiClient {

    @Value("${services.calculs.url}")
    private String calculsApiUrl;

    private final RestTemplate restTemplate;

    public CalculsApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public CalculResponseDto calculerParts(CalculRequestDto request) {
        // Construct the full URL
        // Note: The context path "/calculs" must be included if the service is
        // configured with it.
        // The SERVICES_CALCULS_URL env var typically points to http://host:port
        // But if we passed http://calculs-api:8081, we need to append context path and
        // endpoint.

        // Let's assume the injected URL is the base root without context path if it
        // comes from docker-compose service name logic,
        // OR the full base URL including context path if we configured it so.
        // Given docker-compose: SERVICES_CALCULS_URL: http://calculs-api:8081
        // And calculs service context-path: /calculs
        // Endpoint: /api/v1/heritage/calculate

        String url = calculsApiUrl + "/calculs/api/v1/heritage/calculate";

        log.info("Appel du service calculs : {}", url);
        log.debug("Payload : {}", request);

        try {
            return restTemplate.postForObject(url, request, CalculResponseDto.class);
        } catch (Exception e) {
            log.error("Erreur lors de l'appel au service de calcul : {}", e.getMessage());
            throw new RuntimeException("Service de calcul indisponible ou erreur de traitement", e);
        }
    }
}
