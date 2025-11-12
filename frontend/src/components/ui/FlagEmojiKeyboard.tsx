import { useEffect, useRef } from 'react';
import type { RefObject } from 'react';

interface FlagEmojiKeyboardProps {
  inputRef: RefObject<HTMLInputElement | null>;
  onEmojiInsert: (emoji: string) => void;
  isOpen: boolean;
  onClose: () => void;
}

export default function FlagEmojiKeyboard({
  inputRef,
  onEmojiInsert,
  isOpen,
  onClose,
}: FlagEmojiKeyboardProps) {
  const panelRef = useRef<HTMLDivElement>(null);

  // Common flag emojis organized in a consistent grid
  const flagEmojis: string[] = [
    '🇺🇸', '🇬🇧', '🇨🇦', '🇦🇺', // English-speaking countries
    '🇪🇸', '🇲🇽', '🇦🇷', '🇨🇴', '🇪🇦', // Spanish-speaking countries
    '🇫🇷', '🇨🇭', '🇧🇪', // French-speaking countries
    '🇩🇪', '🇦🇹', '🇨🇭', // German-speaking countries
    '🇮🇹', '🇸🇲', // Italian-speaking countries
    '🇵🇹', '🇧🇷', '🇲🇿', // Portuguese-speaking countries
    '🇨🇳', '🇹🇼', '🇭🇰', '🇸🇬', // Chinese-speaking regions
    '🇯🇵', // Japan
    '🇰🇷', // Korea
    '🇷🇺', '🇧🇾', // Russian-speaking countries
    '🇳🇱', '🇧🇪', // Dutch-speaking countries
    '🇸🇪', '🇩🇰', '🇳🇴', '🇮🇸', // Nordic countries
    '🇵🇱', '🇨🇿', '🇭🇺', '🇷🇴', // Central/Eastern European countries
    '🇹🇷', '🇦🇿', // Turkish-speaking countries
    '🇮🇳', '🇮🇩', '🇲🇾', '🇸🇬', // Asian countries
    '🇸🇦', '🇦🇪', '🇶🇦', '🇰🇼', // Arabic-speaking countries
    '🇮🇱', '🇮🇷', '🇮🇶', // Middle Eastern countries
    '🇹🇭', '🇻🇳', '🇵🇭', '🇮🇩', // Southeast Asian countries
    '🇬🇷', '🇫🇮', '🇺🇦', '🇧🇬', // Others
    '🌐', '🌍', '🌎', '🌏', // Globe emojis (at the bottom)
  ];

  // Close on outside click
  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (event: MouseEvent) => {
      if (
        panelRef.current &&
        !panelRef.current.contains(event.target as Node) &&
        inputRef.current &&
        !inputRef.current.contains(event.target as Node)
      ) {
        onClose();
      }
    };

    const handleEscKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscKey);

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscKey);
    };
  }, [isOpen, onClose, inputRef]);

  return (
    <>
      {isOpen && (
        <div
          ref={panelRef}
          className="absolute bottom-full mb-2 left-0 bg-white border-2 border-slate-200 rounded-xl shadow-lg p-4 z-50 max-w-64 w-full max-h-[50vh] overflow-y-auto"
        >
          {/* Header with title */}
          <div className="flex items-center justify-between mb-3 pb-2 border-b border-slate-200">
            <div>
              <h3 className="text-sm font-semibold text-slate-900">Flag Emojis</h3>
              <p className="text-xs text-slate-500">Click a flag to insert</p>
            </div>
          </div>

          {/* Flag emoji buttons in a grid */}
          <div className="grid grid-cols-6 gap-2">
            {flagEmojis.map((flag, index) => (
              <button
                key={`${flag}-${index}`}
                onClick={() => {
                  onEmojiInsert(flag);
                  onClose(); // Close the keyboard after selection
                }}
                className="w-full aspect-square text-xl bg-slate-100 hover:bg-brand-100 border border-slate-300 rounded-lg font-medium text-slate-900 transition-all hover:scale-105 hover:border-brand-400 focus:outline-none focus:ring-2 focus:ring-brand-500 flex items-center justify-center"
                aria-label={`Insert flag emoji ${flag}`}
              >
                {flag}
              </button>
            ))}
          </div>

          {/* Close hint */}
          <div className="mt-3 pt-2 border-t border-slate-200 text-xs text-slate-400 text-center">
            Press ESC or click outside to close
          </div>
        </div>
      )}
    </>
  );
}