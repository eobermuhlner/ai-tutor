import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import EmptyState from './EmptyState';

describe('EmptyState', () => {
  it('renders with title and message', () => {
    render(<EmptyState title="No items found" message="There are no items to display" />);
    expect(screen.getByText('No items found')).toBeInTheDocument();
    expect(screen.getByText('There are no items to display')).toBeInTheDocument();
  });

  it('renders with an action button when provided', () => {
    const mockOnClick = vi.fn();
    render(
      <EmptyState 
        title="No items found" 
        message="There are no items to display"
        action={{ label: 'Try again', onClick: mockOnClick }}
      />
    );
    const button = screen.getByText('Try again');
    expect(button).toBeInTheDocument();
    
    fireEvent.click(button);
    expect(mockOnClick).toHaveBeenCalledTimes(1);
  });

  it('does not render action button when not provided', () => {
    render(<EmptyState title="No items found" message="There are no items to display" />);
    expect(screen.queryByText('Try again')).not.toBeInTheDocument();
  });

  it('renders different action button text', () => {
    const mockOnClick = vi.fn();
    render(
      <EmptyState 
        title="Empty"
        message="No data"
        action={{ label: 'Add Item', onClick: mockOnClick }}
      />
    );
    expect(screen.getByText('Add Item')).toBeInTheDocument();
  });

  it('calls different action function', () => {
    const mockOnClick = vi.fn();
    render(
      <EmptyState 
        title="No items found" 
        message="There are no items to display"
        action={{ label: 'Try again', onClick: mockOnClick }}
      />
    );
    
    fireEvent.click(screen.getByText('Try again'));
    expect(mockOnClick).toHaveBeenCalledTimes(1);
  });
});