package com.muhend.backendai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupInfo {
    /** Nom du dossier de sauvegarde (ex. frida_backup_20260912_103015). */
    private String fileName;
    private long sizeBytes;
    /** Avec fuseau : le navigateur l'affiche à l'heure locale, sans décalage. */
    private OffsetDateTime createdAt;
    /** Créée par la sauvegarde automatique (frida_auto_...). */
    private boolean automatique;
    /** Contient la copie des documents (uploads/). */
    private boolean documentsInclus;
}
