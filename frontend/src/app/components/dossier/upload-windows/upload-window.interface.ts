export interface UploadWindowState {
  isVisible: boolean;
  hasFiles: boolean;
  path: string;
}

export interface UploadConfig {
  title: string;
  folderPath: string;
  nextWindow?: string;
}