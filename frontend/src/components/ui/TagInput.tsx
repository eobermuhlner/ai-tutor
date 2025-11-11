import { useState, useRef, useEffect } from 'react';
import type { KeyboardEvent } from 'react';
import { X } from 'lucide-react';

interface TagInputProps {
  value: string[];
  onChange: (tags: string[]) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
}

export default function TagInput({ 
  value = [], 
  onChange, 
  placeholder = 'Add a tag...',
  disabled = false,
  className = '' 
}: TagInputProps) {
  const [inputValue, setInputValue] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  const handleAddTag = (tag: string) => {
    if (!tag.trim() || value.map(t => t.toLowerCase()).includes(tag.trim().toLowerCase())) {
      return;
    }
    
    const newTags = [...value, tag.trim()];
    onChange(newTags);
    setInputValue('');
  };

  const handleRemoveTag = (tagToRemove: string) => {
    const newTags = value.filter(tag => tag.toLowerCase() !== tagToRemove.toLowerCase());
    onChange(newTags);
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' || e.key === ',' || e.key === ';') {
      e.preventDefault();
      if (inputValue.trim()) {
        handleAddTag(inputValue.trim());
      }
    } else if (e.key === 'Backspace' && !inputValue && value.length > 0) {
      // Remove the last tag when backspace is pressed on an empty input
      const lastTag = value[value.length - 1];
      handleRemoveTag(lastTag);
    }
  };

  // Focus the input when component updates
  useEffect(() => {
    if (inputRef.current) {
      inputRef.current.focus();
    }
  }, []);

  return (
    <div className={`flex flex-wrap items-start gap-2 min-h-[44px] p-2 border border-slate-300 rounded-lg ${className}`}>
      {/* Render existing tags */}
      {value.map((tag) => (
        <div 
          key={tag} 
          className="flex items-center bg-brand-100 text-brand-800 rounded-full px-3 py-1 text-sm border border-brand-200"
        >
          {tag}
          {!disabled && (
            <button
              type="button"
              onClick={() => handleRemoveTag(tag)}
              className="ml-2 text-brand-500 hover:text-brand-700 focus:outline-none rounded-full hover:bg-brand-200 p-0.5 transition-colors"
              aria-label={`Remove tag ${tag}`}
            >
              <X size={14} />
            </button>
          )}
        </div>
      ))}
      
      {/* Input field for new tags */}
      {!disabled && (
        <input
          ref={inputRef}
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={value.length === 0 ? placeholder : 'Add another tag...'}
          className="flex-grow min-w-[120px] border-none outline-none bg-transparent text-sm"
          disabled={disabled}
        />
      )}
      
      {/* Show empty state message when no tags */}
      {value.length === 0 && !disabled && (
        <p className="text-slate-400 text-sm italic w-full mt-1">
          No tags added yet. Type and press Enter to add tags.
        </p>
      )}
    </div>
  );
}