import { useState, useRef, useEffect, DragEvent, ChangeEvent } from 'react';
import { Upload, X, FileText, AlertCircle, CheckCircle } from 'lucide-react';
import Button from './Button';

interface FileUploadProps {
  accept?: string;
  multiple?: boolean;
  maxSize?: number; // in bytes
  onFilesSelected: (files: File[]) => void;
  onFilesRemoved?: (files: File[]) => void;
  disabled?: boolean;
  label?: string;
  helpText?: string;
  showPreview?: boolean;
  validateFile?: (file: File) => string | null; // Return error message or null if valid
  initialFiles?: File[]; // Files to display initially
}

export default function FileUpload({
  accept,
  multiple = false,
  maxSize,
  onFilesSelected,
  onFilesRemoved,
  disabled = false,
  label,
  helpText,
  showPreview = true,
  validateFile,
  initialFiles,
}: FileUploadProps) {
  const [files, setFiles] = useState<File[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Sync internal state with initial files prop
  useEffect(() => {
    if (initialFiles && initialFiles.length > 0) {
      setFiles(initialFiles);
    }
  }, [initialFiles]);

  const validateFiles = (filesToValidate: File[]): { valid: File[]; errors: Record<string, string> } => {
    const valid: File[] = [];
    const newErrors: Record<string, string> = {};

    filesToValidate.forEach(file => {
      // Check file size
      if (maxSize && file.size > maxSize) {
        newErrors[file.name] = `File exceeds maximum size of ${(maxSize / 1024 / 1024).toFixed(2)}MB`;
        return;
      }

      // Custom validation
      if (validateFile) {
        const error = validateFile(file);
        if (error) {
          newErrors[file.name] = error;
          return;
        }
      }

      valid.push(file);
    });

    return { valid, errors: newErrors };
  };

  const handleFiles = (newFiles: FileList | File[]) => {
    const filesArray = Array.from(newFiles);
    const { valid, errors: validationErrors } = validateFiles(filesArray);

    if (multiple) {
      const updatedFiles = [...files, ...valid];
      setFiles(updatedFiles);
      setErrors({ ...errors, ...validationErrors });
      if (valid.length > 0) {
        onFilesSelected(valid);
      }
    } else {
      const updatedFiles = valid.slice(0, 1);
      setFiles(updatedFiles);
      setErrors(validationErrors);
      if (updatedFiles.length > 0) {
        onFilesSelected(updatedFiles);
      }
    }
  };

  const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    if (!disabled) {
      setIsDragging(true);
    }
  };

  const handleDragLeave = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);

    if (disabled) return;

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFiles(e.dataTransfer.files);
    }
  };

  const handleFileInputChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      handleFiles(e.target.files);
    }
  };

  const handleRemoveFile = (fileToRemove: File) => {
    const updatedFiles = files.filter(f => f !== fileToRemove);
    setFiles(updatedFiles);

    // Remove error if exists
    const updatedErrors = { ...errors };
    delete updatedErrors[fileToRemove.name];
    setErrors(updatedErrors);

    if (onFilesRemoved) {
      onFilesRemoved([fileToRemove]);
    }
  };

  const handleBrowseClick = () => {
    fileInputRef.current?.click();
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  };

  return (
    <div className="w-full">
      {label && (
        <label className="block text-sm font-medium text-slate-700 mb-2">
          {label}
        </label>
      )}

      <div
        className={`
          border-2 border-dashed rounded-lg p-6 text-center cursor-pointer transition-colors
          ${isDragging ? 'border-brand-500 bg-brand-50' : 'border-slate-300 hover:border-brand-400'}
          ${disabled ? 'opacity-50 cursor-not-allowed' : ''}
        `}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={!disabled ? handleBrowseClick : undefined}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept={accept}
          multiple={multiple}
          onChange={handleFileInputChange}
          disabled={disabled}
          className="hidden"
        />

        <Upload className={`mx-auto h-12 w-12 ${isDragging ? 'text-brand-500' : 'text-slate-400'}`} />

        <p className="mt-2 text-sm text-slate-600">
          <span className="font-medium text-brand-600">Click to browse</span> or drag and drop
        </p>

        {helpText && (
          <p className="mt-1 text-xs text-slate-500">{helpText}</p>
        )}
      </div>

      {/* File List */}
      {showPreview && files.length > 0 && (
        <div className="mt-4 space-y-2">
          <p className="text-sm font-medium text-slate-700">
            Selected Files ({files.length})
          </p>
          {files.map((file, index) => (
            <div
              key={`${file.name}-${index}`}
              className="flex items-center justify-between p-3 bg-slate-50 rounded-lg border border-slate-200"
            >
              <div className="flex items-center space-x-3 flex-1 min-w-0">
                <FileText className="h-5 w-5 text-slate-400 flex-shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-slate-900 truncate">
                    {file.name}
                  </p>
                  <p className="text-xs text-slate-500">
                    {formatFileSize(file.size)}
                  </p>
                </div>
                <CheckCircle className="h-5 w-5 text-green-500 flex-shrink-0" />
              </div>
              {!disabled && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleRemoveFile(file);
                  }}
                  className="ml-2"
                >
                  <X className="h-4 w-4" />
                </Button>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Errors */}
      {Object.keys(errors).length > 0 && (
        <div className="mt-4 space-y-2">
          {Object.entries(errors).map(([fileName, error]) => (
            <div
              key={fileName}
              className="flex items-start space-x-2 p-3 bg-red-50 rounded-lg border border-red-200"
            >
              <AlertCircle className="h-5 w-5 text-red-500 flex-shrink-0 mt-0.5" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-red-900">{fileName}</p>
                <p className="text-xs text-red-700 mt-1">{error}</p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
