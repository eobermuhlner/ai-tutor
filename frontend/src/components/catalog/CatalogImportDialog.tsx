import { useState, useEffect } from 'react';
import { X, Upload, AlertCircle, CheckCircle, Loader2 } from 'lucide-react';
import { importCatalog, validateCatalog } from '../../api/catalog';
import type { CatalogImportResponse } from '../../types';
import Button from '../ui/Button';
import FileUpload from '../ui/FileUpload';

interface CatalogImportDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (response: CatalogImportResponse) => void;
  initialCatalogFile?: File | null;
  initialLessonFiles?: File[];
}

export default function CatalogImportDialog({
  isOpen,
  onClose,
  onSuccess,
  initialCatalogFile,
  initialLessonFiles
}: CatalogImportDialogProps) {
  const [catalogFile, setCatalogFile] = useState<File | null>(null);
  const [lessonFiles, setLessonFiles] = useState<File[]>([]);
  const [isValidating, setIsValidating] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const [validationErrors, setValidationErrors] = useState<string[]>([]);
  const [importResult, setImportResult] = useState<CatalogImportResponse | null>(null);

  // Handle initial files from drag-and-drop
  useEffect(() => {
    if (isOpen && (initialCatalogFile || (initialLessonFiles && initialLessonFiles.length > 0))) {
      if (initialCatalogFile) {
        setCatalogFile(initialCatalogFile);
      }
      if (initialLessonFiles && initialLessonFiles.length > 0) {
        setLessonFiles(initialLessonFiles);
      }
    }
  }, [isOpen, initialCatalogFile, initialLessonFiles]);

  const handleCatalogFileSelected = (files: File[]) => {
    if (files.length > 0) {
      setCatalogFile(files[0]);
      setValidationErrors([]);
    }
  };

  const handleCatalogFileRemoved = () => {
    setCatalogFile(null);
  };

  const handleLessonFilesSelected = (files: File[]) => {
    setLessonFiles(prev => [...prev, ...files]);
    setValidationErrors([]);
  };

  const handleLessonFilesRemoved = (removedFiles: File[]) => {
    setLessonFiles(prev => prev.filter(f => !removedFiles.includes(f)));
  };

  const validateFiles = async () => {
    if (!catalogFile) {
      setValidationErrors(['Please select a catalog.yml file']);
      return false;
    }

    setIsValidating(true);
    setValidationErrors([]);

    try {
      const result = await validateCatalog(catalogFile, lessonFiles);
      if (!result.valid) {
        setValidationErrors(result.errors);
        return false;
      }
      return true;
    } catch (error: unknown) {
      const errorMessage = error instanceof Error && 'response' in error
        ? (error as { response?: { data?: { message?: string } } }).response?.data?.message || 'Validation failed'
        : 'Validation failed';
      setValidationErrors([errorMessage]);
      return false;
    } finally {
      setIsValidating(false);
    }
  };

  const handleImport = async () => {
    if (!catalogFile) {
      setValidationErrors(['catalog.yml file is required']);
      return;
    }

    // Validate files first
    const isValid = await validateFiles();
    if (!isValid) {
      return;
    }

    setIsImporting(true);
    setValidationErrors([]);

    try {
      const result = await importCatalog(catalogFile, lessonFiles);
      setImportResult(result);

      if (result.success) {
        // Show success message briefly then close
        setTimeout(() => {
          onSuccess(result);
          handleClose();
        }, 2000);
      } else {
        setValidationErrors(result.errors);
      }
    } catch (error: unknown) {
      const errorMessage = error instanceof Error && 'response' in error
        ? (error as { response?: { data?: { message?: string } } }).response?.data?.message || 'Import failed'
        : 'Import failed';
      setValidationErrors([errorMessage]);
    } finally {
      setIsImporting(false);
    }
  };

  const handleClose = () => {
    setCatalogFile(null);
    setLessonFiles([]);
    setValidationErrors([]);
    setImportResult(null);
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-3xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="sticky top-0 bg-white border-b border-slate-200 px-6 py-4 flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold text-slate-900">Import Catalog</h2>
            <p className="text-sm text-slate-600 mt-1">
              Upload catalog.yml file containing languages, tutors, and courses
            </p>
          </div>
          <button
            onClick={handleClose}
            disabled={isImporting}
            className="text-slate-400 hover:text-slate-600 disabled:opacity-50"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        {/* Content */}
        <div className="px-6 py-4 space-y-6">
          {/* Success Message */}
          {importResult?.success && (
            <div className="flex items-start space-x-3 p-4 bg-green-50 rounded-lg border border-green-200">
              <CheckCircle className="h-5 w-5 text-green-500 flex-shrink-0 mt-0.5" />
              <div>
                <p className="text-sm font-medium text-green-900">
                  Catalog imported successfully!
                </p>
                <div className="text-xs text-green-700 mt-1 space-y-0.5">
                  <div>{importResult.languagesImported} languages imported</div>
                  <div>{importResult.tutorsImported} tutors imported</div>
                  <div>{importResult.coursesImported} courses imported</div>
                  <div>{importResult.lessonsImported} lessons imported</div>
                </div>
              </div>
            </div>
          )}

          {/* Error Messages */}
          {validationErrors.length > 0 && (
            <div className="space-y-2">
              {validationErrors.map((error, index) => (
                <div
                  key={index}
                  className="flex items-start space-x-3 p-4 bg-red-50 rounded-lg border border-red-200"
                >
                  <AlertCircle className="h-5 w-5 text-red-500 flex-shrink-0 mt-0.5" />
                  <p className="text-sm text-red-700">{error}</p>
                </div>
              ))}
            </div>
          )}

          {/* Information Box */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-start space-x-3">
              <AlertCircle className="h-5 w-5 text-blue-500 flex-shrink-0 mt-0.5" />
              <div className="text-sm text-blue-900">
                <p className="font-medium mb-2">About Catalog Import</p>
                <ul className="list-disc list-inside space-y-1 text-blue-800">
                  <li>The catalog.yml file can contain languages, tutors, and courses</li>
                  <li>Lesson files (.md) are optional if lesson content is embedded in the catalog</li>
                  <li>See CATALOG_IMPORT_FORMAT.md for format specification</li>
                  <li>Requires ADMIN role to import</li>
                </ul>
              </div>
            </div>
          </div>

          {/* File Uploads */}
          <div className="space-y-4">
            <h3 className="text-lg font-semibold text-slate-900">Catalog Files</h3>

            <FileUpload
              label="Catalog File"
              accept=".yml,.yaml"
              multiple={false}
              onFilesSelected={handleCatalogFileSelected}
              onFilesRemoved={handleCatalogFileRemoved}
              disabled={isImporting}
              helpText="Upload catalog.yml file (required)"
              initialFiles={catalogFile ? [catalogFile] : []}
              validateFile={(file) => {
                if (!file.name.endsWith('.yml') && !file.name.endsWith('.yaml')) {
                  return 'File must have .yml or .yaml extension';
                }
                return null;
              }}
            />

            <FileUpload
              label="Lesson Files"
              accept=".md"
              multiple={true}
              onFilesSelected={handleLessonFilesSelected}
              onFilesRemoved={handleLessonFilesRemoved}
              disabled={isImporting}
              helpText="Upload lesson markdown files (optional, multiple files allowed)"
              initialFiles={lessonFiles}
              validateFile={(file) => {
                if (!file.name.endsWith('.md')) {
                  return 'File must have .md extension';
                }
                return null;
              }}
            />
          </div>
        </div>

        {/* Footer */}
        <div className="sticky bottom-0 bg-slate-50 border-t border-slate-200 px-6 py-4 flex items-center justify-end space-x-3">
          <Button
            variant="secondary"
            onClick={handleClose}
            disabled={isImporting}
          >
            Cancel
          </Button>

          <Button
            variant="secondary"
            onClick={validateFiles}
            disabled={isValidating || isImporting || !catalogFile}
          >
            {isValidating ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Validating...
              </>
            ) : (
              'Validate Files'
            )}
          </Button>

          <Button
            onClick={handleImport}
            disabled={isImporting || !catalogFile}
          >
            {isImporting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Importing...
              </>
            ) : (
              <>
                <Upload className="mr-2 h-4 w-4" />
                Import Catalog
              </>
            )}
          </Button>
        </div>
      </div>
    </div>
  );
}
