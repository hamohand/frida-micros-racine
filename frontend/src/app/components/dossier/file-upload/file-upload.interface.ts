export interface UploadedFile {
  file: File;
  id: string;
  progress: number;
  docType: string;
  entityName?: string;
  error?: string;
  versoFile?: File;  // Fichier verso optionnel (CNI)
}

export interface DocTypeOption {
  id: string;    // suffixe dossier: 'en', 'cni', 'pp'
  label: string; // libellé affiché
}

export interface UploadConfig {
  maxFileSize: number;
  allowedTypes: string[];
  uploadPath: string;
  title: string;
  docTypes: DocTypeOption[];
  allowSkip?: boolean;
  skipText?: string;
  maxFiles?: number;
}