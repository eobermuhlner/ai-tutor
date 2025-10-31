import { useState, useRef, useEffect } from 'react';

interface EditableTopicProps {
  topic: string | null;
  onSave: (newTopic: string | null) => Promise<void>;
  disabled?: boolean;
}

export default function EditableTopic({ topic, onSave, disabled }: EditableTopicProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [editValue, setEditValue] = useState(topic || '');
  const [isSaving, setIsSaving] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (isEditing && inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    }
  }, [isEditing]);

  const handleSave = async () => {
    if (isSaving) return;

    const trimmedValue = editValue.trim();
    const newTopic = trimmedValue === '' ? null : trimmedValue;

    // Only save if value changed
    if (newTopic !== topic) {
      setIsSaving(true);
      try {
        await onSave(newTopic);
      } catch (error) {
        // Reset on error
        setEditValue(topic || '');
      } finally {
        setIsSaving(false);
      }
    }

    setIsEditing(false);
  };

  const handleCancel = () => {
    setEditValue(topic || '');
    setIsEditing(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSave();
    } else if (e.key === 'Escape') {
      handleCancel();
    }
  };

  if (isEditing) {
    return (
      <div className="flex items-center gap-2">
        <input
          ref={inputRef}
          type="text"
          value={editValue}
          onChange={(e) => setEditValue(e.target.value)}
          onBlur={handleSave}
          onKeyDown={handleKeyDown}
          disabled={isSaving}
          className="text-sm text-slate-700 bg-white border border-brand-300 rounded px-2 py-0.5 focus:outline-none focus:ring-1 focus:ring-brand-500 disabled:opacity-50"
          placeholder="No topic"
        />
      </div>
    );
  }

  return (
    <button
      onClick={() => !disabled && setIsEditing(true)}
      disabled={disabled}
      className="group flex items-center gap-1.5 text-sm text-slate-600 hover:text-slate-900 transition-colors disabled:cursor-not-allowed"
      title="Click to edit topic"
    >
      <span>{topic || 'No topic'}</span>
      {!disabled && (
        <svg
          className="w-3 h-3 opacity-0 group-hover:opacity-100 transition-opacity"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"
          />
        </svg>
      )}
    </button>
  );
}
