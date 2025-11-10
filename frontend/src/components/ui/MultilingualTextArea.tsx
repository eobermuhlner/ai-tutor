import { useState, useEffect, useRef } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import Textarea from './Textarea';

interface LanguageTab {
  code: string;
  label: string;
  content: string;
}

interface MultilingualTextAreaProps {
  value: string; // JSON string like {"en": "content", "es": "contenido"}
  onChange: (value: string) => void; // Updates JSON string
  label?: string;
  placeholder?: string;
  rows?: number;
  disabled?: boolean;
}

const MultilingualTextArea = ({
  value,
  onChange,
  label,
  placeholder = '',
  rows = 4,
  disabled = false
}: MultilingualTextAreaProps) => {
  const [tabs, setTabs] = useState<LanguageTab[]>([]);
  const [activeTab, setActiveTab] = useState<string | null>(null);
  const [showLanguageDropdown, setShowLanguageDropdown] = useState(false);
  const [buttonPosition, setButtonPosition] = useState({ top: 0, left: 0 });
  const buttonRef = useRef<HTMLButtonElement>(null);

  // Fixed global list of available languages for UI
  const availableLanguages = [
    { code: 'en', name: 'English', flag: '🇺🇸' },
    { code: 'de', name: 'German', flag: '🇩🇪' },
    { code: 'es', name: 'Spanish', flag: '🇪🇸' },
  ];

  // Calculate dropdown position when dropdown opens
  useEffect(() => {
    if (showLanguageDropdown && buttonRef.current) {
      const buttonRect = buttonRef.current.getBoundingClientRect();
      setButtonPosition({
        top: buttonRect.bottom,
        left: buttonRect.left
      });
    }
  }, [showLanguageDropdown]);

  // Initialize tabs from JSON value
  useEffect(() => {
    try {
      const parsed = JSON.parse(value);
      const initialTabs: LanguageTab[] = [];
      
      for (const [code, content] of Object.entries(parsed)) {
        const lang = availableLanguages.find(l => l.code === code);
        let displayLabel;
        
        if (lang) {
          displayLabel = `${lang.flag} ${lang.name}`;
        } else {
          // Fallback for common language codes even if not in our fixed list
          const commonLanguages: Record<string, { flag: string; name: string }> = {
            'en': { flag: '🇺🇸', name: 'English' },
            'es': { flag: '🇪🇸', name: 'Spanish' },
            'fr': { flag: '🇫🇷', name: 'Français' },
            'de': { flag: '🇩🇪', name: 'German' },
            'it': { flag: '🇮🇹', name: 'Italiano' },
            'pt': { flag: '🇵🇹', name: 'Português' },
            'ru': { flag: '🇷🇺', name: 'Русский' },
            'ja': { flag: '🇯🇵', name: '日本語' },
            'zh': { flag: '🇨🇳', name: '中文' },
            'ko': { flag: '🇰🇷', name: '한국어' },
            'ar': { flag: '🇸🇦', name: 'العربية' },
            'hi': { flag: '🇮🇳', name: 'हिन्दी' },
            'nl': { flag: '🇳🇱', name: 'Nederlands' },
            'sv': { flag: '🇸🇪', name: 'Svenska' },
            'da': { flag: '🇩🇰', name: 'Dansk' },
            'no': { flag: '🇳🇴', name: 'Norsk' },
            'fi': { flag: '🇫🇮', name: 'Suomi' },
            'pl': { flag: '🇵🇱', name: 'Polski' },
            'tr': { flag: '🇹🇷', name: 'Türkçe' },
            'he': { flag: '🇮🇱', name: 'עברית' },
            'cs': { flag: '🇨🇿', name: 'Čeština' },
            'el': { flag: '🇬🇷', name: 'Ελληνικά' },
            'ro': { flag: '🇷🇴', name: 'Română' },
            'hu': { flag: '🇭🇺', name: 'Magyar' },
            'th': { flag: '🇹🇭', name: 'ไทย' },
            'id': { flag: '🇮🇩', name: 'Bahasa Indonesia' },
            'vi': { flag: '🇻🇳', name: 'Tiếng Việt' },
          };
          
          const commonLang = commonLanguages[code.toLowerCase()];
          displayLabel = commonLang ? `${commonLang.flag} ${commonLang.name}` : code.toUpperCase();
        }
        
        initialTabs.push({
          code,
          label: displayLabel,
          content: content as string
        });
      }
      
      // Add English as default if no tabs exist
      if (initialTabs.length === 0 && availableLanguages.length > 0) {
        const english = availableLanguages.find(l => l.code === 'en') || availableLanguages[0];
        const newTab = {
          code: english.code,
          label: `${english.flag} ${english.name}`,
          content: ''
        };
        initialTabs.push(newTab);
        setActiveTab(english.code);
      } else if (initialTabs.length > 0) {
        setActiveTab(initialTabs[0].code);
      }
      
      setTabs(initialTabs);
    } catch {
      // If parsing fails, initialize with first available language
      if (availableLanguages.length > 0) {
        const firstLang = availableLanguages[0];
        const initialTabs = [{
          code: firstLang.code,
          label: `${firstLang.flag} ${firstLang.name}`,
          content: ''
        }];
        setTabs(initialTabs);
        setActiveTab(firstLang.code);
      }
    }
  }, [value]);

  // Update JSON value when tabs change
  const updateJsonValue = () => {
    const obj: Record<string, string> = {};
    tabs.forEach(tab => {
      obj[tab.code] = tab.content;
    });
    onChange(JSON.stringify(obj));
  };

  const handleContentChange = (content: string) => {
    if (!activeTab) return;
    
    setTabs(prevTabs => {
      const newTabs = prevTabs.map(tab => 
        tab.code === activeTab ? { ...tab, content } : tab
      );
      return newTabs;
    });
    
    // Debounce the JSON update for performance
    setTimeout(updateJsonValue, 0);
  };

  const addTab = (langCode: string) => {
    if (tabs.some(t => t.code === langCode)) return; // Already exists
    
    const lang = availableLanguages.find(l => l.code === langCode);
    const newTab = {
      code: langCode,
      label: lang ? `${lang.flag} ${lang.name}` : langCode.toUpperCase(),
      content: ''
    };
    
    setTabs(prev => [...prev, newTab]);
    setActiveTab(langCode);
    setShowLanguageDropdown(false);
  };

  const removeTab = (code: string) => {
    if (tabs.length <= 1) return; // Don't remove the last tab
    
    setTabs(prev => {
      const newTabs = prev.filter(tab => tab.code !== code);
      // Switch to first tab if removing active tab
      if (activeTab === code && newTabs.length > 0) {
        setActiveTab(newTabs[0].code);
      }
      return newTabs;
    });
  };

  const getAvailableLanguages = () => {
    // Get the base language codes that are already in use (e.g., 'en' from 'en' or 'en-US')
    const usedBaseCodes = new Set(tabs.map(tab => tab.code.split('-')[0]));
    
    return availableLanguages.filter(lang => 
      !usedBaseCodes.has(lang.code)  // Don't include if code is already used
    );
  };

  const activeTabContent = activeTab 
    ? tabs.find(t => t.code === activeTab)?.content || ''
    : '';

  // Handle document clicks to close dropdown
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (showLanguageDropdown) {
        const target = event.target as HTMLElement;
        const buttonElement = buttonRef.current;
        if (!target.closest('.language-dropdown-container') && 
            (!buttonElement || !buttonElement.contains(target))) {
          setShowLanguageDropdown(false);
        }
      }
    };

    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  }, [showLanguageDropdown]);

  return (
    <div className="w-full">
      {label && (
        <label className="block text-sm font-medium text-slate-700 mb-2">
          {label}
        </label>
      )}
      
      <div className="border border-slate-300 rounded-lg overflow-hidden">
        {/* Tab Headers */}
        <div className="flex items-center border-b border-slate-200 bg-slate-50">
          <div className="flex overflow-x-auto">
            {tabs.map((tab) => (
              <div
                key={tab.code}
                className={`flex items-center px-4 py-2 text-sm font-medium cursor-pointer border-r border-slate-200 whitespace-nowrap ${
                  activeTab === tab.code
                    ? 'bg-white text-slate-900'
                    : 'text-slate-600 hover:bg-slate-100'
                }`}
                onClick={() => setActiveTab(tab.code)}
              >
                <span className="mr-2">{tab.label}</span>
                {tabs.length > 1 && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      removeTab(tab.code);
                    }}
                    className="text-slate-400 hover:text-red-500 ml-1"
                  >
                    <Trash2 className="w-3 h-3" />
                  </button>
                )}
              </div>
            ))}
          </div>
          
          <div className="relative ml-auto">
            <button
              ref={buttonRef}
              onClick={(e) => {
                e.stopPropagation();
                const button = e.currentTarget;
                const rect = button.getBoundingClientRect();
                setButtonPosition({
                  top: rect.bottom + window.scrollY,
                  left: rect.left + window.scrollX
                });
                setShowLanguageDropdown(true);
              }}
              disabled={getAvailableLanguages().length === 0 || disabled}
              className={`p-2 text-slate-600 hover:bg-slate-100 rounded hover:text-slate-900 ${
                getAvailableLanguages().length === 0 || disabled ? 'opacity-50 cursor-not-allowed' : ''
              }`}
              type="button"
            >
              <Plus className="w-4 h-4" />
            </button>
            
            {showLanguageDropdown && (
              <div 
                className="language-dropdown-container absolute z-50 mt-1 w-64 bg-white border border-slate-200 rounded-md shadow-lg max-h-60 overflow-y-auto"
                style={{
                  top: buttonPosition.top,
                  left: buttonPosition.left,
                  position: 'fixed'
                }}
              >
                <div className="py-1">
                  {getAvailableLanguages().map((lang) => (
                    <button
                      key={lang.code}
                      className="flex items-center w-full px-4 py-2 text-sm text-slate-700 hover:bg-slate-100"
                      onClick={() => {
                        addTab(lang.code);
                      }}
                    >
                      <span className="mr-2 text-lg">{lang.flag}</span>
                      <span>{lang.name}</span>
                      <span className="ml-auto text-slate-500 text-xs">{lang.code.toUpperCase()}</span>
                    </button>
                  ))}
                  {getAvailableLanguages().length === 0 && (
                    <div className="px-4 py-2 text-sm text-slate-500 text-center">
                      No more languages available
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
        
        {/* Text Area for active tab */}
        <div className="p-1">
          <Textarea
            value={activeTabContent}
            onChange={(e) => handleContentChange(e.target.value)}
            placeholder={placeholder}
            rows={rows}
            className="w-full"
            disabled={disabled}
          />
        </div>
      </div>
      
      <p className="mt-1 text-sm text-slate-500">
        Manage content in different languages using tabs
      </p>
    </div>
  );
};

export default MultilingualTextArea;