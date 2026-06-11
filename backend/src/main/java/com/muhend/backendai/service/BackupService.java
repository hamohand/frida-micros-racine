package com.muhend.backendai.service;

import com.muhend.backendai.dto.BackupInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BackupService {

    @Value("${ROOT_PATH:/app/uploads}")
    private String rootPath;

    @Value("${DB_HOST:db}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private String dbPort;

    @Value("${DB_NAME:fridaocrdb}")
    private String dbName;

    @Value("${DB_USER:postgres}")
    private String dbUser;

    @Value("${DB_PASSWORD:password}")
    private String dbPassword;

    private Path getBackupDir() {
        Path path = Paths.get(rootPath, "db_backups");
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                log.error("Could not create backup directory", e);
            }
        }
        return path;
    }

    public List<BackupInfo> listBackups() {
        try {
            return Files.list(getBackupDir())
                    .filter(path -> path.toString().endsWith(".dump"))
                    .map(path -> {
                        File file = path.toFile();
                        return BackupInfo.builder()
                                .fileName(file.getName())
                                .sizeBytes(file.length())
                                .createdAt(LocalDateTime.ofInstant(
                                        java.time.Instant.ofEpochMilli(file.lastModified()),
                                        ZoneId.systemDefault()))
                                .build();
                    })
                    .sorted((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error listing backups", e);
            return new ArrayList<>();
        }
    }

    public BackupInfo createBackup() throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String fileName = "backup_" + timestamp + ".dump";
        Path backupFile = getBackupDir().resolve(fileName);

        ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-h", dbHost,
                "-p", dbPort,
                "-U", dbUser,
                "-d", dbName,
                "-F", "c",
                "-f", backupFile.toString()
        );

        pb.environment().put("PGPASSWORD", dbPassword);
        pb.redirectErrorStream(true);

        log.info("Starting backup process for {}", fileName);
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            String output = new String(process.getInputStream().readAllBytes());
            log.error("pg_dump failed with exit code {}: {}", exitCode, output);
            Files.deleteIfExists(backupFile);
            throw new RuntimeException("Backup failed: " + output);
        }

        log.info("Backup successfully created: {}", fileName);

        File file = backupFile.toFile();
        return BackupInfo.builder()
                .fileName(fileName)
                .sizeBytes(file.length())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void restoreBackup(String fileName) throws Exception {
        Path backupFile = getBackupDir().resolve(fileName);
        if (!Files.exists(backupFile)) {
            throw new IllegalArgumentException("Backup file does not exist: " + fileName);
        }

        ProcessBuilder pb = new ProcessBuilder(
                "pg_restore",
                "-h", dbHost,
                "-p", dbPort,
                "-U", dbUser,
                "-d", dbName,
                "-c", "-1",
                backupFile.toString()
        );

        pb.environment().put("PGPASSWORD", dbPassword);
        pb.redirectErrorStream(true);

        log.info("Starting restore process from {}", fileName);
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            String output = new String(process.getInputStream().readAllBytes());
            log.error("pg_restore failed with exit code {}: {}", exitCode, output);
            throw new RuntimeException("Restore failed: " + output);
        }

        log.info("Database successfully restored from {}", fileName);
    }

    public void deleteBackup(String fileName) throws Exception {
        Path backupFile = getBackupDir().resolve(fileName);
        if (Files.exists(backupFile)) {
            Files.delete(backupFile);
            log.info("Deleted backup {}", fileName);
        }
    }

    public Path getBackupFilePath(String fileName) {
        return getBackupDir().resolve(fileName);
    }

    /**
     * Supprime les sauvegardes plus anciennes que le nombre de jours spécifié.
     * @return le nombre de fichiers supprimés
     */
    public int cleanupOldBackups(int retentionDays) {
        int deleted = 0;
        try {
            long thresholdMillis = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000);
            List<Path> oldFiles = Files.list(getBackupDir())
                    .filter(p -> p.toString().endsWith(".dump"))
                    .filter(p -> p.toFile().lastModified() < thresholdMillis)
                    .collect(Collectors.toList());

            for (Path oldFile : oldFiles) {
                Files.delete(oldFile);
                deleted++;
                log.info("Ancienne sauvegarde supprimée: {}", oldFile.getFileName());
            }
        } catch (IOException e) {
            log.error("Erreur lors du nettoyage des anciennes sauvegardes", e);
        }
        return deleted;
    }
}
