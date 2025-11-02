import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import CopyButton from '../../src/components/chat/CopyButton';

// Mock navigator.clipboard
const mockWriteText = vi.fn();

Object.assign(navigator, {
  clipboard: {
    writeText: mockWriteText
  }
});

describe('CopyButton', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders with copy icon initially', () => {
    render(<CopyButton text="test content" />);
    expect(screen.getByRole('button')).toBeInTheDocument();
    expect(screen.getByRole('button').querySelector('svg')).toBeInTheDocument(); // Copy icon
  });

  it('copies text to clipboard when clicked', async () => {
    const testText = 'Hello, world!';
    render(<CopyButton text={testText} />);
    
    const button = screen.getByRole('button');
    fireEvent.click(button);
    
    await waitFor(() => {
      expect(mockWriteText).toHaveBeenCalledWith(testText);
    });
  });

  it('shows check icon after successful copy', async () => {
    mockWriteText.mockResolvedValue(undefined);
    
    render(<CopyButton text="test content" />);
    
    fireEvent.click(screen.getByRole('button'));
    
    // Check that the check icon appears (with class indicating check icon)
    await waitFor(() => {
      expect(screen.getByRole('button').querySelector('svg.lucide-check')).toBeInTheDocument(); // Check icon
    });
  });

  it('has default title and can be customized', () => {
    const { rerender } = render(<CopyButton text="test" />);
    expect(screen.getByRole('button')).toHaveAttribute('title', 'Copy to clipboard');
    
    rerender(<CopyButton text="test" title="Custom title" />);
    expect(screen.getByRole('button')).toHaveAttribute('title', 'Custom title');
  });

  it('has correct aria-label', () => {
    render(<CopyButton text="test" title="Copy text" />);
    expect(screen.getByRole('button')).toHaveAttribute('aria-label', 'Copy text');
  });

  it('applies custom className', () => {
    render(<CopyButton text="test" className="custom-class" />);
    expect(screen.getByRole('button')).toHaveClass('custom-class');
  });

  it('resets to copy icon after timeout', () => {
    vi.useFakeTimers();
    mockWriteText.mockResolvedValue(undefined);
    
    const { container } = render(<CopyButton text="test content" />);
    
    fireEvent.click(container.querySelector('button'));
    
    // Initially should show check icon (after click)
    vi.advanceTimersByTime(0); // Process the click state update
    
    // Should have check icon now (since we're mocking writeText to resolve immediately)
    expect(container.querySelector('svg.lucide-check')).toBeInTheDocument();
    
    // Fast forward time to trigger the reset
    vi.advanceTimersByTime(2000);
    
    // Should reset to copy icon after timeout
    expect(container.querySelector('svg.lucide-copy')).toBeInTheDocument();
    
    vi.useRealTimers();
  });

  it('handles clipboard error gracefully', async () => {
    // Mock clipboard API to throw an error
    const error = new Error('Clipboard error');
    mockWriteText.mockRejectedValue(error);
    
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    
    render(<CopyButton text="test content" />);
    fireEvent.click(screen.getByRole('button'));
    
    await waitFor(() => {
      expect(consoleSpy).toHaveBeenCalledWith('Failed to copy text: ', error);
    });
    
    consoleSpy.mockRestore();
  });
});