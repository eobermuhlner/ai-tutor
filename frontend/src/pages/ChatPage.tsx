import { useState } from 'react';
import { useParams } from 'react-router-dom';
import Layout from '../components/layout/Layout';
import MessageInput from '../components/chat/MessageInput';
import ChatHeader from '../components/chat/ChatHeader';
import ChatMessagesPanel from '../components/chat/ChatMessagesPanel';
import ChatSidebar from '../components/chat/ChatSidebar';
import Spinner from '../components/ui/Spinner';
import { ChatSessionProvider, useChatSession } from '../contexts/ChatSessionContext';

function ChatPageContent() {
  const {
    sessionId,
    targetLanguageCode,
    isLoading,
    isSending,
    sendMessage,
  } = useChatSession();

  const [showSummaryPanel, setShowSummaryPanel] = useState(true);

  if (isLoading) {
    return (
      <Layout>
        <div className="flex min-h-[60vh] items-center justify-center">
          <Spinner size="lg" />
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="relative flex gap-4">
        <div className="flex-1 flex h-[calc(100vh-8rem)] flex-col rounded-2xl border border-slate-200 bg-white shadow-soft-lg overflow-hidden">
          {/* Header */}
          <ChatHeader />

          {/* Messages Container */}
          <ChatMessagesPanel />

          {/* Input */}
          <MessageInput
            onSend={sendMessage}
            disabled={isSending}
            languageCode={targetLanguageCode}
          />
        </div>

        {/* Sidebar */}
        {sessionId && <ChatSidebar isVisible={showSummaryPanel} />}

        {/* Toggle Tab - Always visible on right edge */}
        {sessionId && (
          <button
            onClick={() => setShowSummaryPanel(!showSummaryPanel)}
            className="absolute right-0 top-1/2 -translate-y-1/2 flex flex-col items-center gap-1.5 rounded-l-lg border border-r-0 border-slate-200 bg-white px-2.5 py-3 text-slate-600 hover:bg-slate-50 hover:text-brand-600 transition-all shadow-md z-10"
            title={showSummaryPanel ? 'Hide summary stats' : 'Show summary stats'}
            style={{ right: showSummaryPanel ? '320px' : '0' }}
          >
            <svg
              className="w-5 h-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
              />
            </svg>
            <svg
              className={`w-3 h-3 transition-transform ${showSummaryPanel ? 'rotate-0' : 'rotate-180'}`}
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          </button>
        )}
      </div>
    </Layout>
  );
}

export default function ChatPage() {
  const { sessionId } = useParams<{ sessionId: string }>();

  if (!sessionId) {
    return (
      <Layout>
        <div className="flex min-h-[60vh] items-center justify-center">
          <p className="text-slate-600">Session not found</p>
        </div>
      </Layout>
    );
  }

  return (
    <ChatSessionProvider sessionId={sessionId}>
      <ChatPageContent />
    </ChatSessionProvider>
  );
}
