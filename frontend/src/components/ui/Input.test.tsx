import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Input from './Input';

describe('Input', () => {
  it('renders with default props', () => {
    render(<Input />);
    const inputElement = screen.getByRole('textbox');
    expect(inputElement).toBeInTheDocument();
    expect(inputElement).not.toHaveAttribute('disabled');
    expect(inputElement).not.toHaveAttribute('required');
  });

  it('renders with custom props', () => {
    render(
      <Input 
        placeholder="Enter text" 
        defaultValue="test value" 
        disabled={false} 
        required={true}
        className="custom-class"
      />
    );
    const inputElement = screen.getByRole('textbox');
    expect(inputElement).toHaveAttribute('placeholder', 'Enter text');
    expect(inputElement).toHaveValue('test value');
    expect(inputElement).toHaveAttribute('required');
    expect(inputElement).toHaveClass('custom-class');
  });

  it('renders with label when provided', () => {
    render(<Input label="Username" id="username" />);
    const labelElement = screen.getByText('Username');
    const inputElement = screen.getByRole('textbox');
    
    expect(labelElement).toBeInTheDocument();
    expect(inputElement).toHaveAttribute('id', 'username');
  });

  it('shows error message when error prop is provided', () => {
    render(<Input error="This field is required" />);
    const inputElement = screen.getByRole('textbox');
    const errorElement = screen.getByText('This field is required');
    
    expect(inputElement).toHaveClass('border-red-500');
    expect(errorElement).toBeInTheDocument();
  });

  it('does not show error message when error prop is not provided', () => {
    render(<Input />);
    const inputElement = screen.getByRole('textbox');
    
    expect(inputElement).not.toHaveClass('border-red-500');
    expect(screen.queryByText(/This field is required|error/i)).not.toBeInTheDocument(); // No error text should be present
  });
});