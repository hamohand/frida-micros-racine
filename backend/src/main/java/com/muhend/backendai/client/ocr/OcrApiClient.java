package com.muhend.backendai.client.ocr;

import com.muhend.backendai.client.ocr.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;

@Service
@Slf4j
public class OcrApiClient {

    @Value("${services.ocr.url}")
    private String ocrApiUrl;

    private final RestTemplate restTemplate;

    public OcrApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Upload a file to the OCR service.
     */
    public OcrUploadResponseDto uploadFile(Path filePath) {
        String url = ocrApiUrl + "/api/upload";
        log.info("Appel du service OCR upload : {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new FileSystemResource(filePath.toFile()));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            return restTemplate.postForObject(url, requestEntity, OcrUploadResponseDto.class);
        } catch (Exception e) {
            log.error("Erreur upload OCR : {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'upload vers le service OCR", e);
        }
    }

    /**
     * Fetch entity definition (zones) from OCR service.
     */
    public OcrEntityDefinitionDto getEntityDefinition(String entityName) {
        String url = ocrApiUrl + "/api/entite/" + entityName;
        log.info("Récupération définition entité : {}", url);

        try {
            return restTemplate.getForObject(url, OcrEntityDefinitionDto.class);
        } catch (Exception e) {
            log.warn("Entité non trouvée ou erreur OCR : {}", e.getMessage());
            return null;
        }
    }

    /**
     * Analyze a document using specific zones.
     */
    public OcrAnalysisResponseDto analyze(OcrAnalysisRequestDto request) {
        String url = ocrApiUrl + "/api/analyser";
        log.info("Analyse OCR : {}", url);

        try {
            return restTemplate.postForObject(url, request, OcrAnalysisResponseDto.class);
        } catch (Exception e) {
            log.error("Erreur analyse OCR : {}", e.getMessage());
            throw new RuntimeException("Erreur lors de l'analyse OCR", e);
        }
    }
}
