import { UploadedFile } from '../file-upload/file-upload.interface';

export interface UploadWindowState {
  isVisible: boolean;
  hasFiles: boolean;
  isUploading?: boolean;
  path: string;
  rawFiles?: UploadedFile[];
  groupedFiles?: {files: File[], docType: string}[];
}

export interface UploadConfig {
  title: string;
  folderPath: string;
  nextWindow?: string;
}