import { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import Layout from '../components/layout/Layout';
import ErrorPatternList from '../components/analytics/ErrorPatternList';
import ErrorHistoryWidget from '../components/analytics/ErrorHistoryWidget';
import Spinner from '../components/ui/Spinner';
import { getErrorPatterns } from '../api/analytics';
import { getLanguages } from '../api/catalog';
import { formatCompactLanguageDisplay } from '../utils/languageDisplay';
import type { ErrorPattern, Language } from '../types';

export default function ErrorPatternsPage() {
  const [patterns, setPatterns] = useState<ErrorPattern[]>([]);
  const [languages, setLanguages] = useState<Language[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedLanguage, setSelectedLanguage] = useState<string>('');
  const [sortBy, setSortBy] = useState<'weightedScore' | 'totalCount'>('weightedScore');

  // Normalize language code to base code (e.g., "es-ES" -> "es")
  const getBaseLangCode = (langCode: string): string => {
    return langCode.split('-')[0];
  };

  useEffect(() => {
    async function loadLanguages() {
      try {
        const languagesData = await getLanguages();
        setLanguages(languagesData);

        // Auto-select first language if available
        if (languagesData.length > 0) {
          setSelectedLanguage(languagesData[0].code);
        }
      } catch (error) {
        console.error('Failed to load languages:', error);
        toast.error('Failed to load languages');
      }
    }

    loadLanguages();
  }, []);

  useEffect(() => {
    if (!selectedLanguage) return;

    async function loadPatterns() {
      setIsLoading(true);
      try {
        const baseLangCode = getBaseLangCode(selectedLanguage);
        const patternsData = await getErrorPatterns(baseLangCode, 10);
        setPatterns(patternsData);
      } catch (error) {
        console.error('Failed to load error patterns:', error);
        toast.error('Failed to load error patterns');
      } finally {
        setIsLoading(false);
      }
    }

    loadPatterns();
  }, [selectedLanguage]);

  const sortedPatterns = [...patterns].sort((a, b) => {
    if (sortBy === 'weightedScore') {
      return b.weightedScore - a.weightedScore;
    } else {
      return b.totalCount - a.totalCount;
    }
  });

  const totalErrorCount = patterns.reduce((sum, p) => sum + p.totalCount, 0);

  return (
    <Layout>
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-900 to-slate-700 bg-clip-text text-transparent">
            My Error Patterns
          </h1>
          <p className="mt-2 text-slate-600">
            Track your common mistakes and see your progress over time
          </p>
        </div>

        {/* Filters and Stats */}
        <div className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6 mb-6">
          <div className="flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
            {/* Language Filter */}
            <div className="flex-1 max-w-xs">
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Language
              </label>
              <select
                value={selectedLanguage}
                onChange={(e) => setSelectedLanguage(e.target.value)}
                className="block w-full px-3 py-2.5 border-2 border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all"
              >
                {languages.map((lang) => (
                  <option key={lang.code} value={lang.code}>
                    {formatCompactLanguageDisplay(lang)}
                  </option>
                ))}
              </select>
            </div>

            {/* Sort By */}
            <div className="flex-1 max-w-xs">
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Sort by
              </label>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as 'weightedScore' | 'totalCount')}
                className="block w-full px-3 py-2.5 border-2 border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all"
              >
                <option value="weightedScore">Importance (Weighted Score)</option>
                <option value="totalCount">Frequency (Total Count)</option>
              </select>
            </div>

            {/* Total Errors Badge */}
            <div className="flex items-center gap-3 text-sm font-medium text-slate-600 bg-slate-50 px-4 py-3 rounded-xl">
              <span>Total Errors:</span>
              <span className="text-2xl font-bold text-brand-600">{totalErrorCount}</span>
            </div>
          </div>
        </div>

        {/* Main Content */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Error Patterns List - Takes 2 columns */}
          <div className="lg:col-span-2">
            {isLoading ? (
              <div className="flex items-center justify-center py-12">
                <Spinner size="lg" />
              </div>
            ) : sortedPatterns.length === 0 ? (
              <div className="bg-white rounded-2xl shadow-soft border border-slate-200 p-12">
                <div className="text-center">
                  <div className="mx-auto w-16 h-16 rounded-full bg-green-100 flex items-center justify-center mb-4">
                    <span className="text-4xl">🎉</span>
                  </div>
                  <h3 className="text-xl font-semibold text-slate-900 mb-2">
                    No errors detected yet!
                  </h3>
                  <p className="text-slate-600">
                    Keep practicing and we'll track your error patterns here.
                  </p>
                </div>
              </div>
            ) : (
              <ErrorPatternList patterns={sortedPatterns} language={getBaseLangCode(selectedLanguage)} />
            )}
          </div>

          {/* Error History Widget - Takes 1 column */}
          <div className="lg:col-span-1">
            <ErrorHistoryWidget />
          </div>
        </div>
      </div>
    </Layout>
  );
}
