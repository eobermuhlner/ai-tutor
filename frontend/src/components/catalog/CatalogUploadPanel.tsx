import { useState } from 'react';
import { Upload, FileText, AlertCircle } from 'lucide-react';
import CatalogImportDialog from './CatalogImportDialog';
import type { CatalogImportResponse } from '../../types';
import Button from '../ui/Button';

interface CatalogUploadPanelProps {
  onImportSuccess?: (response: CatalogImportResponse) => void;
}

export default function CatalogUploadPanel({ onImportSuccess }: CatalogUploadPanelProps) {
  const [showImportDialog, setShowImportDialog] = useState(false);
  const [importResult, setImportResult] = useState<CatalogImportResponse | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const [initialCatalogFile, setInitialCatalogFile] = useState<File | null>(null);
  const [initialLessonFiles, setInitialLessonFiles] = useState<File[]>([]);

  const handleDragEnter = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    const files = Array.from(e.dataTransfer.files);
    if (files.length === 0) return;

    // Separate catalog file and lesson files
    const catalogFile = files.find(f =>
      f.name.endsWith('.yml') || f.name.endsWith('.yaml')
    );
    const lessonFiles = files.filter(f => f.name.endsWith('.md'));

    if (catalogFile) {
      setInitialCatalogFile(catalogFile);
      setInitialLessonFiles(lessonFiles);
      setShowImportDialog(true);
    }
  };

  const handleImportClick = () => {
    setInitialCatalogFile(null);
    setInitialLessonFiles([]);
    setShowImportDialog(true);
  };

  const handleImportSuccess = (response: CatalogImportResponse) => {
    setImportResult(response);
    if (onImportSuccess) {
      onImportSuccess(response);
    }
  };

  return (
    <div className="space-y-6">
      {/* Import Result */}
      {importResult && importResult.success && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-6">
          <div className="flex items-start space-x-3">
            <div className="flex-shrink-0">
              <div className="h-10 w-10 rounded-full bg-green-100 flex items-center justify-center">
                <Upload className="h-5 w-5 text-green-600" />
              </div>
            </div>
            <div className="flex-1">
              <h3 className="text-lg font-semibold text-green-900">Import Successful</h3>
              <div className="mt-2 text-sm text-green-800 space-y-1">
                <div>✓ {importResult.languagesImported} languages imported</div>
                <div>✓ {importResult.tutorsImported} tutors imported</div>
                <div>✓ {importResult.coursesImported} courses imported</div>
                <div>✓ {importResult.lessonsImported} lessons imported</div>
              </div>
              <button
                onClick={() => setImportResult(null)}
                className="mt-3 text-sm font-medium text-green-700 hover:text-green-800 underline"
              >
                Dismiss
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Main Upload Area */}
      <div
        className={`border-2 border-dashed rounded-lg p-12 text-center transition-colors ${
          dragActive
            ? 'border-brand-500 bg-brand-50'
            : 'border-slate-300 bg-white hover:border-slate-400'
        }`}
        onDragEnter={handleDragEnter}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        <div className="flex flex-col items-center space-y-4">
          <div className="h-16 w-16 rounded-full bg-brand-100 flex items-center justify-center">
            <Upload className="h-8 w-8 text-brand-600" />
          </div>

          <div>
            <h3 className="text-xl font-semibold text-slate-900 mb-2">
              Import Catalog
            </h3>
            <p className="text-slate-600 mb-4">
              Upload a catalog.yml file to import languages, tutors, and courses
            </p>
          </div>

          <Button onClick={handleImportClick} size="lg">
            <Upload className="mr-2 h-5 w-5" />
            Select Catalog File
          </Button>

          <p className="text-sm text-slate-500">
            or drag and drop your catalog.yml file here
          </p>
        </div>
      </div>

      {/* Information Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* About Catalog Format */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
          <div className="flex items-start space-x-3">
            <FileText className="h-5 w-5 text-blue-600 flex-shrink-0 mt-0.5" />
            <div>
              <h4 className="font-semibold text-blue-900 mb-2">Catalog Format</h4>
              <ul className="text-sm text-blue-800 space-y-1 list-disc list-inside">
                <li>YAML format with structured schema</li>
                <li>Contains languages, tutors, and courses</li>
                <li>Supports embedded or file-referenced lessons</li>
                <li>Optional tutor archetypes for reuse</li>
              </ul>
            </div>
          </div>
        </div>

        {/* Requirements */}
        <div className="bg-amber-50 border border-amber-200 rounded-lg p-6">
          <div className="flex items-start space-x-3">
            <AlertCircle className="h-5 w-5 text-amber-600 flex-shrink-0 mt-0.5" />
            <div>
              <h4 className="font-semibold text-amber-900 mb-2">Requirements</h4>
              <ul className="text-sm text-amber-800 space-y-1 list-disc list-inside">
                <li>ADMIN role required</li>
                <li>Valid catalog.yml file format</li>
                <li>Lesson .md files (if referenced)</li>
                <li>See CATALOG_IMPORT_FORMAT.md</li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      {/* Documentation Link */}
      <div className="bg-slate-50 border border-slate-200 rounded-lg p-6">
        <div className="flex items-start space-x-3">
          <FileText className="h-5 w-5 text-slate-600 flex-shrink-0 mt-0.5" />
          <div>
            <h4 className="font-semibold text-slate-900 mb-2">Documentation</h4>
            <p className="text-sm text-slate-700 mb-3">
              For complete format specification and migration instructions, see:
            </p>
            <ul className="text-sm text-slate-600 space-y-1 list-disc list-inside">
              <li>
                <code className="bg-slate-200 px-1.5 py-0.5 rounded text-xs">
                  CATALOG_IMPORT_FORMAT.md
                </code>
                {' '}— Unified catalog format specification
              </li>
              <li>
                <code className="bg-slate-200 px-1.5 py-0.5 rounded text-xs">
                  COURSE_MIGRATION_GUIDE.md
                </code>
                {' '}— Migration from file-based courses
              </li>
            </ul>
          </div>
        </div>
      </div>

      {/* Import Dialog */}
      <CatalogImportDialog
        isOpen={showImportDialog}
        onClose={() => {
          setShowImportDialog(false);
          setInitialCatalogFile(null);
          setInitialLessonFiles([]);
        }}
        onSuccess={handleImportSuccess}
        initialCatalogFile={initialCatalogFile}
        initialLessonFiles={initialLessonFiles}
      />
    </div>
  );
}
