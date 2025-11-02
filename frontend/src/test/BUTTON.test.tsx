import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import Button from '../../src/components/ui/Button';

describe('Button', () => {
  it('renders children correctly', () => {
    render(<Button>Click me</Button>);
    expect(screen.getByText('Click me')).toBeInTheDocument();
  });

  it('applies primary variant by default', () => {
    render(<Button>Button</Button>);
    const button = screen.getByRole('button');
    expect(button).toHaveClass('from-brand-600');
    expect(button).toHaveClass('to-brand-700');
    expect(button).toHaveClass('text-white');
  });

  it('applies different variants correctly', () => {
    const { rerender } = render(<Button variant="primary">Button</Button>);
    let button = screen.getByRole('button');
    expect(button).toHaveClass('from-brand-600');
    
    rerender(<Button variant="secondary">Button</Button>);
    button = screen.getByRole('button');
    expect(button).toHaveClass('bg-slate-100');
    expect(button).toHaveClass('text-slate-900');
    
    rerender(<Button variant="danger">Button</Button>);
    button = screen.getByRole('button');
    expect(button).toHaveClass('from-red-500');
    expect(button).toHaveClass('to-red-600');
    
    rerender(<Button variant="ghost">Button</Button>);
    button = screen.getByRole('button');
    expect(button).toHaveClass('bg-transparent');
    
    rerender(<Button variant="outline">Button</Button>);
    button = screen.getByRole('button');
    expect(button).toHaveClass('border-2');
    expect(button).toHaveClass('border-slate-300');
  });

  it('applies different sizes correctly', () => {
    const { rerender } = render(<Button size="sm">Button</Button>);
    let button = screen.getByRole('button');
    expect(button).toHaveClass('px-3');
    expect(button).toHaveClass('py-1.5');
    expect(button).toHaveClass('text-sm');
    
    rerender(<Button size="md">Button</Button>);
    button = screen.getByRole('button');
    expect(button).toHaveClass('px-5');
    expect(button).toHaveClass('py-2.5');
    expect(button).toHaveClass('text-base');
    
    rerender(<Button size="lg">Button</Button>);
    button = screen.getByRole('button');
    expect(button).toHaveClass('px-7');
    expect(button).toHaveClass('py-3.5');
    expect(button).toHaveClass('text-lg');
  });

  it('shows loading state with spinner', () => {
    render(<Button isLoading={true}>Click me</Button>);
    expect(screen.getByText('Loading...')).toBeInTheDocument();
    expect(screen.getByRole('button')).toBeInTheDocument();
    expect(screen.getByRole('button')).toBeDisabled();
    expect(screen.getByText('Loading...')).toBeInTheDocument();
    expect(screen.queryByText('Click me')).not.toBeInTheDocument();
  });

  it('applies custom className', () => {
    render(<Button className="custom-class">Button</Button>);
    const button = screen.getByRole('button');
    expect(button).toHaveClass('custom-class');
  });

  it('calls onClick when clicked', () => {
    const handleClick = vi.fn();
    render(<Button onClick={handleClick}>Click me</Button>);
    
    fireEvent.click(screen.getByRole('button'));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('is disabled when isLoading is true', () => {
    render(<Button isLoading={true}>Button</Button>);
    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
  });

  it('is disabled when disabled prop is true', () => {
    render(<Button disabled={true}>Button</Button>);
    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
  });

  it('applies base classes consistently', () => {
    render(<Button>Button</Button>);
    const button = screen.getByRole('button');
    expect(button).toHaveClass('inline-flex');
    expect(button).toHaveClass('items-center');
    expect(button).toHaveClass('justify-center');
    expect(button).toHaveClass('rounded-xl');
    expect(button).toHaveClass('font-medium');
  });
});