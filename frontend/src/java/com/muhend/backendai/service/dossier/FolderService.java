package com.muhend.backendai.service.dossier;

import com.muhend.backendai.config.exception.FolderCreationException;
import com.muhend.backendai.dto.dossier.CreateFolderRequest;
import com.muhend.backendai.dto.dossier.FolderResponse;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class FolderService {
    private static final String ROOT_PATH = "C:/frida/";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    @Getter
    public static Path folderPath;

    public FolderResponse createFolder(CreateFolderRequest request) {
        String baseFolderName = generateBaseFolderName(request);
        folderPath = findAvailablePath(baseFolderName);
        
        try {
            Files.createDirectories(folderPath);
            log.info("Dossier créé avec succès folderPath: {}", folderPath);
//            log.info("Dossier créé avec succès getFileName: {}", folderPath.getFileName());
//            log.info("Dossier créé avec succès toString: {}", folderPath.toString());
            
            return FolderResponse.builder()
                    .folderName(folderPath.getFileName().toString())
                    .fullPath(folderPath.toString())
                    .build();
        } catch (IOException e) {
            log.error("Erreur lors de la création du dossier: {}", e.getMessage());
            throw new FolderCreationException("Impossible de créer le dossier: " + e.getMessage());
        }
    }

    private String generateBaseFolderName(CreateFolderRequest request) {
        return (request.getNom() + request.getPrenom() + 
                LocalDate.now().format(DATE_FORMAT)).toLowerCase();
    }

    private Path findAvailablePath(String baseName) { // ajoute "_i" si le dossier existe dèjà
        Path baseFolder = Paths.get(ROOT_PATH, baseName);
        if (!Files.exists(baseFolder)) {
            return baseFolder;
        }

        int suffix = 1;
        Path folderPath;
        do {
            folderPath = Paths.get(ROOT_PATH, baseName + "_" + suffix++);
        } while (Files.exists(folderPath));

        return folderPath;
    }
}