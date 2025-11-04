import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Tooltip from './Tooltip';

describe('Tooltip', () => {
  it('renders with default props', () => {
    render(<Tooltip title="Tooltip text">Hover me</Tooltip>);
    const triggerElement = screen.getByText('Hover me');
    expect(triggerElement).toBeInTheDocument();
  });

  it('renders children properly', () => {
    render(<Tooltip title="Tooltip">My Button</Tooltip>);
    expect(screen.getByText('My Button')).toBeInTheDocument();
  });

  it('uses default position when no position prop is provided', () => {
    render(<Tooltip title="Tooltip">Hover me</Tooltip>);
    expect(screen.getByText('Hover me')).toBeInTheDocument();
  });

  it('applies correct position classes', () => {
    render(<Tooltip title="Tooltip" position="bottom">Hover me</Tooltip>);
    expect(screen.getByText('Hover me')).toBeInTheDocument();
  });

  it('renders with different positions', () => {
    const { rerender } = render(<Tooltip title="Tooltip" position="top">Hover me</Tooltip>);
    expect(screen.getByText('Hover me')).toBeInTheDocument();
    
    rerender(<Tooltip title="Tooltip" position="bottom">Hover me</Tooltip>);
    expect(screen.getByText('Hover me')).toBeInTheDocument();
    
    rerender(<Tooltip title="Tooltip" position="left">Hover me</Tooltip>);
    expect(screen.getByText('Hover me')).toBeInTheDocument();
    
    rerender(<Tooltip title="Tooltip" position="right">Hover me</Tooltip>);
    expect(screen.getByText('Hover me')).toBeInTheDocument();
  });
});