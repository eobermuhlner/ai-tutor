import { useState, useEffect } from 'react';
import { useAuthStore } from '../store/authStore';
import Layout from '../components/layout/Layout';
import VocabularyTable from '../components/vocabulary/VocabularyTable';
import VocabularyDetail from '../components/vocabulary/VocabularyDetail';
import Spinner from '../components/ui/Spinner';
import * as vocabularyApi from '../api/vocabulary';
import { getLanguages } from '../api/catalog';
import { formatLanguageDisplay } from '../utils/languageDisplay';
import type { VocabularyItem, VocabularyContext, Language } from '../types';
import toast from 'react-hot-toast';

export default function VocabularyPage() {
  const { user } = useAuthStore();
  const [items, setItems] = useState<VocabularyItem[]>([]);
  const [filteredItems, setFilteredItems] = useState<VocabularyItem[]>([]);
  const [languages, setLanguages] = useState<Language[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedLanguage, setSelectedLanguage] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState('');

  // Detail modal state
  const [selectedItem, setSelectedItem] = useState<VocabularyItem | null>(null);
  const [selectedContexts, setSelectedContexts] = useState<VocabularyContext[]>([]);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);

  // Load vocabulary items and languages
  useEffect(() => {
    async function loadData() {
      if (!user) return;

      setIsLoading(true);
      try {
        const [vocabularyData, languagesData] = await Promise.all([
          vocabularyApi.getVocabulary(user.id),
          getLanguages(),
        ]);
        setItems(vocabularyData);
        setFilteredItems(vocabularyData);
        setLanguages(languagesData);
      } catch (error) {
        console.error('Failed to load data:', error);
        toast.error('Failed to load vocabulary');
      } finally {
        setIsLoading(false);
      }
    }

    loadData();
  }, [user]);

  // Filter items by language and search query
  useEffect(() => {
    let filtered = items;

    // Filter by language
    if (selectedLanguage) {
      filtered = filtered.filter(
        (item) => item.lang === selectedLanguage
      );
    }

    // Filter by search query
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        (item) =>
          item.lemma.toLowerCase().includes(query)
      );
    }

    setFilteredItems(filtered);
  }, [items, selectedLanguage, searchQuery]);

  // Get unique language codes from items
  const uniqueLanguageCodes = Array.from(new Set(items.map((item) => item.lang)));

  // Filter languages to only those present in vocabulary
  const availableLanguages = languages.filter((lang) =>
    uniqueLanguageCodes.includes(lang.code)
  );



  // Handle item click
  const handleItemClick = async (itemId: string) => {
    setIsLoadingDetail(true);
    try {
      const data = await vocabularyApi.getVocabularyItem(itemId);
      setSelectedItem(data);
      setSelectedContexts(data.contexts);
    } catch (error) {
      console.error('Failed to load vocabulary item:', error);
      toast.error('Failed to load vocabulary details');
    } finally {
      setIsLoadingDetail(false);
    }
  };

  // Close detail modal
  const handleCloseDetail = () => {
    setSelectedItem(null);
    setSelectedContexts([]);
  };

  if (isLoading) {
    return (
      <Layout>
        <div className="flex items-center justify-center min-h-[60vh]">
          <Spinner size="lg" />
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-900 to-slate-700 bg-clip-text text-transparent">
            Vocabulary
          </h1>
          <p className="mt-2 text-slate-600">
            Review words and phrases you've encountered in your learning sessions
          </p>
        </div>

        {/* Filters */}
        <div className="bg-white rounded-2xl shadow-soft border border-slate-200 p-6 mb-6">
          <div className="flex flex-col sm:flex-row gap-4">
            {/* Search */}
            <div className="flex-1">
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <svg
                    className="h-5 w-5 text-slate-400"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                    />
                  </svg>
                </div>
                <input
                  type="text"
                  placeholder="Search vocabulary..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="block w-full pl-10 pr-3 py-2.5 border-2 border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all"
                />
              </div>
            </div>

            {/* Language Filter */}
            <div className="sm:w-64">
              <select
                value={selectedLanguage}
                onChange={(e) => setSelectedLanguage(e.target.value)}
                className="block w-full px-3 py-2.5 border-2 border-slate-200 rounded-xl focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500 transition-all"
              >
                <option value="">All Languages</option>
                {availableLanguages.map((lang) => (
                  <option key={lang.code} value={lang.code}>
                    {formatLanguageDisplay(lang)}
                  </option>
                ))}
              </select>
            </div>

            {/* Results count */}
            <div className="flex items-center text-sm font-medium text-slate-600 bg-slate-50 px-4 rounded-xl">
              {filteredItems.length} {filteredItems.length === 1 ? 'item' : 'items'}
            </div>
          </div>
        </div>

        {/* Table */}
        <div className="bg-white rounded-2xl shadow-soft border border-slate-200 overflow-hidden">
          <VocabularyTable
            items={filteredItems}
            onItemClick={handleItemClick}
            languages={languages}
          />
        </div>

        {/* Detail Modal */}
        {selectedItem && (
          <VocabularyDetail
            item={selectedItem}
            contexts={selectedContexts}
            onClose={handleCloseDetail}
          />
        )}

        {/* Loading overlay for detail */}
        {isLoadingDetail && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <Spinner size="lg" />
          </div>
        )}
      </div>
    </Layout>
  );
}
