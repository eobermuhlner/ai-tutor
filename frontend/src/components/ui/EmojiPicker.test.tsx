import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import EmojiPicker from './EmojiPicker';

describe('EmojiPicker', () => {
  const mockOnSelect = vi.fn();
  const mockOnClose = vi.fn();

  beforeEach(() => {
    mockOnSelect.mockClear();
    mockOnClose.mockClear();
  });

  it('renders with default props', () => {
    render(<EmojiPicker onSelect={mockOnSelect} onClose={mockOnClose} />);
    
    expect(screen.getByText('Pick an Emoji')).toBeInTheDocument();
    expect(screen.getByLabelText('Close')).toBeInTheDocument();
  });

  it('calls onClose when close button is clicked', () => {
    render(<EmojiPicker onSelect={mockOnSelect} onClose={mockOnClose} />);
    
    const closeButton = screen.getByLabelText('Close');
    fireEvent.click(closeButton);
    
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  it('calls onClose when backdrop is clicked', () => {
    render(<EmojiPicker onSelect={mockOnSelect} onClose={mockOnClose} />);
    
    // The backdrop is the outermost fixed element with the bg-black class
    const backdrop = document.querySelector('.fixed.inset-0.bg-black.bg-opacity-50') as HTMLElement;
    
    if (backdrop) {
      fireEvent.click(backdrop);
    }
    
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  it('displays emoji categories', () => {
    render(<EmojiPicker onSelect={mockOnSelect} onClose={mockOnClose} />);
    
    expect(screen.getByText('People')).toBeInTheDocument();
    expect(screen.getByText('Smileys')).toBeInTheDocument();
    expect(screen.getByText('Animals')).toBeInTheDocument();
    expect(screen.getByText('Objects')).toBeInTheDocument();
    expect(screen.getByText('Flags')).toBeInTheDocument();
  });

  it('changes active category when a category button is clicked', () => {
    render(<EmojiPicker onSelect={mockOnSelect} onClose={mockOnClose} />);
    
    const smileysButton = screen.getByText('Smileys');
    fireEvent.click(smileysButton);
    
    // Check that smileys button is now active (has different styling)
    expect(smileysButton).toHaveClass('bg-blue-500');
  });

  it('calls onSelect and onClose when an emoji is clicked', () => {
    render(<EmojiPicker onSelect={mockOnSelect} onClose={mockOnClose} />);
    
    // Click on the first emoji in the default category (People category)
    const emojiButton = screen.getByTitle('👨‍🏫');
    fireEvent.click(emojiButton);
    
    expect(mockOnSelect).toHaveBeenCalledWith('👨‍🏫');
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });
});