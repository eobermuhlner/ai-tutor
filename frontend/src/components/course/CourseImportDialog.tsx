import { useState, useEffect } from 'react';
import { X, Upload, AlertCircle, CheckCircle, Loader2 } from 'lucide-react';
import { importCourseFromFiles, validateImportFiles } from '../../api/courseManagement';
import type { CourseImportRequest, CourseImportResponse } from '../../api/courseManagement';
import { getLanguages } from '../../api/catalog';
import type { Language } from '../../types';
import Button from '../ui/Button';
import Input from '../ui/Input';
import Select from '../ui/Select';
import FileUpload from '../ui/FileUpload';

interface CourseImportDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (response: CourseImportResponse) => void;
  initialCurriculumFile?: File | null;
  initialLessonFiles?: File[];
}

const CATEGORIES = ['Conversational', 'Grammar', 'Travel', 'Business', 'General'];
const CEFR_LEVELS = ['A1', 'A2', 'B1', 'B2', 'C1', 'C2'];

export default function CourseImportDialog({
  isOpen,
  onClose,
  onSuccess,
  initialCurriculumFile,
  initialLessonFiles
}: CourseImportDialogProps) {
  const [curriculumFile, setCurriculumFile] = useState<File | null>(null);
  const [lessonFiles, setLessonFiles] = useState<File[]>([]);
  const [languages, setLanguages] = useState<Language[]>([]);
  const [metadata, setMetadata] = useState<CourseImportRequest>({
    languageCode: 'en',
    courseName: '',
    courseDescription: '',
    category: 'Conversational',
    startingLevel: 'A1',
    targetLevel: 'B2',
  });
  const [isValidating, setIsValidating] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const [validationErrors, setValidationErrors] = useState<string[]>([]);
  const [importResult, setImportResult] = useState<CourseImportResponse | null>(null);

  useEffect(() => {
    if (isOpen) {
      loadLanguages();
    }
  }, [isOpen]);

  // Handle initial files from drag-and-drop
  useEffect(() => {
    if (isOpen && (initialCurriculumFile || (initialLessonFiles && initialLessonFiles.length > 0))) {
      if (initialCurriculumFile) {
        setCurriculumFile(initialCurriculumFile);
      }
      if (initialLessonFiles && initialLessonFiles.length > 0) {
        setLessonFiles(initialLessonFiles);
      }
    }
  }, [isOpen, initialCurriculumFile, initialLessonFiles]);

  const loadLanguages = async () => {
    try {
      const langs = await getLanguages('en');
      setLanguages(langs);
    } catch (error) {
      console.error('Failed to load languages:', error);
    }
  };

  const handleCurriculumFileSelected = (files: File[]) => {
    if (files.length > 0) {
      setCurriculumFile(files[0]);
      setValidationErrors([]);
    }
  };

  const handleCurriculumFileRemoved = () => {
    setCurriculumFile(null);
  };

  const handleLessonFilesSelected = (files: File[]) => {
    setLessonFiles(prev => [...prev, ...files]);
    setValidationErrors([]);
  };

  const handleLessonFilesRemoved = (removedFiles: File[]) => {
    setLessonFiles(prev => prev.filter(f => !removedFiles.includes(f)));
  };

  const validateFiles = async () => {
    if (!curriculumFile && lessonFiles.length === 0) {
      setValidationErrors(['Please select at least curriculum.yml or lesson files']);
      return false;
    }

    setIsValidating(true);
    setValidationErrors([]);

    try {
      const result = await validateImportFiles(curriculumFile, lessonFiles);
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
    if (!curriculumFile) {
      setValidationErrors(['curriculum.yml file is required']);
      return;
    }

    if (lessonFiles.length === 0) {
      setValidationErrors(['At least one lesson file is required']);
      return;
    }

    if (!metadata.courseName.trim()) {
      setValidationErrors(['Course name is required']);
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
      const result = await importCourseFromFiles(curriculumFile, lessonFiles, metadata);
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
    setCurriculumFile(null);
    setLessonFiles([]);
    setValidationErrors([]);
    setImportResult(null);
    setMetadata({
      languageCode: 'en',
      courseName: '',
      courseDescription: '',
      category: 'Conversational',
      startingLevel: 'A1',
      targetLevel: 'B2',
    });
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-3xl w-full max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="sticky top-0 bg-white border-b border-slate-200 px-6 py-4 flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold text-slate-900">Import Course from Files</h2>
            <p className="text-sm text-slate-600 mt-1">
              Upload curriculum.yml and lesson markdown files
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
                  Course imported successfully!
                </p>
                <p className="text-xs text-green-700 mt-1">
                  {importResult.lessonsImported} lessons imported. Redirecting...
                </p>
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

          {/* Course Metadata */}
          <div className="space-y-4">
            <h3 className="text-lg font-semibold text-slate-900">Course Information</h3>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Input
                label="Course Name"
                value={metadata.courseName}
                onChange={(e) => setMetadata({ ...metadata, courseName: e.target.value })}
                placeholder="e.g., Conversational German"
                required
                disabled={isImporting}
              />

              <Select
                label="Language"
                value={metadata.languageCode}
                onChange={(value) => setMetadata({ ...metadata, languageCode: value })}
                required
                disabled={isImporting}
              >
                {languages.map(lang => (
                  <option key={lang.code} value={lang.code}>
                    {lang.name}
                  </option>
                ))}
              </Select>

              <Select
                label="Category"
                value={metadata.category}
                onChange={(value) => setMetadata({ ...metadata, category: value })}
                disabled={isImporting}
              >
                {CATEGORIES.map(cat => (
                  <option key={cat} value={cat}>
                    {cat}
                  </option>
                ))}
              </Select>

              <Select
                label="Starting Level"
                value={metadata.startingLevel}
                onChange={(value) => setMetadata({ ...metadata, startingLevel: value })}
                disabled={isImporting}
              >
                {CEFR_LEVELS.map(level => (
                  <option key={level} value={level}>
                    {level}
                  </option>
                ))}
              </Select>

              <Select
                label="Target Level"
                value={metadata.targetLevel}
                onChange={(value) => setMetadata({ ...metadata, targetLevel: value })}
                disabled={isImporting}
              >
                {CEFR_LEVELS.map(level => (
                  <option key={level} value={level}>
                    {level}
                  </option>
                ))}
              </Select>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Course Description
              </label>
              <textarea
                className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent"
                rows={3}
                value={metadata.courseDescription}
                onChange={(e) => setMetadata({ ...metadata, courseDescription: e.target.value })}
                placeholder="Brief description of the course..."
                disabled={isImporting}
              />
            </div>
          </div>

          {/* File Uploads */}
          <div className="space-y-4">
            <h3 className="text-lg font-semibold text-slate-900">Course Files</h3>

            <FileUpload
              label="Curriculum File"
              accept=".yml,.yaml"
              multiple={false}
              onFilesSelected={handleCurriculumFileSelected}
              onFilesRemoved={handleCurriculumFileRemoved}
              disabled={isImporting}
              helpText="Upload curriculum.yml file (required)"
              initialFiles={curriculumFile ? [curriculumFile] : []}
              validateFile={(file) => {
                if (file.name !== 'curriculum.yml' && file.name !== 'curriculum.yaml') {
                  return 'File must be named curriculum.yml';
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
              helpText="Upload lesson markdown files (required, multiple files allowed)"
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
            disabled={isValidating || isImporting || !curriculumFile || lessonFiles.length === 0}
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
            disabled={isImporting || !curriculumFile || lessonFiles.length === 0 || !metadata.courseName}
          >
            {isImporting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Importing...
              </>
            ) : (
              <>
                <Upload className="mr-2 h-4 w-4" />
                Import Course
              </>
            )}
          </Button>
        </div>
      </div>
    </div>
  );
}
