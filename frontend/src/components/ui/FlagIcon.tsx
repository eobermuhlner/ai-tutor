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
    return (
      <div
        className={`inline-flex items-center justify-center border border-gray-300 ${className}`}
        style={{
          fontSize: `${size}em`,
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: size <= 1 ? '16px' : size <= 1.5 ? '20px' : '32px',
          height: size <= 1 ? '12px' : size <= 1.5 ? '15px' : '24px' // Maintain rectangular aspect ratio
        }}
      >
        <Globe
          aria-label={ariaLabel || 'International Phonetic Alphabet symbol'}
          style={{
            width: size <= 1 ? '10px' : size <= 1.5 ? '13px' : '20px',
            height: size <= 1 ? '10px' : size <= 1.5 ? '13px' : '20px'
          }}
        />
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

  // Map sizes to rectangular dimensions based on flagpack's actual aspect ratios
  let containerWidth, containerHeight;
  switch(flagSize) {
    case 's':
      containerWidth = '16px';
      containerHeight = '12px'; // 4:3 ratio for small
      break;
    case 'm':
      containerWidth = '20px';
      containerHeight = '15px'; // 4:3 ratio for medium
      break;
    case 'l':
    default:
      containerWidth = '32px';
      containerHeight = '24px'; // 4:3 ratio for large
      break;
  }

  return (
    <div
      className={`inline-flex items-center justify-center border border-gray-300 ${className}`}
      style={{
        fontSize: `${size}em`,
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: containerWidth,
        height: containerHeight
      }}
    >
      <Flag
        code={countryCode as any} // Bypass TypeScript error for Flags type
        size={flagSize}
        hasBorder={false}
        hasDropShadow={false}
        aria-label={ariaLabel || `Flag for ${languageCode}`}
      />
    </div>
  );
}
