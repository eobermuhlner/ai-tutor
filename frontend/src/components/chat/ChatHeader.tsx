import { PanelRightOpen, PanelRightClose } from 'lucide-react';
import TutorImage from '../tutor/TutorImage';
import SkillBreakdownBadge from '../assessment/SkillBreakdownBadge';
import EditableTopic from './EditableTopic';
import { useChatSession } from '../../contexts/ChatSessionContext';
import FlagIcon from '../ui/FlagIcon';

interface ChatHeaderProps {
  showSidebar: boolean;
  onToggleSidebar: () => void;
}

export default function ChatHeader({ showSidebar, onToggleSidebar }: ChatHeaderProps) {
  const {
    courseName,
    targetLanguageCode,
    languages,
    userLevel,
    skillLevels,
    currentTopic,
    tutor,
    isSending,
    updateTopic,
  } = useChatSession();

  const currentLanguage = languages.find((lang) => lang.code === targetLanguageCode);

  return (
    <div className="relative z-20 flex items-center justify-between border-b border-slate-200 bg-white/90 backdrop-blur-sm px-6 py-4">
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-3">
          {tutor.profileId && (
            <div className="flex-shrink-0">
              <TutorImage
                tutorId={tutor.profileId}
                tutorImageUrl={tutor.image}
                tutorEmoji={tutor.emoji}
                tutorName={tutor.name}
                size="medium"
                rounded="lg"
              />
            </div>
          )}
          <div>
            <h1 className="text-xl font-semibold text-slate-900">
              {courseName || 'Chat Session'}
            </h1>
            <div className="flex items-center gap-3 mt-1 flex-wrap">
              {currentLanguage && (
                <>
                  <span className="inline-flex items-center gap-1 text-sm font-medium text-slate-600">
                    <FlagIcon languageCode={currentLanguage.code} size={1} />
                    {currentLanguage.nativeName.split('(')[0].trim()}
                  </span>
                  {tutor.name && (
                    <>
                      <span className="text-slate-300">•</span>
                      <span className="text-sm font-medium text-slate-600">{tutor.name}</span>
                    </>
                  )}
                </>
              )}
              {(currentLanguage && userLevel) && (
                <span className="text-slate-300">•</span>
              )}
              {userLevel && (
                <SkillBreakdownBadge
                  overall={userLevel}
                  grammar={skillLevels.grammar}
                  vocabulary={skillLevels.vocabulary}
                  fluency={skillLevels.fluency}
                  comprehension={skillLevels.comprehension}
                />
              )}
              {(userLevel && currentTopic !== undefined) && (
                <span className="text-slate-300">•</span>
              )}
              {currentTopic !== undefined && (
                <EditableTopic
                  topic={currentTopic}
                  onSave={updateTopic}
                  disabled={isSending}
                />
              )}
            </div>
          </div>
        </div>
      </div>
      <div className="flex items-center">
        <button
          onClick={onToggleSidebar}
          className="p-2 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors"
          aria-label={showSidebar ? 'Close sidebar' : 'Open sidebar'}
        >
          {showSidebar ? (
            <PanelRightClose className="w-5 h-5" />
          ) : (
            <PanelRightOpen className="w-5 h-5" />
          )}
        </button>
      </div>
    </div>
  );
}
