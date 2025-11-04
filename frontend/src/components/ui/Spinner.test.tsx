import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Spinner from './Spinner';

describe('Spinner', () => {
  it('renders with default props', () => {
    render(<Spinner />);
    const spinnerElement = screen.getByRole('status');
    expect(spinnerElement).toBeInTheDocument();
    expect(spinnerElement).toHaveClass('h-8', 'w-8'); // Default size is 'md'
  });

  it('renders with large size', () => {
    render(<Spinner size="lg" />);
    const spinnerElement = screen.getByRole('status');
    expect(spinnerElement).toHaveClass('h-12', 'w-12');
  });

  it('renders with small size', () => {
    render(<Spinner size="sm" />);
    const spinnerElement = screen.getByRole('status');
    expect(spinnerElement).toHaveClass('h-4', 'w-4');
  });

  it('renders with medium size', () => {
    render(<Spinner size="md" />);
    const spinnerElement = screen.getByRole('status');
    expect(spinnerElement).toHaveClass('h-8', 'w-8');
  });

  it('has default size when no size prop is provided', () => {
    render(<Spinner />);
    const spinnerElement = screen.getByRole('status');
    // Default size is medium (h-8 w-8), based on the component
    expect(spinnerElement).toHaveClass('h-8', 'w-8');
  });
});