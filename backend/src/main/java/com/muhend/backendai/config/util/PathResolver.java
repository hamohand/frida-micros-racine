package com.muhend.backendai.config.util;

import com.muhend.backendai.config.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class PathResolver {

    @Value("${ROOT_PATH}")
    private String rootPathString;

    public Path resolveTargetPath(String subFolder) throws IOException {
        Path rootPath = Paths.get(rootPathString);
        ensureDirectoryExists(rootPath);

        Path latestFolder = findLatestFolder(rootPath);
        Path targetLocation = latestFolder.resolve(subFolder);
        ensureDirectoryExists(targetLocation);

        return targetLocation;
    }

    public Path getLatestFolder() throws IOException {
        Path rootPath = Paths.get(rootPathString);
        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath);
        }
        return findLatestFolder(rootPath);
    }

    private Path findLatestFolder(Path rootPath) throws IOException {
        return Files.list(rootPath)
                .filter(Files::isDirectory)
                .max((p1, p2) -> {
                    try {
                        return Files.getLastModifiedTime(p1)
                                .compareTo(Files.getLastModifiedTime(p2));
                    } catch (IOException e) {
                        log.error("Erreur lors de la comparaison des dossiers", e);
                        return 0;
                    }
                })
                .orElseThrow(() -> new FileStorageException("Aucun dossier trouvé"));
    }

    private void ensureDirectoryExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("Dossier créé: {}", path);
        }
    }
}