import { useState, useEffect, useRef } from 'react';
import type { RefObject } from 'react';
import { getKeyboardLayout } from '../../utils/keyboardLayouts';
import Button from '../ui/Button';

interface VirtualKeyboardProps {
  languageCode: string;
  textareaRef: RefObject<HTMLTextAreaElement | null>;
  onCharacterInsert: (char: string) => void;
  isOpen: boolean;
  onClose: () => void;
}

export default function VirtualKeyboard({
  languageCode,
  textareaRef,
  onCharacterInsert,
  isOpen,
  onClose,
}: VirtualKeyboardProps) {
  const [mode, setMode] = useState<'simplified' | 'full'>('simplified');
  const panelRef = useRef<HTMLDivElement>(null);

  const layout = getKeyboardLayout(languageCode);

  // Close on outside click
  useEffect(() => {
    if (!isOpen) return;

    const handleClickOutside = (event: MouseEvent) => {
      if (
        panelRef.current &&
        !panelRef.current.contains(event.target as Node) &&
        textareaRef.current &&
        !textareaRef.current.contains(event.target as Node)
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
  }, [isOpen, onClose, textareaRef]);

  if (!layout) {
    return null;
  }

  const characters = mode === 'simplified' ? layout.simplified : layout.full || layout.simplified;
  const cols = layout.config?.cols || 10;
  const buttonSize = layout.config?.buttonSize || 'md';

  // Check if characters is a 2D array
  const is2DArray = Array.isArray(characters) && characters.length > 0 && Array.isArray(characters[0]);

  // Button size classes
  const sizeClasses = {
    sm: 'w-8 h-8 text-sm',
    md: 'w-10 h-10 text-lg',
    lg: 'w-12 h-12 text-xl',
  };

  // Grid column classes (Tailwind needs explicit class names)
  const gridColClasses: Record<number, string> = {
    6: 'grid-cols-6',
    8: 'grid-cols-8',
    10: 'grid-cols-10',
    12: 'grid-cols-12',
  };
  const gridClass = gridColClasses[cols] || 'grid-cols-10';

  return (
    <>
      {isOpen && (
        <div
          ref={panelRef}
          className="absolute bottom-full mb-2 left-4 right-4 bg-white border-2 border-slate-200 rounded-xl shadow-lg p-4 z-50 max-w-4xl mx-auto max-h-[60vh] overflow-y-auto"
        >
          {/* Header with title and mode toggle */}
          <div className="flex items-center justify-between mb-3 pb-2 border-b border-slate-200">
            <div>
              <h3 className="text-sm font-semibold text-slate-900">{layout.languageName} Keyboard</h3>
              <p className="text-xs text-slate-500">Click a character to insert</p>
            </div>
            {layout.full && (
              <Button
                onClick={() => setMode(mode === 'simplified' ? 'full' : 'simplified')}
                variant="outline"
                size="sm"
                className="text-xs"
              >
                {layout.languageCode === 'ja'
                  ? (mode === 'simplified' ? 'あ → ア' : 'ア → あ')
                  : (mode === 'simplified' ? 'Full' : 'Simplified')}
              </Button>
            )}
          </div>

          {/* Character buttons */}
          {is2DArray ? (
            // 2D array: render rows
            <div className="flex flex-col gap-2">
              {(characters as (string | null)[][]).map((row, rowIndex) => (
                <div key={rowIndex} className="flex gap-2 justify-center">
                  {row.map((char, colIndex) =>
                    char === null ? (
                      <div key={`empty-${rowIndex}-${colIndex}`} className={sizeClasses[buttonSize]} />
                    ) : (
                      <button
                        key={`${char}-${rowIndex}-${colIndex}`}
                        onClick={() => onCharacterInsert(char)}
                        className={`${sizeClasses[buttonSize]} bg-slate-100 hover:bg-brand-100 border border-slate-300 rounded-lg font-medium text-slate-900 transition-all hover:scale-105 hover:border-brand-400 focus:outline-none focus:ring-2 focus:ring-brand-500`}
                        aria-label={`Insert character ${char}`}
                      >
                        {char}
                      </button>
                    )
                  )}
                </div>
              ))}
            </div>
          ) : (
            // Flat array: render grid
            <div className={`grid ${gridClass} gap-2`}>
              {(characters as (string | null)[]).map((char, index) =>
                char === null ? (
                  <div key={`empty-${index}`} className={sizeClasses[buttonSize]} />
                ) : (
                  <button
                    key={`${char}-${index}`}
                    onClick={() => onCharacterInsert(char)}
                    className={`${sizeClasses[buttonSize]} bg-slate-100 hover:bg-brand-100 border border-slate-300 rounded-lg font-medium text-slate-900 transition-all hover:scale-105 hover:border-brand-400 focus:outline-none focus:ring-2 focus:ring-brand-500`}
                    aria-label={`Insert character ${char}`}
                  >
                    {char}
                  </button>
                )
              )}
            </div>
          )}

          {/* Close hint */}
          <div className="mt-3 pt-2 border-t border-slate-200 text-xs text-slate-400 text-center">
            Press ESC or click outside to close
          </div>
        </div>
      )}
    </>
  );
}
