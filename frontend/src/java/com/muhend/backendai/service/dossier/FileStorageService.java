package com.muhend.backendai.service.dossier;

import com.muhend.backendai.config.exception.FileStorageException;
import com.muhend.backendai.config.exception.GlobalExceptionHandler;
import com.muhend.backendai.config.util.FileValidator;
import com.muhend.backendai.config.util.PathResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final FileValidator fileValidator;
    private final PathResolver pathResolver;

    public List<String> storeFiles(MultipartFile[] files, String subFolder) {
        List<String> savedFiles = new ArrayList<>();
        
        for (MultipartFile file : files) {
            try {
                String savedPath = storeFile(file, subFolder);
                savedFiles.add(savedPath);
            } catch (IOException e) {
                log.error("Erreur lors de la sauvegarde du fichier {}: {}", 
                    file.getOriginalFilename(), e.getMessage());
                throw new FileStorageException("Erreur lors de la sauvegarde du fichier " + 
                    file.getOriginalFilename(), e);
            }
        }
        
        return savedFiles;
    }

    private String storeFile(MultipartFile file, String subFolder) throws IOException {
        fileValidator.validateFile(file);
        
        Path targetLocation = pathResolver.resolveTargetPath(subFolder);
        Path filePath = targetLocation.resolve(generateSafeFileName(file));
        
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Fichier sauvegardé avec succès: {}", filePath);
        
        return filePath.toString();
    }

    private String generateSafeFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        assert originalFileName != null;
        return System.currentTimeMillis() + "_" + originalFileName.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}