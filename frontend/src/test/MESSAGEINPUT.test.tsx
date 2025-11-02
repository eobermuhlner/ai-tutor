import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import MessageInput from '../../src/components/chat/MessageInput';

describe('MessageInput', () => {
  const mockOnSend = vi.fn();
  const mockOnCancel = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders correctly with default props', () => {
    render(<MessageInput onSend={mockOnSend} />);
    
    expect(screen.getByPlaceholderText('Type your message... (Shift+Enter for newline)')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Send' })).toBeInTheDocument();
  });

  it('allows typing and sends message when send button clicked', () => {
    render(<MessageInput onSend={mockOnSend} />);
    
    const textarea = screen.getByPlaceholderText('Type your message... (Shift+Enter for newline)');
    const sendButton = screen.getByRole('button', { name: 'Send' });
    
    fireEvent.change(textarea, { target: { value: 'Hello, world!' } });
    fireEvent.click(sendButton);
    
    expect(mockOnSend).toHaveBeenCalledWith('Hello, world!');
    expect(textarea).toHaveValue('');
  });

  it('sends message when Enter is pressed without Shift', () => {
    render(<MessageInput onSend={mockOnSend} />);
    
    const textarea = screen.getByPlaceholderText('Type your message... (Shift+Enter for newline)');
    
    fireEvent.change(textarea, { target: { value: 'Hello!' } });
    fireEvent.keyDown(textarea, { key: 'Enter', shiftKey: false });
    
    expect(mockOnSend).toHaveBeenCalledWith('Hello!');
  });

  it('does not send message when Shift+Enter is pressed', () => {
    render(<MessageInput onSend={mockOnSend} />);
    
    const textarea = screen.getByPlaceholderText('Type your message... (Shift+Enter for newline)');
    
    fireEvent.change(textarea, { target: { value: 'Hello!' } });
    fireEvent.keyDown(textarea, { key: 'Enter', shiftKey: true });
    
    expect(mockOnSend).not.toHaveBeenCalled();
    expect(textarea).toHaveValue('Hello!');
  });

  it('disables send button when message is empty', () => {
    render(<MessageInput onSend={mockOnSend} />);
    
    const sendButton = screen.getByRole('button', { name: 'Send' });
    expect(sendButton).toBeDisabled();
  });

  it('enables send button when message has content', () => {
    render(<MessageInput onSend={mockOnSend} />);
    
    const textarea = screen.getByPlaceholderText('Type your message... (Shift+Enter for newline)');
    fireEvent.change(textarea, { target: { value: 'Hello!' } });
    
    const sendButton = screen.getByRole('button', { name: 'Send' });
    expect(sendButton).not.toBeDisabled();
  });

  it('shows cancel button when disabled and onCancel is provided', () => {
    render(<MessageInput onSend={mockOnSend} onCancel={mockOnCancel} disabled={true} />);
    
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Send' })).not.toBeInTheDocument();
  });

  it('trims whitespace from message before sending', () => {
    render(<MessageInput onSend={mockOnSend} />);
    
    const textarea = screen.getByPlaceholderText('Type your message... (Shift+Enter for newline)');
    fireEvent.change(textarea, { target: { value: '  Hello, world!  ' } });
    
    fireEvent.click(screen.getByRole('button', { name: 'Send' }));
    expect(mockOnSend).toHaveBeenCalledWith('Hello, world!');
  });

  it('does not send message when disabled', () => {
    render(<MessageInput onSend={mockOnSend} disabled={true} />);
    
    const textarea = screen.getByPlaceholderText('Type your message... (Shift+Enter for newline)');
    fireEvent.change(textarea, { target: { value: 'Hello!' } });
    
    const sendButton = screen.getByRole('button', { name: 'Send' });
    expect(sendButton).toBeDisabled();
    fireEvent.click(sendButton);
    
    expect(mockOnSend).not.toHaveBeenCalled();
  });

  it('focuses textarea when not disabled', () => {
    render(<MessageInput onSend={mockOnSend} />);
    
    const textarea = screen.getByPlaceholderText('Type your message... (Shift+Enter for newline)');
    expect(textarea).toHaveFocus();
  });
});