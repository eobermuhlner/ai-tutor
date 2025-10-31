import { useState, useRef, useEffect } from 'react';
import type { Correction } from '../../types';
import { ErrorSeverity } from '../../types';
import { CorrectionTooltip } from './CorrectionTooltip';
import CopyButton from './CopyButton';

interface CorrectedTextProps {
  text: string;
  corrections: Correction[];
}

interface TextSegment {
  text: string;
  correction?: Correction;
}

const severityBorderColors: Record<ErrorSeverity, string> = {
  [ErrorSeverity.CRITICAL]: 'border-b-2 border-red-400 cursor-pointer',
  [ErrorSeverity.HIGH]: 'border-b-2 border-orange-400 cursor-pointer',
  [ErrorSeverity.MEDIUM]: 'border-b-2 border-yellow-400 cursor-pointer',
  [ErrorSeverity.LOW]: 'border-b-2 border-blue-400 cursor-pointer',
};

function splitTextIntoSegments(
  text: string,
  corrections: Correction[]
): TextSegment[] {
  if (corrections.length === 0) {
    return [{ text }];
  }

  // Sort corrections by startIndex
  const sortedCorrections = [...corrections].sort(
    (a, b) => a.startIndex - b.startIndex
  );

  const segments: TextSegment[] = [];
  let currentIndex = 0;

  for (const correction of sortedCorrections) {
    // Add text before correction
    if (currentIndex < correction.startIndex) {
      segments.push({
        text: text.substring(currentIndex, correction.startIndex),
      });
    }

    // Add corrected segment
    segments.push({
      text: correction.originalText,
      correction,
    });

    currentIndex = correction.endIndex;
  }

  // Add remaining text
  if (currentIndex < text.length) {
    segments.push({
      text: text.substring(currentIndex),
    });
  }

  return segments;
}

export function CorrectedText({ text, corrections }: CorrectedTextProps) {
  const [activeCorrection, setActiveCorrection] = useState<Correction | null>(
    null
  );
  const [tooltipPosition, setTooltipPosition] = useState<{
    top: number;
    left: number;
  } | null>(null);
  const spanRefs = useRef<Map<number, HTMLSpanElement>>(new Map());
  const tooltipRef = useRef<HTMLDivElement>(null);

  const segments = splitTextIntoSegments(text, corrections);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        tooltipRef.current &&
        !tooltipRef.current.contains(event.target as Node) &&
        !Array.from(spanRefs.current.values()).some((span) =>
          span.contains(event.target as Node)
        )
      ) {
        setActiveCorrection(null);
        setTooltipPosition(null);
      }
    }

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleCorrectionInteraction = (
    correction: Correction,
    index: number,
    event: React.MouseEvent | React.KeyboardEvent
  ) => {
    event.stopPropagation();
    const span = spanRefs.current.get(index);
    if (!span) return;

    const rect = span.getBoundingClientRect();
    const tooltipTop = rect.bottom + window.scrollY + 8;
    const tooltipLeft = rect.left + window.scrollX;

    setActiveCorrection(correction);
    setTooltipPosition({
      top: tooltipTop,
      left: tooltipLeft,
    });
  };

  const handleKeyDown = (
    correction: Correction,
    index: number,
    event: React.KeyboardEvent
  ) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      handleCorrectionInteraction(correction, index, event);
    } else if (event.key === 'Escape') {
      setActiveCorrection(null);
      setTooltipPosition(null);
    }
  };

  return (
    <div className="relative group">
      <div className="absolute -top-2 -right-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200 focus-within:opacity-100">
        <CopyButton text={text} title="Copy corrected text" />
      </div>
      <span className="inline leading-relaxed">
        {segments.map((segment, index) => {
          if (!segment.correction) {
            return <span key={index}>{segment.text}</span>;
          }

          const correction = segment.correction;
          const isActive = activeCorrection === correction;

          return (
            <span
              key={index}
              ref={(el) => {
                if (el) {
                  spanRefs.current.set(index, el);
                } else {
                  spanRefs.current.delete(index);
                }
              }}
              className={`${severityBorderColors[correction.severity]} ${
                isActive ? 'bg-blue-200' : ''
              } hover:bg-blue-200 transition-colors`}
              role="button"
              tabIndex={0}
              aria-label={`Error: ${correction.errorType}, press Enter for details`}
              onClick={(e) => handleCorrectionInteraction(correction, index, e)}
              onKeyDown={(e) => handleKeyDown(correction, index, e)}
            >
              {segment.text}
            </span>
          );
        })}
      </span>

      {/* Tooltip */}
      {activeCorrection && tooltipPosition && (
        <div
          ref={tooltipRef}
          className="fixed z-50"
          style={{
            top: `${tooltipPosition.top}px`,
            left: `${tooltipPosition.left}px`,
          }}
        >
          <CorrectionTooltip correction={activeCorrection} />
        </div>
      )}
    </div>
  );
}
