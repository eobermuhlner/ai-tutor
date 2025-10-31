import type { Language } from '../types';

/**
 * Extract region code from locale string
 * @example extractRegion('es-ES') => 'ES'
 * @example extractRegion('de-CH') => 'CH'
 */
export function extractRegion(localeCode: string): string | null {
  const parts = localeCode.split('-');
  return parts.length > 1 ? parts[1] : null;
}

/**
 * Format language display with flag emoji and native name
 * @example formatLanguageDisplay(language) => "🇪🇸 Español (España)"
 */
export function formatLanguageDisplay(language: Language): string {
  return `${language.flagEmoji} ${language.nativeName}`;
}

/**
 * Generate accessible label for language with English translation
 * @example getLanguageAriaLabel(language) => "Español (España) - Spanish (Spain)"
 */
export function getLanguageAriaLabel(language: Language): string {
  return `${language.nativeName} - ${language.name}`;
}

/**
 * Format compact language display (for space-constrained areas)
 * @example formatCompactLanguageDisplay(language) => "🇪🇸 Español"
 */
export function formatCompactLanguageDisplay(language: Language): string {
  // Show flag + first part of native name (before parentheses if present)
  const baseName = language.nativeName.split('(')[0].trim();
  return `${language.flagEmoji} ${baseName}`;
}
