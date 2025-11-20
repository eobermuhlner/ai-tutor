import Flag from 'react-flagpack';
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
    // Calculate the visual dimensions after scaling
    const baseSize = size <= 1 ? 12 : size <= 1.5 ? 15 : 24;
    const actualVisualSize = baseSize * size; // Scale relative to 1, not 3.5

    return (
      <div
        className={`inline-flex items-center justify-center ${className}`}
        style={{
          fontSize: `${size}em`,
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: `${size * 16}px`,
          height: `${size * 12}px` // Maintain 4:3 aspect ratio
        }}
      >
        <div
          style={{
            width: `${actualVisualSize}px`,
            height: `${actualVisualSize}px`,
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
              width: `${actualVisualSize * 0.8}px`, // Slightly smaller than container for padding
              height: `${actualVisualSize * 0.8}px`
            }}
          />
        </div>
      </div>
    );
  }

  // For all other language codes, extract country code and use flag
  let countryCode = extractCountryCode(languageCode);

  // Handle special country code mappings for react-flagpack
  // Map standard country codes to special flag variants in flagpack-core
  const specialMappings: Record<string, string> = {
    'GB': 'GB-UKM', // United Kingdom (UK Monarchy) - main UK flag
  };

  // Apply special mapping if it exists, otherwise use the original code
  countryCode = specialMappings[countryCode] || countryCode;

  // Map size multiplier to predefined flagpack sizes
  let flagSize: string = 'm'; // Default to 'm' (lowercase)
  if (size <= 1) {
    flagSize = 's'; // Small
  } else if (size <= 1.5) {
    flagSize = 'm'; // Medium
  } else {
    flagSize = 'l'; // Large
  }

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
            width: size <= 1 ? '16px' : size <= 1.5 ? '20px' : '32px',
            height: size <= 1 ? '12px' : size <= 1.5 ? '15px' : '24px', // Maintain 4:3 aspect ratio
            overflow: 'hidden'
          }}
        >
          <Flag
            code={countryCode as any} // Bypass TypeScript error for Flags type
            size={flagSize}
            hasBorder={false}
            hasDropShadow={false}
            ariaLabel={ariaLabel || `Flag for ${languageCode}`}
          />
        </div>
      ) : (
        // For larger sizes, use the enhanced scaling approach
        <div
          style={{
            width: `${baseWidth * scaleAdjustment}px`,
            height: `${baseHeight * scaleAdjustment}px`,
            border: '1px solid #d1d5db', // gray-300 equivalent
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            overflow: 'hidden'
          }}
        >
          <div
            style={{
              transform: `scale(${scaleAdjustment})`,
              transformOrigin: 'center',
            }}
          >
            <Flag
              code={countryCode as any} // Bypass TypeScript error for Flags type
              size={flagSize}
              hasBorder={false}
              hasDropShadow={false}
              ariaLabel={ariaLabel || `Flag for ${languageCode}`}
            />
          </div>
        </div>
      )}
    </div>
  );
}
