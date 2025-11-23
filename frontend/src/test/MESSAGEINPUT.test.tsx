import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import MessageInput from '../../src/components/chat/MessageInput';

// Mock context provider that provides minimal required values for MessageInput
const MockChatSessionProvider = ({ children }: { children: React.ReactNode }) => {
  return (
    // We'll use a Context.Provider with null value to avoid the error
    // In a real test, we'd provide a proper mock implementation
    // But for MessageInput specifically, we'll mock the hook instead
    <div>{children}</div>
  );
};

// Mock the useChatSession hook to return mock values
vi.mock('../../src/contexts/ChatSessionContext', () => ({
  useChatSession: () => ({
    isSending: false,
    vocabularyReviewMode: false,
  }),
}));

describe('MessageInput', () => {
  const mockOnSend = vi.fn();
  const mockOnCancel = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders correctly with default props', () => {
    render(
      <MockChatSessionProvider>
        <MessageInput onSend={mockOnSend} />
      </MockChatSessionProvider>
    );

    expect(screen.getByPlaceholderText('Type your message... (Shift+Enter for newline)')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Send' })).toBeInTheDocument();
  });

  it('allows typing and sends message when send button clicked', () => {
    render(
      <MockChatSessionProvider>
        <MessageInput onSend={mockOnSend} />
      </MockChatSessionProvider>
    );

    const textarea = screen.getByPlaceholderText('Type your message... (Shift+Enter for newline)');
    const sendButton = screen.getByRole('button', { name: 'Send' });

    fireEvent.change(textarea, { target: { value: 'Hello, world!' } });
    fireEvent.click(sendButton);

    expect(mockOnSend).toHaveBeenCalledWith('Hello, world!');
    expect(textarea).toHaveValue('');
  });

  it('sends message when Enter is pressed without Shift', () => {
    render(
      <MockChatSessionProvider>
        <MessageInput onSend={mockOnSend} />
      </MockChatSessionProvider>
    );

    const textarea = screen.getByPlaceholderText('Type your message... (Shift+Enter for newline)');

    fireEvent.change(textarea, { target: { value: 'Hello!' } });
    fireEvent.keyDown(textarea, { key: 'Enter', shiftKey: false });

    expect(mockOnSend).toHaveBeenCalledWith('Hello!');
  });

  it('does not send message when Shift+Enter is pressed', () => {
    render(
      <MockChatSessionProvider>
        <MessageInput onSend={mockOnSend} />
      </MockChatSessionProvider>
    );

    const textarea = screen.getByPlaceholderText('Type your message... (Shift+Enter for newline)');

    fireEvent.change(textarea, { target: { value: 'Hello!' } });
    fireEvent.keyDown(textarea, { key: 'Enter', shiftKey: true });

    expect(mockOnSend).not.toHaveBeenCalled();
    expect(textarea).toHaveValue('Hello!');
  });

  it('disables send button when message is empty', () => {
    render(
      <MockChatSessionProvider>
        <MessageInput onSend={mockOnSend} />
      </MockChatSessionProvider>
    );

    const sendButton = screen.getByRole('button', { name: 'Send' });
    expect(sendButton).toBeDisabled();
  });

  it('enables send button when message has content', () => {
    render(
      <MockChatSessionProvider>
        <MessageInput onSend={mockOnSend} />
      </MockChatSessionProvider>
    );

    const textarea = screen.getByPlaceholderText('Type your message... (Shift+Enter for newline)');
    fireEvent.change(textarea, { target: { value: 'Hello!' } });

    const sendButton = screen.getByRole('button', { name: 'Send' });
    expect(sendButton).not.toBeDisabled();
  });

  it('shows cancel button when disabled and onCancel is provided', () => {
    render(
      <MockChatSessionProvider>
        <MessageInput onSend={mockOnSend} onCancel={mockOnCancel} disabled={true} />
      </MockChatSessionProvider>
    );

    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Send' })).not.toBeInTheDocument();
  });

  it('trims whitespace from message before sending', () => {
    render(
      <MockChatSessionProvider>
        <MessageInput onSend={mockOnSend} />
      </MockChatSessionProvider>
    );

    const textarea = screen.getByPlaceholderText('Type your message... (Shift+Enter for newline)');
    fireEvent.change(textarea, { target: { value: '  Hello, world!  ' } });

    fireEvent.click(screen.getByRole('button', { name: 'Send' }));
    expect(mockOnSend).toHaveBeenCalledWith('Hello, world!');
  });

  it('does not send message when disabled', () => {
    render(
      <MockChatSessionProvider>
        <MessageInput onSend={mockOnSend} disabled={true} />
      </MockChatSessionProvider>
    );

    const textarea = screen.getByPlaceholderText('Type your message... (Shift+Enter for newline)');
    fireEvent.change(textarea, { target: { value: 'Hello!' } });

    const sendButton = screen.getByRole('button', { name: 'Send' });
    expect(sendButton).toBeDisabled();
    fireEvent.click(sendButton);

    expect(mockOnSend).not.toHaveBeenCalled();
  });

  it('focuses textarea when not disabled', () => {
    render(
      <MockChatSessionProvider>
        <MessageInput onSend={mockOnSend} />
      </MockChatSessionProvider>
    );

    const textarea = screen.getByPlaceholderText('Type your message... (Shift+Enter for newline)');
    expect(textarea).toHaveFocus();
  });
});