import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Layout from '../components/layout/Layout';
import LanguageCard from '../components/catalog/LanguageCard';
import Spinner from '../components/ui/Spinner';
import EmptyState from '../components/ui/EmptyState';
import { getLanguages } from '../api/catalog';
import type { Language } from '../types';
import toast from 'react-hot-toast';

export default function LanguageCatalogPage() {
  const [languages, setLanguages] = useState<Language[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    loadLanguages();
  }, []);

  const loadLanguages = async () => {
    setIsLoading(true);
    try {
      const data = await getLanguages();
      setLanguages(data);
    } catch (error) {
      toast.error('Failed to load languages');
      console.error('Error loading languages:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLanguageClick = (languageCode: string) => {
    navigate(`/languages/${languageCode}/courses`);
  };

  return (
    <Layout>
      <div className="flex flex-col max-h-[calc(100vh-10rem)]">
        <div className="mb-8 flex-shrink-0">
          <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-900 to-slate-700 bg-clip-text text-transparent">
            Choose a Language
          </h1>
          <p className="mt-2 text-slate-600">
            Select a language to start your learning journey
          </p>
        </div>

        <div className="flex-1 overflow-y-auto pt-2">
          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <Spinner size="lg" />
            </div>
          ) : languages.length === 0 ? (
            <EmptyState
              title="No languages available"
              message="There are no languages available at the moment. Please try again later."
              action={{
                label: 'Retry',
                onClick: loadLanguages,
              }}
            />
          ) : (
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 pb-4">
              {languages.map((language) => (
                <LanguageCard
                  key={language.code}
                  language={language}
                  onClick={() => handleLanguageClick(language.code)}
                />
              ))}
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
}
