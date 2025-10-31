export interface KeyboardLayout {
  languageCode: string;
  languageName: string;
  simplified: string[] | string[][] | (string | null)[] | (string | null)[][]; // Flat array or 2D array with optional nulls
  full?: (string | null)[][];
  config?: {
    cols?: number; // Number of columns for grid layout (default: 10)
    buttonSize?: 'sm' | 'md' | 'lg'; // Button size (default: 'md')
  };
}

export const keyboardLayouts: Record<string, KeyboardLayout> = {
  es: {
    languageCode: 'es',
    languageName: 'Spanish',
    config: {
      cols: 8,
      buttonSize: 'md',
    },
    simplified: [
      // Lowercase
      ['á', 'é', 'í', 'ó', 'ú', 'ñ', 'ü'],
      // Uppercase
      ['Á', 'É', 'Í', 'Ó', 'Ú', 'Ñ', 'Ü'],
      // Punctuation
      ['¿', '¡'],
    ],
  },
  fr: {
    languageCode: 'fr',
    languageName: 'French',
    config: {
      cols: 10,
      buttonSize: 'md',
    },
    simplified: [
      // Lowercase
      ['à', 'â', 'æ', 'ç', 'é', 'è', 'ê', 'ë'],
      ['î', 'ï', 'ô', 'œ', 'ù', 'û', 'ü', 'ÿ'],
      // Uppercase
      ['À', 'Â', 'Æ', 'Ç', 'É', 'È', 'Ê', 'Ë'],
      ['Î', 'Ï', 'Ô', 'Œ', 'Ù', 'Û', 'Ü', 'Ÿ'],
    ],
  },
  de: {
    languageCode: 'de',
    languageName: 'German',
    config: {
      cols: 8,
      buttonSize: 'md',
    },
    simplified: [
      // Lowercase
      ['ä', 'ö', 'ü', 'ß'],
      // Uppercase
      ['Ä', 'Ö', 'Ü', null] as (string | null)[],
    ],
  },
  it: {
    languageCode: 'it',
    languageName: 'Italian',
    config: {
      cols: 10,
      buttonSize: 'md',
    },
    simplified: [
      // Lowercase
      ['à', 'è', 'é', 'ì', 'í', 'ò', 'ó', 'ù', 'ú'],
      // Uppercase
      ['À', 'È', 'É', 'Ì', 'Í', 'Ò', 'Ó', 'Ù', 'Ú'],
    ],
  },
  pt: {
    languageCode: 'pt',
    languageName: 'Portuguese',
    config: {
      cols: 10,
      buttonSize: 'md',
    },
    simplified: [
      // Lowercase
      ['á', 'â', 'ã', 'à', 'ç', 'é', 'ê', 'í'],
      ['ó', 'ô', 'õ', 'ú', 'ü'],
      // Uppercase
      ['Á', 'Â', 'Ã', 'À', 'Ç', 'É', 'Ê', 'Í'],
      ['Ó', 'Ô', 'Õ', 'Ú', 'Ü'],
    ],
  },
  pl: {
    languageCode: 'pl',
    languageName: 'Polish',
    simplified: [
      'ą', 'ć', 'ę', 'ł', 'ń', 'ó', 'ś', 'ź', 'ż',
      'Ą', 'Ć', 'Ę', 'Ł', 'Ń', 'Ó', 'Ś', 'Ź', 'Ż',
    ],
  },
  cs: {
    languageCode: 'cs',
    languageName: 'Czech',
    simplified: [
      'á', 'č', 'ď', 'é', 'ě', 'í', 'ň', 'ó', 'ř', 'š', 'ť', 'ú', 'ů', 'ý', 'ž',
      'Á', 'Č', 'Ď', 'É', 'Ě', 'Í', 'Ň', 'Ó', 'Ř', 'Š', 'Ť', 'Ú', 'Ů', 'Ý', 'Ž',
    ],
  },
  ro: {
    languageCode: 'ro',
    languageName: 'Romanian',
    simplified: ['ă', 'â', 'î', 'ș', 'ț', 'Ă', 'Â', 'Î', 'Ș', 'Ț'],
  },
  tr: {
    languageCode: 'tr',
    languageName: 'Turkish',
    simplified: ['ç', 'ğ', 'ı', 'ö', 'ş', 'ü', 'Ç', 'Ğ', 'İ', 'Ö', 'Ş', 'Ü'],
  },
  sv: {
    languageCode: 'sv',
    languageName: 'Swedish',
    simplified: ['å', 'ä', 'ö', 'Å', 'Ä', 'Ö'],
  },
  no: {
    languageCode: 'no',
    languageName: 'Norwegian',
    simplified: ['å', 'æ', 'ø', 'Å', 'Æ', 'Ø'],
  },
  da: {
    languageCode: 'da',
    languageName: 'Danish',
    simplified: ['å', 'æ', 'ø', 'Å', 'Æ', 'Ø'],
  },
  nl: {
    languageCode: 'nl',
    languageName: 'Dutch',
    simplified: [
      'á', 'à', 'ä', 'é', 'è', 'ë', 'í', 'ì', 'ï', 'ó', 'ò', 'ö', 'ú', 'ù', 'ü',
      'Á', 'À', 'Ä', 'É', 'È', 'Ë', 'Í', 'Ì', 'Ï', 'Ó', 'Ò', 'Ö', 'Ú', 'Ù', 'Ü',
    ],
  },
  ko: {
    languageCode: 'ko',
    languageName: 'Korean',
    config: {
      cols: 10,
      buttonSize: 'md',
    },
    simplified: [
      // Basic vowels
      ['ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ'],
      ['ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ'],
      ['ㅣ'],
      // Basic consonants
      ['ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ'],
      ['ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ'],
      ['ㅎ'],
    ],
  },
  el: {
    languageCode: 'el',
    languageName: 'Greek',
    config: {
      cols: 10,
      buttonSize: 'md',
    },
    simplified: [
      // Lowercase
      ['α', 'β', 'γ', 'δ', 'ε', 'ζ', 'η', 'θ', 'ι'],
      ['κ', 'λ', 'μ', 'ν', 'ξ', 'ο', 'π', 'ρ', 'ς'],
      ['σ', 'τ', 'υ', 'φ', 'χ', 'ψ', 'ω'],
      // Uppercase
      ['Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι'],
      ['Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', null] as (string | null)[],
      ['Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'],
    ],
  },
  ru: {
    languageCode: 'ru',
    languageName: 'Russian',
    config: {
      cols: 10,
      buttonSize: 'md',
    },
    simplified: [
      // Lowercase row 1
      ['а', 'б', 'в', 'г', 'д', 'е', 'ё', 'ж', 'з'],
      // Lowercase row 2
      ['и', 'й', 'к', 'л', 'м', 'н', 'о', 'п', 'р'],
      // Lowercase row 3
      ['с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ'],
      // Lowercase row 4
      ['ъ', 'ы', 'ь', 'э', 'ю', 'я'],
      // Uppercase row 1
      ['А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ё', 'Ж', 'З'],
      // Uppercase row 2
      ['И', 'Й', 'К', 'Л', 'М', 'Н', 'О', 'П', 'Р'],
      // Uppercase row 3
      ['С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ'],
      // Uppercase row 4
      ['Ъ', 'Ы', 'Ь', 'Э', 'Ю', 'Я'],
    ],
  },
  ja: {
    languageCode: 'ja',
    languageName: 'Japanese',
    config: {
      cols: 10,
      buttonSize: 'md',
    },
    simplified: [
      // Hiragana - Vowels row
      ['あ', 'い', 'う', 'え', 'お'],
      // K-row
      ['か', 'き', 'く', 'け', 'こ'],
      // G-row (dakuten)
      ['が', 'ぎ', 'ぐ', 'げ', 'ご'],
      // S-row
      ['さ', 'し', 'す', 'せ', 'そ'],
      // Z-row (dakuten)
      ['ざ', 'じ', 'ず', 'ぜ', 'ぞ'],
      // T-row
      ['た', 'ち', 'つ', 'て', 'と'],
      // D-row (dakuten)
      ['だ', 'ぢ', 'づ', 'で', 'ど'],
      // N-row
      ['な', 'に', 'ぬ', 'ね', 'の'],
      // H-row
      ['は', 'ひ', 'ふ', 'へ', 'ほ'],
      // B-row (dakuten)
      ['ば', 'び', 'ぶ', 'べ', 'ぼ'],
      // P-row (handakuten)
      ['ぱ', 'ぴ', 'ぷ', 'ぺ', 'ぽ'],
      // M-row
      ['ま', 'み', 'む', 'め', 'も'],
      // Y-row (with spacing)
      ['や', null, 'ゆ', null, 'よ'] as (string | null)[],
      // R-row
      ['ら', 'り', 'る', 'れ', 'ろ'],
      // W-row and N (with spacing)
      ['わ', null, 'を', null, 'ん'] as (string | null)[],
      // Small characters
      ['ぁ', 'ぃ', 'ぅ', 'ぇ', 'ぉ', 'ゃ', 'ゅ', 'ょ', 'っ'],
    ],
    full: [
      // Katakana - Vowels row
      ['ア', 'イ', 'ウ', 'エ', 'オ'],
      // K-row
      ['カ', 'キ', 'ク', 'ケ', 'コ'],
      // G-row (dakuten)
      ['ガ', 'ギ', 'グ', 'ゲ', 'ゴ'],
      // S-row
      ['サ', 'シ', 'ス', 'セ', 'ソ'],
      // Z-row (dakuten)
      ['ザ', 'ジ', 'ズ', 'ゼ', 'ゾ'],
      // T-row
      ['タ', 'チ', 'ツ', 'テ', 'ト'],
      // D-row (dakuten)
      ['ダ', 'ヂ', 'ヅ', 'デ', 'ド'],
      // N-row
      ['ナ', 'ニ', 'ヌ', 'ネ', 'ノ'],
      // H-row
      ['ハ', 'ヒ', 'フ', 'ヘ', 'ホ'],
      // B-row (dakuten)
      ['バ', 'ビ', 'ブ', 'ベ', 'ボ'],
      // P-row (handakuten)
      ['パ', 'ピ', 'プ', 'ペ', 'ポ'],
      // M-row
      ['マ', 'ミ', 'ム', 'メ', 'モ'],
      // Y-row (with spacing)
      ['ヤ', null, 'ユ', null, 'ヨ'] as (string | null)[],
      // R-row
      ['ラ', 'リ', 'ル', 'レ', 'ロ'],
      // W-row and N (with spacing)
      ['ワ', null, 'ヲ', null, 'ン'] as (string | null)[],
      // Small characters
      ['ァ', 'ィ', 'ゥ', 'ェ', 'ォ', 'ャ', 'ュ', 'ョ', 'ッ'],
    ],
  },
};

/**
 * Get keyboard layout for a specific language code
 * @param languageCode ISO 639-1 language code or BCP 47 (e.g., 'es', 'fr', 'es-ES')
 * @returns KeyboardLayout object or null if not found
 */
export function getKeyboardLayout(languageCode: string): KeyboardLayout | null {
  // Extract base language code (e.g., 'es' from 'es-ES')
  const baseCode = languageCode.toLowerCase().split('-')[0];
  const layout = keyboardLayouts[baseCode];
  return layout || null;
}

/**
 * Check if a language has a keyboard layout available
 * @param languageCode ISO 639-1 language code or BCP 47 (e.g., 'es' or 'es-ES')
 * @returns true if layout exists, false otherwise
 */
export function hasKeyboardLayout(languageCode: string): boolean {
  // Extract base language code (e.g., 'es' from 'es-ES')
  const baseCode = languageCode.toLowerCase().split('-')[0];
  return baseCode in keyboardLayouts;
}
