import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { act } from 'react-dom/test-utils';
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
    // Initially should show Copy icon (no text-green-600 class which only Check icon has)
    const svg = screen.getByRole('button').querySelector('svg');
    expect(svg).toBeInTheDocument();
    expect(svg).not.toHaveClass('text-green-600');
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
    
    // Check that the check icon appears (with text-green-600 class that only Check icon has)
    await waitFor(() => {
      expect(screen.getByRole('button').querySelector('svg.text-green-600')).toBeInTheDocument();
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

  it.skip('resets to copy icon after timeout', async () => {
    vi.useFakeTimers();
    mockWriteText.mockResolvedValue(undefined);
    
    const { container, rerender } = render(<CopyButton text="test content" />);

    const button = screen.getByRole('button');
    fireEvent.click(button);
    
    // Should have check icon now after click (with text-green-600 class)
    await waitFor(() => {
      expect(container.querySelector('svg.text-green-600')).toBeInTheDocument();
    }, { timeout: 2000 });
    
    // Fast forward time to trigger the reset
    act(() => {
      vi.advanceTimersByTime(2000);
    });
    
    // Force a re-render to update the UI
    rerender(<CopyButton text="test content" />);
    
    // After timeout, check that the check icon is no longer present
    await waitFor(() => {
      expect(container.querySelector('svg.text-green-600')).not.toBeInTheDocument();
    }, { timeout: 2000 });
    
    vi.useRealTimers();
  });

  it.skip('handles clipboard error gracefully', async () => {
    // Mock clipboard API to throw an error
    const error = new Error('Clipboard error');
    mockWriteText.mockRejectedValue(error);
    
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    
    render(<CopyButton text="test content" />);
    fireEvent.click(screen.getByRole('button'));
    
    // Wait for copy to fail and console.error to be called
    await waitFor(() => {
      expect(consoleSpy).toHaveBeenCalledWith('Failed to copy text: ', error);
    }, { timeout: 1000 });
    
    consoleSpy.mockRestore();
  });
});