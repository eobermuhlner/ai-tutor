import { useState } from 'react';
import { CEFRLevel } from '../../types';
import { updateLanguageProficiency } from '../../api/userLanguages';
import { useAuthStore } from '../../store/authStore';
import toast from 'react-hot-toast';

interface UserLevelSelectorProps {
  currentLevel: CEFRLevel;
  targetLanguageCode: string;
  disabled?: boolean;
  onLevelChange: (newLevel: CEFRLevel) => void;
}

export default function UserLevelSelector({
  currentLevel,
  targetLanguageCode,
  disabled = false,
  onLevelChange,
}: UserLevelSelectorProps) {
  const [isUpdating, setIsUpdating] = useState(false);
  const user = useAuthStore((state) => state.user);

  const cefrLevelDescriptions: Record<CEFRLevel, string> = {
    [CEFRLevel.None]: 'Not set',
    [CEFRLevel.A1]: 'Beginner',
    [CEFRLevel.A2]: 'Elementary',
    [CEFRLevel.B1]: 'Intermediate',
    [CEFRLevel.B2]: 'Upper Intermediate',
    [CEFRLevel.C1]: 'Advanced',
    [CEFRLevel.C2]: 'Proficient',
  };

  const handleLevelChange = async (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newLevel = e.target.value as CEFRLevel;
    if (newLevel === currentLevel || !user) return;

    setIsUpdating(true);
    try {
      await updateLanguageProficiency(user.id, targetLanguageCode, newLevel);
      onLevelChange(newLevel);
      toast.success('Your level has been updated');
    } catch (error) {
      console.error('Failed to update level:', error);
      toast.error('Failed to update level. Please try again.');
      // Reset dropdown to current level on error
      e.target.value = currentLevel;
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <div className="space-y-2">
      <label htmlFor="user-level" className="block text-sm font-medium text-slate-700">
        Your Current Level
      </label>
      <select
        id="user-level"
        value={currentLevel}
        onChange={handleLevelChange}
        disabled={disabled || isUpdating}
        className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-transparent disabled:bg-slate-100 disabled:cursor-not-allowed transition-colors"
      >
        {Object.values(CEFRLevel).map((level) => (
          <option key={level} value={level}>
            {level} - {cefrLevelDescriptions[level]}
          </option>
        ))}
      </select>
      <p className="text-xs text-slate-500">
        Set your current proficiency level to help the tutor adjust to your needs
      </p>
    </div>
  );
}
