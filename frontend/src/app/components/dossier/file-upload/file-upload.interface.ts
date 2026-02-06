export interface UploadedFile {
  file: File;
  id: string;
  progress: number;
  error?: string;
}

export interface UploadConfig {
  maxFileSize: number;
  allowedTypes: string[];
  uploadPath: string;
  title: string;
}