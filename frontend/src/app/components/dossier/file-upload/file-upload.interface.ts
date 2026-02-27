export interface UploadedFile {
  file: File;
  id: string;
  progress: number;
  docType: string;
  error?: string;
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
}