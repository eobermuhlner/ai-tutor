import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import Layout from '../components/layout/Layout';
import CatalogUploadPanel from '../components/catalog/CatalogUploadPanel';
import LanguagesPanel from '../components/language/LanguagesPanel';
import CoursesPanel from '../components/course/CoursesPanel';

type TabType = 'upload' | 'languages' | 'courses';

export default function ContentManagementPage() {
  const { user } = useAuthStore();
  const [searchParams, setSearchParams] = useSearchParams();
  const [activeTab, setActiveTab] = useState<TabType>('upload');

  // Sync active tab with URL query params
  useEffect(() => {
    const tab = searchParams.get('tab') as TabType;
    if (tab && ['upload', 'languages', 'courses'].includes(tab)) {
      setActiveTab(tab);
    } else {
      setActiveTab('upload');
    }
  }, [searchParams]);

  const handleTabChange = (tab: TabType) => {
    setActiveTab(tab);
    setSearchParams({ tab });
  };

  // Access control
  if (!user || !user.roles.includes('EDITOR')) {
    return (
      <Layout>
        <div className="max-w-4xl mx-auto p-6">
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <h2 className="text-lg font-semibold text-red-800">Access Denied</h2>
            <p className="text-red-600">
              You must be an editor to access content management.
            </p>
          </div>
        </div>
      </Layout>
    );
  }

  // Additional check for Upload tab (EDITOR only)
  const isEditor = user.roles.includes('EDITOR');

  return (
    <Layout>
      <div className="max-w-7xl mx-auto p-6">
        {/* Page Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-slate-900">Content Management</h1>
          <p className="text-slate-600 mt-2">
            Manage system content including catalog imports, languages, and courses.
          </p>
        </div>

        {/* Tab Navigation */}
        <div className="border-b border-slate-200 mb-6">
          <nav className="-mb-px flex space-x-8" aria-label="Tabs">
            <button
              onClick={() => handleTabChange('upload')}
              disabled={!isEditor}
              className={`
                whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors
                ${
                  activeTab === 'upload'
                    ? 'border-brand-500 text-brand-600'
                    : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
                }
                ${!isEditor ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
              `}
            >
              Upload
              {!isEditor && (
                <span className="ml-2 text-xs bg-slate-100 text-slate-600 px-2 py-0.5 rounded-full">
                  Editor Only
                </span>
              )}
            </button>
            <button
              onClick={() => handleTabChange('languages')}
              className={`
                whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors cursor-pointer
                ${
                  activeTab === 'languages'
                    ? 'border-brand-500 text-brand-600'
                    : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
                }
              `}
            >
              Languages
            </button>
            <button
              onClick={() => handleTabChange('courses')}
              className={`
                whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm transition-colors cursor-pointer
                ${
                  activeTab === 'courses'
                    ? 'border-brand-500 text-brand-600'
                    : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
                }
              `}
            >
              Courses
            </button>
          </nav>
        </div>

        {/* Tab Panels */}
        <div className="mt-6">
          {activeTab === 'upload' && isEditor && <CatalogUploadPanel />}
          {activeTab === 'upload' && !isEditor && (
            <div className="bg-amber-50 border border-amber-200 rounded-lg p-6">
              <h3 className="text-lg font-semibold text-amber-900 mb-2">Editor Access Required</h3>
              <p className="text-amber-800">
                The catalog upload feature requires editor privileges. Please contact your system administrator if you need access.
              </p>
            </div>
          )}
          {activeTab === 'languages' && <LanguagesPanel />}
          {activeTab === 'courses' && <CoursesPanel />}
        </div>
      </div>
    </Layout>
  );
}
