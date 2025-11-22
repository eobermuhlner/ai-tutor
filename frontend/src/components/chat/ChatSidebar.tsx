import { useState, useEffect } from 'react';
import { PanelRightClose } from 'lucide-react';
import PhaseSelector from './PhaseSelector';
import TeachingStyleSelector from './TeachingStyleSelector';
import SessionSummaryPanel from './SessionSummaryPanel';
import VocabularyReviewPanel from '../vocabulary/VocabularyReviewPanel';
import LessonPanel from './LessonPanel';
import TTSSettings from './TTSSettings';
import RateLimitIndicator from '../profile/RateLimitIndicator';
import UserLevelSelector from './UserLevelSelector';
import { useChatSession } from '../../contexts/ChatSessionContext';

type SidebarTab = 'summary' | 'review' | 'settings' | 'lesson' | 'rate-limits';

interface ChatSidebarProps {
  isVisible: boolean;
  onClose?: () => void;
}

export default function ChatSidebar({ isVisible, onClose }: ChatSidebarProps) {
  const {
    sessionId,
    courseId,
    messages,
    targetLanguageCode,
    userLevel,
    phase,
    teachingStyle,
    isSending,
    vocabularyReviewMode,
    dueCount,
    summaryRefreshKey,
    rateLimitRefreshTrigger,
    updatePhase,
    updateTeachingStyle,
    updateVocabularyReviewMode,
    updateUserLevel,
    refreshDueCount,
  } = useChatSession();

  const [sidebarTab, setSidebarTab] = useState<SidebarTab>('settings');

  // Handle vocabulary review mode tab switching
  useEffect(() => {
    if (vocabularyReviewMode) {
      setSidebarTab('review');
    } else if (sidebarTab === 'review') {
      setSidebarTab('summary');
    }
  }, [vocabularyReviewMode]); // eslint-disable-line react-hooks/exhaustive-deps

  // Handle ESC key to close sidebar
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isVisible && onClose) {
        onClose();
      }
    };

    if (isVisible) {
      document.addEventListener('keydown', handleEscape);
      return () => document.removeEventListener('keydown', handleEscape);
    }
  }, [isVisible, onClose]);

  return (
    <div
      className={`
        flex-shrink-0 transition-all duration-200 ease-in-out overflow-hidden
        fixed top-[4.5rem] left-0 right-0 bottom-0 z-50
        md:relative md:inset-auto md:max-w-none md:z-auto
        ${isVisible
          ? 'translate-x-0 opacity-100 md:w-80 md:opacity-100'
          : 'translate-x-full opacity-0 md:translate-x-0 md:w-0 md:opacity-0'
        }
      `}
    >
      <div className="h-full w-full md:h-[calc(100vh-10rem)] md:w-80 flex flex-col md:rounded-2xl bg-white shadow-soft-lg border-0 md:border md:border-slate-200">
        {/* Close Button - Mobile Only */}
        {onClose && (
          <button
            onClick={onClose}
            className="md:hidden absolute top-3 right-3 z-10 p-2 text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors"
            aria-label="Close sidebar"
          >
            <PanelRightClose className="w-5 h-5" />
          </button>
        )}

        {/* Tab Navigation */}
        <div className="flex border-b border-slate-200 pr-12 md:pr-0">
          <button
            onClick={() => setSidebarTab('settings')}
            className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
              sidebarTab === 'settings'
                ? 'text-brand-600 border-b-2 border-brand-600 bg-brand-50/50'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            Settings
          </button>
          <button
            onClick={() => setSidebarTab('lesson')}
            className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
              sidebarTab === 'lesson'
                ? 'text-brand-600 border-b-2 border-brand-600 bg-brand-50/50'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            Lesson
          </button>
          <button
            onClick={() => setSidebarTab('summary')}
            className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
              sidebarTab === 'summary'
                ? 'text-brand-600 border-b-2 border-brand-600 bg-brand-50/50'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            Summary
          </button>
          <button
            onClick={() => setSidebarTab('rate-limits')}
            className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
              sidebarTab === 'rate-limits'
                ? 'text-brand-600 border-b-2 border-brand-600 bg-brand-50/50'
                : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            Limits
          </button>
          {vocabularyReviewMode && (
            <button
              onClick={() => setSidebarTab('review')}
              className={`flex-1 px-4 py-3 text-sm font-medium transition-colors relative ${
                sidebarTab === 'review'
                  ? 'text-brand-600 border-b-2 border-brand-600 bg-brand-50/50'
                  : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
              }`}
            >
              Review
              {dueCount > 0 && (
                <span className="absolute top-2 right-2 inline-flex items-center justify-center w-5 h-5 text-xs font-bold text-white bg-brand-600 rounded-full">
                  {dueCount}
                </span>
              )}
            </button>
          )}
        </div>

        {/* Tab Content */}
        <div className="flex-1 overflow-y-auto p-4">
          {sidebarTab === 'lesson' && (
            <LessonPanel sessionId={sessionId} courseId={courseId} />
          )}
          {sidebarTab === 'summary' && (
            <SessionSummaryPanel
              key={summaryRefreshKey}
              sessionId={sessionId}
              optimisticMessageCount={messages.length}
            />
          )}
          {sidebarTab === 'review' && vocabularyReviewMode && (
            <VocabularyReviewPanel
              targetLanguageCode={targetLanguageCode}
              isVisible={true}
              onReviewComplete={refreshDueCount}
            />
          )}
          {sidebarTab === 'rate-limits' && (
            <div>
              <RateLimitIndicator forceRefresh={rateLimitRefreshTrigger} />
            </div>
          )}
          {sidebarTab === 'settings' && (
            <div className="space-y-6">
              <div>
                <UserLevelSelector
                  currentLevel={userLevel}
                  targetLanguageCode={targetLanguageCode}
                  disabled={isSending}
                  onLevelChange={updateUserLevel}
                />
              </div>

              <div className="border-t border-slate-200 pt-6">
                <h3 className="text-sm font-semibold text-slate-900 mb-3">Conversation Mode</h3>
                <PhaseSelector
                  currentPhase={phase.current}
                  effectivePhase={phase.effective}
                  onPhaseChange={updatePhase}
                  disabled={isSending}
                />
              </div>

              <div className="border-t border-slate-200 pt-6">
                <h3 className="text-sm font-semibold text-slate-900 mb-3">Teaching Style</h3>
                <TeachingStyleSelector
                  currentStyle={teachingStyle}
                  onStyleChange={updateTeachingStyle}
                  disabled={isSending}
                />
              </div>

              <div className="border-t border-slate-200 pt-6">
                <h3 className="text-sm font-semibold text-slate-900 mb-3">Vocabulary Review</h3>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="text-sm text-slate-700">Enable review mode</span>
                    {vocabularyReviewMode && dueCount > 0 && (
                      <span className="inline-flex items-center justify-center px-2 py-0.5 text-xs font-bold leading-none text-white bg-brand-600 rounded-full">
                        {dueCount} due
                      </span>
                    )}
                  </div>
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={vocabularyReviewMode}
                      onChange={(e) => updateVocabularyReviewMode(e.target.checked)}
                      disabled={isSending}
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-brand-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-brand-600"></div>
                  </label>
                </div>
              </div>

              <div className="border-t border-slate-200 pt-6">
                <TTSSettings />
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
