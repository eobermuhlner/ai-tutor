import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Edit, Trash2, Eye, EyeOff, CheckCircle, XCircle } from 'lucide-react';
import { getAllLanguages, deleteLanguage, activateLanguage, deactivateLanguage } from '../api/languageManagement';
import { useAuthStore } from '../store/authStore';
import Button from '../components/ui/Button';
import { Dialog, DialogActions, DialogContent, DialogTitle } from '../components/ui/Dialog';
import Tooltip from '../components/ui/Tooltip';
import Layout from '../components/layout/Layout';
import { Difficulty, type Language } from '../types';
import FlagIcon from '../components/ui/FlagIcon';

export default function LanguageManagementPage() {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [languages, setLanguages] = useState<Language[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showConfirmDialog, setShowConfirmDialog] = useState(false);

  const [languageToDelete, setLanguageToDelete] = useState<string | null>(null);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      try {
        const languagesData = await getAllLanguages();
        setLanguages(languagesData);
        setError(null);
      } catch (err) {
        console.error('Failed to load languages:', err);
        setError('Failed to load languages. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, []);

  const handleDeleteLanguage = async () => {
    if (!languageToDelete) return;

    try {
      await deleteLanguage(languageToDelete);
      setLanguages(languages.filter(lang => lang.code !== languageToDelete));
      setLanguageToDelete(null);
      setShowConfirmDialog(false);
    } catch (err) {
      console.error('Failed to delete language:', err);
      setError('Failed to delete language. Please try again.');
    }
  };

  const handleActivateLanguage = async (code: string) => {
    try {
      const updatedLanguage = await activateLanguage(code);
      setLanguages(languages.map(lang =>
        lang.code === code ? updatedLanguage : lang
      ));
    } catch (err) {
      console.error('Failed to activate language:', err);
      setError('Failed to activate language. Please try again.');
    }
  };

  const handleDeactivateLanguage = async (code: string) => {
    try {
      const updatedLanguage = await deactivateLanguage(code);
      setLanguages(languages.map(lang =>
        lang.code === code ? updatedLanguage : lang
      ));
    } catch (err) {
      console.error('Failed to deactivate language:', err);
      setError('Failed to deactivate language. Please try again.');
    }
  };

  const getStatusBadge = (isActive: boolean) => {
    if (isActive) {
      return (
        <span className="inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-semibold text-green-800">
          <Eye className="w-3 h-3 mr-1" />
          Active
        </span>
      );
    } else {
      return (
        <span className="inline-flex items-center rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-semibold text-amber-800">
          <EyeOff className="w-3 h-3 mr-1" />
          Inactive
        </span>
      );
    }
  };

  const getDifficultyLabel = (difficulty: Difficulty) => {
    switch (difficulty) {
      case Difficulty.Easy:
        return 'Easy';
      case Difficulty.Medium:
        return 'Medium';
      case Difficulty.Hard:
        return 'Hard';
      default:
        return difficulty;
    }
  };

  if (!user || (!user.roles.includes('EDITOR') && !user.roles.includes('ADMIN'))) {
    return (
      <Layout>
        <div className="max-w-4xl mx-auto p-6">
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <h2 className="text-lg font-semibold text-red-800">Access Denied</h2>
            <p className="text-red-600">You must be an editor or admin to manage languages.</p>
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
            <h1 className="text-3xl font-bold text-slate-900">Languages</h1>
            <p className="text-slate-600 mt-2">
              Manage system languages available for courses. Inactive languages won't appear in course catalogs.
            </p>
          </div>
          <Button
            onClick={() => navigate('/languages/create')}
            className="flex items-center gap-2"
          >
            <Plus className="w-5 h-5" />
            Add New Language
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
                      Language
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                      Native Name
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                      Difficulty
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                      Courses
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-slate-200">
                  {languages.map((language) => (
                    <tr key={language.code} className="hover:bg-slate-50 transition-colors">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center">
                          <div className="text-lg mr-2">
                            <FlagIcon languageCode={language.code} size={1.5} />
                          </div>
                          <div>
                            <div className="text-sm font-medium text-slate-900">{language.name}</div>
                            <div className="text-xs text-slate-500">{language.code.toUpperCase()}</div>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-slate-900">{language.nativeName}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-slate-900">
                          {getDifficultyLabel(language.difficulty)}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        {getStatusBadge(!!language.isActive)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-slate-500">
                        {language.courseCount}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                        <div className="flex items-center gap-2">
                          <Tooltip title="Edit">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => navigate(`/languages/edit/${language.code}`)}
                              className="p-2"
                            >
                              <Edit className="w-4 h-4" />
                            </Button>
                          </Tooltip>
                          {language.isActive ? (
                            <Tooltip title="Deactivate">
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => handleDeactivateLanguage(language.code)}
                                className="p-2"
                              >
                                <XCircle className="w-4 h-4" />
                              </Button>
                            </Tooltip>
                          ) : (
                            <Tooltip title="Activate">
                              <Button
                                variant="primary"
                                size="sm"
                                onClick={() => handleActivateLanguage(language.code)}
                                className="p-2"
                              >
                                <CheckCircle className="w-4 h-4" />
                              </Button>
                            </Tooltip>
                          )}
                          <Tooltip title="Delete">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => {
                                setLanguageToDelete(language.code);
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
              {languages.map((language) => (
                <div key={language.code} className="border border-slate-200 rounded-lg p-4 bg-white shadow-sm">
                  <div className="flex justify-between items-start">
                    <div>
                      <div className="flex items-center gap-2">
                        <FlagIcon languageCode={language.code} size={1.5} />
                        <h3 className="font-medium text-slate-900">{language.name}</h3>
                      </div>
                      <p className="text-sm text-slate-900 mt-1">
                        <span className="text-slate-500">Code:</span> {language.code.toUpperCase()}
                      </p>
                      <p className="text-sm text-slate-900">
                        <span className="text-slate-500">Native:</span> {language.nativeName}
                      </p>
                    </div>
                    {getStatusBadge(!!language.isActive)}
                  </div>

                  <div className="mt-3 flex items-center justify-between">
                    <div className="text-sm text-slate-700">
                      <span className="font-medium">Difficulty:</span> {getDifficultyLabel(language.difficulty)}
                    </div>
                    <div className="text-sm text-slate-700">
                      <span className="font-medium">Courses:</span> {language.courseCount}
                    </div>
                  </div>

                  <div className="mt-4 flex justify-end gap-1">
                    <Tooltip title="Edit">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate(`/languages/edit/${language.code}`)}
                        className="p-2"
                      >
                        <Edit className="w-4 h-4" />
                      </Button>
                    </Tooltip>
                    <div className="flex gap-1">
                      {language.isActive ? (
                        <Tooltip title="Deactivate">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleDeactivateLanguage(language.code)}
                            className="p-2"
                          >
                            <XCircle className="w-4 h-4" />
                          </Button>
                        </Tooltip>
                      ) : (
                        <Tooltip title="Activate">
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => handleActivateLanguage(language.code)}
                            className="p-2"
                          >
                            <CheckCircle className="w-4 h-4" />
                          </Button>
                        </Tooltip>
                      )}
                      <Tooltip title="Delete">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            setLanguageToDelete(language.code);
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

            {languages.length === 0 && (
              <div className="text-center py-12">
                <p className="text-slate-500 text-lg">No languages found</p>
                <p className="text-slate-400 mt-2">Create your first language to get started</p>
                <Button
                  onClick={() => navigate('/languages/create')}
                  className="mt-4 flex items-center gap-2 mx-auto"
                >
                  <Plus className="w-5 h-5" />
                  Add New Language
                </Button>
              </div>
            )}
          </div>
        )}

        {/* Delete Confirmation Dialog */}
        <Dialog open={showConfirmDialog} onOpenChange={setShowConfirmDialog}>
          <DialogContent>
            <DialogTitle>Confirm Delete</DialogTitle>
            <p className="text-slate-600">
              Are you sure you want to delete this language? This action cannot be undone.
            </p>
            <DialogActions>
              <Button
                variant="outline"
                onClick={() => setShowConfirmDialog(false)}
              >
                Cancel
              </Button>
              <Button
                variant="danger"
                onClick={handleDeleteLanguage}
              >
                Delete
              </Button>
            </DialogActions>
          </DialogContent>
        </Dialog>
      </div>
    </Layout>
  );
}