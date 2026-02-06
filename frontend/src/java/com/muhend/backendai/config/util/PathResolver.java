package com.muhend.backendai.config.util;

import com.muhend.backendai.config.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class PathResolver {
    private static final String ROOT_PATH = "C:/frida/";

    public Path resolveTargetPath(String subFolder) throws IOException {
        Path rootPath = Paths.get(ROOT_PATH);
        ensureDirectoryExists(rootPath);

        Path latestFolder = findLatestFolder(rootPath);
        Path targetLocation = latestFolder.resolve(subFolder);
        ensureDirectoryExists(targetLocation);

        return targetLocation;
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