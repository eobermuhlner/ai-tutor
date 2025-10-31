import { useState } from 'react';
import type { ReactNode } from 'react';

interface TooltipProps {
  children: ReactNode;
  title: string;
  position?: 'top' | 'bottom' | 'left' | 'right';
}

export default function Tooltip({ children, title, position = 'top' }: TooltipProps) {
  const [isVisible, setIsVisible] = useState(false);

  const positionClasses = {
    top: 'bottom-full left-1/2 transform -translate-x-1/2 mb-2',
    bottom: 'top-full left-1/2 transform -translate-x-1/2 mt-2',
    left: 'right-full top-1/2 transform -translate-y-1/2 mr-2',
    right: 'left-full top-1/2 transform -translate-y-1/2 ml-2',
  };

  return (
    <div className="relative inline-block">
      <div
        onMouseEnter={() => setIsVisible(true)}
        onMouseLeave={() => setIsVisible(false)}
        className="cursor-pointer"
      >
        {children}
      </div>
      {isVisible && (
        <div
          className={`absolute z-50 whitespace-nowrap bg-white text-slate-900 text-xs rounded py-2 px-3 shadow-lg border border-slate-200 ${positionClasses[position]}`}
        >
          {title}
          <div className={`absolute w-0 h-0 border-4 ${position === 'top' ? 'top-full left-1/2 transform -translate-x-1/2 border-t-white border-l-transparent border-r-transparent border-b-transparent' : ''} ${position === 'bottom' ? 'bottom-full left-1/2 transform -translate-x-1/2 border-b-white border-l-transparent border-r-transparent border-t-transparent' : ''} ${position === 'left' ? 'right-full top-1/2 transform -translate-y-1/2 border-l-white border-t-transparent border-b-transparent border-r-transparent' : ''} ${position === 'right' ? 'left-full top-1/2 transform -translate-y-1/2 border-r-white border-t-transparent border-b-transparent border-l-transparent' : ''}`}></div>
        </div>
      )}
    </div>
  );
}