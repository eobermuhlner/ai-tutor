import { useState } from 'react';
import { useParams } from 'react-router-dom';
import Layout from '../components/layout/Layout';
import MessageInput from '../components/chat/MessageInput';
import ChatHeader from '../components/chat/ChatHeader';
import ChatMessagesPanel from '../components/chat/ChatMessagesPanel';
import ChatSidebar from '../components/chat/ChatSidebar';
import ChatBackdrop from '../components/chat/ChatBackdrop';
import Spinner from '../components/ui/Spinner';
import { ChatSessionProvider, useChatSession } from '../contexts/ChatSessionContext';

function ChatPageContent() {
  const {
    sessionId,
    targetLanguageCode,
    isLoading,
    isSending,
    cancelSendMessage,
    sendMessage,
  } = useChatSession();

  const [showSummaryPanel, setShowSummaryPanel] = useState(false);

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
      <div className={`relative md:flex ${showSummaryPanel ? 'md:gap-4' : ''}`}>
        {/* Backdrop for mobile bottom sheet */}
        {sessionId && (
          <ChatBackdrop
            isVisible={showSummaryPanel}
            onClick={() => setShowSummaryPanel(false)}
          />
        )}

        {/* Main chat area */}
        <div className="flex-1 flex max-h-[calc(100vh-10rem)] flex-col rounded-2xl border border-slate-200 bg-white shadow-soft-lg overflow-hidden">
          {/* Header */}
          <ChatHeader
            showSidebar={showSummaryPanel}
            onToggleSidebar={() => setShowSummaryPanel(!showSummaryPanel)}
          />

          {/* Messages Container */}
          <ChatMessagesPanel />

          {/* Input */}
          <MessageInput
            onSend={sendMessage}
            onCancel={cancelSendMessage}
            disabled={isSending}
            languageCode={targetLanguageCode}
          />
        </div>

        {/* Sidebar - responsive: bottom sheet on mobile, side panel on desktop */}
        {sessionId && (
          <ChatSidebar
            isVisible={showSummaryPanel}
            onClose={() => setShowSummaryPanel(false)}
          />
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
