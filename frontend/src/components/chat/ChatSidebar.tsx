import { useState, useEffect } from 'react';
import PhaseSelector from './PhaseSelector';
import TeachingStyleSelector from './TeachingStyleSelector';
import SessionSummaryPanel from './SessionSummaryPanel';
import VocabularyReviewPanel from '../vocabulary/VocabularyReviewPanel';
import LessonPanel from './LessonPanel';
import TTSSettings from './TTSSettings';
import RateLimitIndicator from '../profile/RateLimitIndicator';
import UserLevelSelector from './UserLevelSelector';
import { useChatSession } from '../../contexts/ChatSessionContext';

type SidebarTab = 'summary' | 'review' | 'settings' | 'lesson';

interface ChatSidebarProps {
  isVisible: boolean;
}

export default function ChatSidebar({ isVisible }: ChatSidebarProps) {
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

  return (
    <div
      className={`flex-shrink-0 transition-all duration-300 ease-in-out ${
        isVisible ? 'w-80 opacity-100' : 'w-0 opacity-0'
      } overflow-hidden`}
    >
      <div className="h-full flex flex-col rounded-2xl border border-slate-200 bg-white shadow-soft-lg">
        {/* Tab Navigation */}
        <div className="flex border-b border-slate-200">
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

              <div className="border-t border-slate-200 pt-6">
                <RateLimitIndicator forceRefresh={rateLimitRefreshTrigger} />
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
