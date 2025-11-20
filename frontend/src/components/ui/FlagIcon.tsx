import ReactCountryFlag from 'react-country-flag';
import { Globe } from 'lucide-react';

interface FlagIconProps {
  /**
   * Language code (e.g., "de-DE", "es-ES", "en-US", "ipa")
   * Will extract the country code from the locale or handle special codes
   */
  languageCode: string;

  /**
   * Optional size multiplier (e.g., 1.5, 2, 3)
   * Default is 1em (inherits from parent font size)
   */
  size?: number;

  /**
   * Additional CSS classes
   */
  className?: string;

  /**
   * Aria label for accessibility
   */
  ariaLabel?: string;
}

/**
 * Extract ISO 3166-1 alpha-2 country code from language code
 * @example extractCountryCode('de-DE') => 'DE'
 * @example extractCountryCode('es-ES') => 'ES'
 * @example extractCountryCode('en-US') => 'US'
 * @example extractCountryCode('zh-CN') => 'CN'
 */
function extractCountryCode(languageCode: string): string {
  const parts = languageCode.split('-');

  // If there's a region code (e.g., "de-DE"), use it
  if (parts.length > 1) {
    return parts[1].toUpperCase();
  }

  // Otherwise, use the language code itself (e.g., "de" -> "DE")
  return parts[0].toUpperCase();
}

export default function FlagIcon({
  languageCode,
  size = 1,
  className = '',
  ariaLabel
}: FlagIconProps) {
  // Handle special language codes that don't have country flags
  const lowerCaseCode = languageCode.toLowerCase();
  if (lowerCaseCode.includes('ipa')) {
    // For IPA (International Phonetic Alphabet), use a globe icon
    // Use the same sizing logic as flags for consistency
    const ipaWidth = size <= 1.5
      ? (size <= 1 ? 16 : 20)
      : Math.round((size <= 1 ? 16 : size <= 1.5 ? 20 : 32) * (size / 3.5));
    const ipaHeight = size <= 1.5
      ? (size <= 1 ? 12 : 15)
      : Math.round((size <= 1 ? 12 : size <= 1.5 ? 15 : 24) * (size / 3.5));

    // Calculate container dimensions to match flag containers
    const ipaContainerWidth = size <= 1.5 ? `${size * 16}px` : `${ipaWidth}px`;
    const ipaContainerHeight = size <= 1.5 ? `${size * 12}px` : `${ipaHeight}px`;

    return (
      <div
        className={`inline-flex items-center justify-center ${className}`}
        style={{
          fontSize: `${size}em`,
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: ipaContainerWidth,
          height: ipaContainerHeight
        }}
      >
        <div
          style={{
            width: `${ipaWidth}px`,
            height: `${ipaHeight}px`,
            border: '1px solid #d1d5db', // gray-300 equivalent
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            overflow: 'hidden',
            borderRadius: '2px' // Add slight rounding for aesthetics
          }}
        >
          <Globe
            aria-label={ariaLabel || 'International Phonetic Alphabet symbol'}
            style={{
              width: `${ipaWidth * 0.8}px`, // Slightly smaller than container for padding
              height: `${ipaHeight * 0.8}px`
            }}
          />
        </div>
      </div>
    );
  }

  // For all other language codes, extract country code and use flag
  const originalCountryCode = extractCountryCode(languageCode);

  // Handle special country code mappings for react-country-flag
  const specialMappings: Record<string, string> = {
    'GB': 'GB-UKM', // United Kingdom (UK Monarchy) - main UK flag
  };

  // Apply special mapping if it exists, otherwise use the original code
  // For react-country-flag, we'll first try the special mapping if it exists
  const countryCode = specialMappings[originalCountryCode] || originalCountryCode;

  // Calculate the base dimensions before scaling
  const baseWidth = size <= 1 ? 16 : size <= 1.5 ? 20 : 32;
  const baseHeight = size <= 1 ? 12 : size <= 1.5 ? 15 : 24;

  // Calculate the scaling factor differently to support both legacy and enhanced sizing
  // For smaller sizes (typical usage like headers), keep them similar to original
  // For larger sizes (like in language catalog), use enhanced scaling
  let scaleAdjustment;
  if (size <= 1.5) {
    // For smaller sizes, keep them closer to their original visual appearance
    // This maintains backward compatibility for headers, etc.
    scaleAdjustment = size;  // size=1 remains visually similar to original
  } else {
    // For larger sizes, use relative scaling to the original 3.5 baseline
    // So size=3.5 gives scale factor of 1 (original visual size)
    // And size=7 gives scale factor of 2 (double visual size)
    scaleAdjustment = size / 3.5;
  }

  // Calculate dimensions based on size multiplier with 4:3 aspect ratio
  // For backward compatibility with existing UI, calculate container differently based on size range
  let containerWidth, containerHeight;
  if (size <= 1.5) {
    // For smaller sizes, use original calculation to maintain compatibility
    containerWidth = `${size * 16}px`;
    containerHeight = `${size * 12}px`;
  } else {
    // For larger sizes, ensure container can accommodate the scaled content
    const scaledWidth = baseWidth * scaleAdjustment;
    const scaledHeight = baseHeight * scaleAdjustment;
    containerWidth = `${scaledWidth}px`;
    containerHeight = `${scaledHeight}px`;
  }

  // For react-country-flag, we need to extract just the country code part
  // If special mapping like GB-UKM is used, extract the first two characters as the country code
  const getFlagCode = (code: string) => {
    if (code.includes('-') && code.length > 2) {
      return code.substring(0, 2); // Extract country code part (e.g., "GB" from "GB-UKM")
    }
    return code;
  };

  const flagCode = getFlagCode(countryCode);

  // Calculate flag dimensions based on container size
  // For smaller sizes (<=1.5), use the direct width/height
  // For larger sizes, use the scaled dimensions
  const flagWidth = size <= 1.5
    ? (size <= 1 ? 16 : 20)
    : Math.round(baseWidth * scaleAdjustment);
  const flagHeight = size <= 1.5
    ? (size <= 1 ? 12 : 15)
    : Math.round(baseHeight * scaleAdjustment);

  // ReactCountryFlag component with explicit pixel dimensions
  const FlagComponent = ({ code, width, height }: { code: string; width: number; height: number }) => (
    <ReactCountryFlag
      countryCode={code}
      svg
      style={{
        width: `${width}px`,
        height: `${height}px`,
        fontSize: `${height}px`, // Helps with emoji fallback sizing
      }}
      title={code}
    />
  );

  return (
    <div
      className={`inline-flex items-center justify-center ${className}`}
      style={{
        fontSize: `${size}em`,
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: containerWidth,
        height: containerHeight
      }}
    >
      {size <= 1.5 ? (
        // For smaller sizes, use original approach to maintain compatibility
        <div
          className="border border-gray-300"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: `${flagWidth}px`,
            height: `${flagHeight}px`,
            overflow: 'hidden'
          }}
        >
          <FlagComponent code={flagCode} width={flagWidth} height={flagHeight} />
        </div>
      ) : (
        // For larger sizes, use direct sizing without transform scaling
        <div
          style={{
            width: `${flagWidth}px`,
            height: `${flagHeight}px`,
            border: '1px solid #d1d5db', // gray-300 equivalent
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            overflow: 'hidden'
          }}
        >
          <FlagComponent code={flagCode} width={flagWidth} height={flagHeight} />
        </div>
      )}
    </div>
  );
}
