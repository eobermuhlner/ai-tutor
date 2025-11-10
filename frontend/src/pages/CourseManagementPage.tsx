import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Edit, Trash2, Eye, EyeOff, CheckCircle, XCircle } from 'lucide-react';
import { getAllCourses, deleteCourse, publishCourse, unpublishCourse } from '../api/courseManagement';
import { useAuthStore } from '../store/authStore';
import { format } from 'date-fns';
import Button from '../components/ui/Button';
import { Dialog, DialogActions, DialogContent, DialogTitle } from '../components/ui/Dialog';
import Tooltip from '../components/ui/Tooltip';
import Layout from '../components/layout/Layout';
import { getLanguages } from '../api/catalog';

export default function CourseManagementPage() {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [courses, setCourses] = useState<any[]>([]);
  const [allLanguages, setAllLanguages] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showConfirmDialog, setShowConfirmDialog] = useState(false);

  // Language code to display mapping
  const getLanguageDisplayParts = (languageCode: string): { emoji: string; text: string } => {
    const language = allLanguages.find((lang: any) => lang.code === languageCode);
    if (language) {
      return { emoji: language.flagEmoji, text: language.nativeName };
    }
    
    // Fallback mapping for common languages
    const languageMap: Record<string, { nativeName: string; flag: string }> = {
      'en': { nativeName: 'English', flag: '🇺🇸' },
      'es': { nativeName: 'Español', flag: '🇪🇸' },
      'fr': { nativeName: 'Français', flag: '🇫🇷' },
      'de': { nativeName: 'Deutsch', flag: '🇩🇪' },
      'ja': { nativeName: '日本語', flag: '🇯🇵' },
      'it': { nativeName: 'Italiano', flag: '🇮🇹' },
      'pt': { nativeName: 'Português', flag: '🇵🇹' },
      'ru': { nativeName: 'Русский', flag: '🇷🇺' },
      'zh': { nativeName: '中文', flag: '🇨🇳' },
      'ko': { nativeName: '한국어', flag: '🇰🇷' },
      'ar': { nativeName: 'العربية', flag: '🇸🇦' },
      'hi': { nativeName: 'हिन्दी', flag: '🇮🇳' },
      'nl': { nativeName: 'Nederlands', flag: '🇳🇱' },
      'sv': { nativeName: 'Svenska', flag: '🇸🇪' },
      'da': { nativeName: 'Dansk', flag: '🇩🇰' },
      'no': { nativeName: 'Norsk', flag: '🇳🇴' },
      'fi': { nativeName: 'Suomi', flag: '🇫🇮' },
      'pl': { nativeName: 'Polski', flag: '🇵🇱' },
      'tr': { nativeName: 'Türkçe', flag: '🇹🇷' },
      'he': { nativeName: 'עברית', flag: '🇮🇱' },
      'cs': { nativeName: 'Čeština', flag: '🇨🇿' },
      'el': { nativeName: 'Ελληνικά', flag: '🇬🇷' },
      'ro': { nativeName: 'Română', flag: '🇷🇴' },
      'hu': { nativeName: 'Magyar', flag: '🇭🇺' },
      'th': { nativeName: 'ไทย', flag: '🇹🇭' },
      'id': { nativeName: 'Bahasa Indonesia', flag: '🇮🇩' },
      'vi': { nativeName: 'Tiếng Việt', flag: '🇻🇳' },
    };

    const lang = languageMap[languageCode];
    if (lang) {
      return { emoji: lang.flag, text: lang.nativeName };
    }

    // Handle regional variants (e.g., es-ES, de-DE)
    const baseCode = languageCode.split('-')[0];
    const baseLang = languageMap[baseCode];
    if (baseLang) {
      const region = languageCode.split('-')[1];
      if (region) {
        return { emoji: baseLang.flag, text: `${baseLang.nativeName} (${region})` };
      }
      return { emoji: baseLang.flag, text: baseLang.nativeName };
    }

    // Default fallback
    return { emoji: '🌐', text: languageCode.toUpperCase() };
  };

  const getLocalizedName = (nameJson: string): string => {
    try {
      const nameMap = JSON.parse(nameJson);
      // Fallback to English, then to the first available
      return nameMap['en'] || Object.values(nameMap)[0] || 'Untitled Course';
    } catch {
      // If parsing fails, return a default
      return 'Untitled Course';
    }
  };
  const [courseToDelete, setCourseToDelete] = useState<string | null>(null);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      try {
        // Load both courses and languages
        const [coursesData, languagesData] = await Promise.all([
          getAllCourses(true), // Include drafts for editors
          getLanguages('en') // Load all languages with English locale
        ]);
        setCourses(coursesData);
        setAllLanguages(languagesData);
        setError(null);
      } catch (err) {
        console.error('Failed to load courses and languages:', err);
        setError('Failed to load courses and languages. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, []);

  const handleDeleteCourse = async () => {
    if (!courseToDelete) return;

    try {
      await deleteCourse(courseToDelete);
      setCourses(courses.filter(course => course.id !== courseToDelete));
      setCourseToDelete(null);
      setShowConfirmDialog(false);
    } catch (err) {
      console.error('Failed to delete course:', err);
      setError('Failed to delete course. Please try again.');
    }
  };

  const handlePublishCourse = async (courseId: string) => {
    try {
      const updatedCourse = await publishCourse(courseId);
      setCourses(courses.map(course => 
        course.id === courseId ? updatedCourse : course
      ));
    } catch (err) {
      console.error('Failed to publish course:', err);
      setError('Failed to publish course. Please try again.');
    }
  };

  const handleUnpublishCourse = async (courseId: string) => {
    try {
      const updatedCourse = await unpublishCourse(courseId);
      setCourses(courses.map(course => 
        course.id === courseId ? updatedCourse : course
      ));
    } catch (err) {
      console.error('Failed to unpublish course:', err);
      setError('Failed to unpublish course. Please try again.');
    }
  };

  const getStatusBadge = (isDraft: boolean) => {
    if (isDraft) {
      return (
        <span className="inline-flex items-center rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-semibold text-amber-800">
          <EyeOff className="w-3 h-3 mr-1" />
          Draft
        </span>
      );
    } else {
      return (
        <span className="inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-semibold text-green-800">
          <Eye className="w-3 h-3 mr-1" />
          Published
        </span>
      );
    }
  };



  if (!user || !user.roles.includes('EDITOR') && !user.roles.includes('ADMIN')) {
    return (
      <Layout>
        <div className="max-w-4xl mx-auto p-6">
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <h2 className="text-lg font-semibold text-red-800">Access Denied</h2>
            <p className="text-red-600">You must be an editor or admin to manage courses.</p>
          </div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="max-w-7xl mx-auto p-6">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Course Management</h1>
          <p className="text-slate-600 mt-2">
            Manage your courses, lessons, and curriculum. Draft courses are only visible to editors.
          </p>
        </div>
        <Button 
          onClick={() => navigate('/courses/create')}
          className="flex items-center gap-2"
        >
          <Plus className="w-5 h-5" />
          Create New Course
        </Button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <p className="text-red-600">{error}</p>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center items-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-600"></div>
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow-soft border border-slate-200 overflow-hidden">
          {/* Desktop Table View */}
          <div className="hidden md:block overflow-x-auto">
            <table className="min-w-full divide-y divide-slate-200">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                    Course
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                    Level Range
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                    Language
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                    Last Updated
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-slate-200">
                {courses.map((course) => (
                  <tr key={course.id} className="hover:bg-slate-50 transition-colors">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-slate-900">{getLocalizedName(course.nameJson)}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-slate-900">
                        {course.startingLevel} → {course.targetLevel}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        {(() => {
                          const langParts = getLanguageDisplayParts(course.languageCode);
                          return (
                            <>
                              <span className="text-lg">{langParts.emoji}</span>
                              <span className="text-sm text-slate-900">{langParts.text}</span>
                            </>
                          );
                        })()}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {getStatusBadge(course.isDraft)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                      {course.updatedAt ? format(new Date(course.updatedAt), 'MMM d, yyyy') : 'N/A'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <div className="flex items-center gap-2">
                        <Tooltip title="Edit">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => navigate(`/courses/edit/${course.id}`)}
                            className="p-2"
                          >
                            <Edit className="w-4 h-4" />
                          </Button>
                        </Tooltip>
                        {course.isDraft ? (
                          <Tooltip title="Publish">
                            <Button
                              variant="primary"
                              size="sm"
                              onClick={() => handlePublishCourse(course.id)}
                              className="p-2"
                            >
                              <CheckCircle className="w-4 h-4" />
                            </Button>
                          </Tooltip>
                        ) : (
                          <Tooltip title="Unpublish">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleUnpublishCourse(course.id)}
                              className="p-2"
                            >
                              <XCircle className="w-4 h-4" />
                            </Button>
                          </Tooltip>
                        )}
                        <Tooltip title="Delete">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              setCourseToDelete(course.id);
                              setShowConfirmDialog(true);
                            }}
                            className="p-2 text-red-600 hover:text-red-700 hover:bg-red-50"
                          >
                            <Trash2 className="w-4 h-4" />
                          </Button>
                        </Tooltip>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          
          {/* Mobile Card View */}
          <div className="md:hidden space-y-4 p-4">
            {courses.map((course) => (
              <div key={course.id} className="border border-slate-200 rounded-lg p-4 bg-white shadow-sm">
                <div className="flex justify-between items-start">
                  <div>
                    <h3 className="font-medium text-slate-900">{getLocalizedName(course.nameJson)}</h3>
                    <p className="text-sm text-slate-900 mt-1">
                      <span className="text-slate-500">Level:</span> {course.startingLevel} → {course.targetLevel}
                    </p>
                  </div>
                  {getStatusBadge(course.isDraft)}
                </div>
                
                <div className="mt-3 flex items-center gap-2">
                  {(() => {
                    const langParts = getLanguageDisplayParts(course.languageCode);
                    return (
                      <div className="flex items-center gap-1">
                        <span className="text-base">{langParts.emoji}</span>
                        <span className="text-sm text-slate-600">{langParts.text}</span>
                      </div>
                    );
                  })()}
                </div>
                
                <div className="mt-3 text-sm text-slate-500">
                  Last Updated: {course.updatedAt ? format(new Date(course.updatedAt), 'MMM d, yyyy') : 'N/A'}
                </div>
                
                <div className="mt-4 flex justify-end gap-1">
                  <Tooltip title="Edit">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => navigate(`/courses/edit/${course.id}`)}
                      className="p-2"
                    >
                      <Edit className="w-4 h-4" />
                    </Button>
                  </Tooltip>
                  <div className="flex gap-1">
                    {course.isDraft ? (
                      <Tooltip title="Publish">
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => handlePublishCourse(course.id)}
                          className="p-2"
                        >
                          <CheckCircle className="w-4 h-4" />
                        </Button>
                      </Tooltip>
                    ) : (
                      <Tooltip title="Unpublish">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleUnpublishCourse(course.id)}
                          className="p-2"
                        >
                          <XCircle className="w-4 h-4" />
                        </Button>
                      </Tooltip>
                    )}
                    <Tooltip title="Delete">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => {
                          setCourseToDelete(course.id);
                          setShowConfirmDialog(true);
                        }}
                        className="p-2 text-red-600 hover:text-red-700 hover:bg-red-50"
                      >
                        <Trash2 className="w-4 h-4" />
                      </Button>
                    </Tooltip>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {courses.length === 0 && (
            <div className="text-center py-12">
              <p className="text-slate-500 text-lg">No courses found</p>
              <p className="text-slate-400 mt-2">Create your first course to get started</p>
              <Button 
                onClick={() => navigate('/courses/create')}
                className="mt-4 flex items-center gap-2 mx-auto"
              >
                <Plus className="w-5 h-5" />
                Create New Course
              </Button>
            </div>
          )}
        </div>
      )}

      {/* Delete Confirmation Dialog */}
      <Dialog open={showConfirmDialog} onOpenChange={setShowConfirmDialog}>
        <DialogTitle>Confirm Delete</DialogTitle>
        <DialogContent>
          <p className="text-slate-600">
            Are you sure you want to delete this course? This action cannot be undone.
          </p>
        </DialogContent>
        <DialogActions>
          <Button 
            variant="outline" 
            onClick={() => setShowConfirmDialog(false)}
          >
            Cancel
          </Button>
          <Button 
            variant="danger" 
            onClick={handleDeleteCourse}
          >
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </div>
    </Layout>
  );
}