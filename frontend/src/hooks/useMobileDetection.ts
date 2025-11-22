import { useState, useEffect } from 'react';

/**
 * Hook to detect if the user is on a mobile device based on screen width
 * @param breakpoint - Width in pixels to consider as mobile threshold (default: 768px matching Tailwind's md breakpoint)
 * @returns true if screen width is below breakpoint
 */
export function useMobileDetection(breakpoint: number = 768): boolean {
  const [isMobile, setIsMobile] = useState<boolean>(() => {
    // Initialize with current window width
    if (typeof window !== 'undefined') {
      return window.innerWidth < breakpoint;
    }
    return false;
  });

  useEffect(() => {
    const handleResize = () => {
      setIsMobile(window.innerWidth < breakpoint);
    };

    // Add event listener
    window.addEventListener('resize', handleResize);

    // Call handler right away to set initial state
    handleResize();

    // Cleanup
    return () => window.removeEventListener('resize', handleResize);
  }, [breakpoint]);

  return isMobile;
}
