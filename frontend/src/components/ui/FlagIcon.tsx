/**
 * FlagIcon component - Displays country flags using flag-icons library
 *
 * Converts language codes (e.g., "de-DE", "es-ES") to ISO 3166-1 alpha-2 country codes
 * and renders them as SVG flags via CSS classes.
 *
 * @see https://github.com/lipis/flag-icons
 */

interface FlagIconProps {
  /**
   * Language code (e.g., "de-DE", "es-ES", "en-US")
   * Will extract the country code from the locale
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
 * @example extractCountryCode('de-DE') => 'de'
 * @example extractCountryCode('es-ES') => 'es'
 * @example extractCountryCode('en-US') => 'us'
 * @example extractCountryCode('zh-CN') => 'cn'
 */
function extractCountryCode(languageCode: string): string {
  const parts = languageCode.split('-');

  // If there's a region code (e.g., "de-DE"), use it
  if (parts.length > 1) {
    return parts[1].toLowerCase();
  }

  // Otherwise, use the language code itself (e.g., "de" -> "de")
  return parts[0].toLowerCase();
}

export default function FlagIcon({
  languageCode,
  size = 1,
  className = '',
  ariaLabel
}: FlagIconProps) {
  const countryCode = extractCountryCode(languageCode);

  return (
    <span
      className={`fi fi-${countryCode} ${className}`}
      style={{ fontSize: `${size}em` }}
      role="img"
      aria-label={ariaLabel || `Flag for ${languageCode}`}
    />
  );
}
