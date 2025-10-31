// Simple translation system for tutor roles
// TODO: Integrate with proper i18n library (react-i18next, react-intl, etc.)

const translations: Record<string, Record<string, string>> = {
  'en': {
    'tutor.role.reactive': 'Reactive',
    'tutor.role.guided': 'Guided',
    'tutor.role.directive': 'Directive',
    'tutor.role.description.reactive': 'Follows your pace, responds naturally to your questions',
    'tutor.role.description.guided': 'Guides you with questions and suggests practice activities',
    'tutor.role.description.directive': 'Provides structured lessons and systematic explanations',
  },
  'es': {
    'tutor.role.reactive': 'Reactivo',
    'tutor.role.guided': 'Guiado',
    'tutor.role.directive': 'Directivo',
    'tutor.role.description.reactive': 'Sigue tu ritmo, responde naturalmente a tus preguntas',
    'tutor.role.description.guided': 'Te guía con preguntas y sugiere actividades de práctica',
    'tutor.role.description.directive': 'Proporciona lecciones estructuradas y explicaciones sistemáticas',
  },
  'de': {
    'tutor.role.reactive': 'Reaktiv',
    'tutor.role.guided': 'Geführt',
    'tutor.role.directive': 'Direktiv',
    'tutor.role.description.reactive': 'Folgt deinem Tempo, antwortet natürlich auf deine Fragen',
    'tutor.role.description.guided': 'Leitet dich mit Fragen und schlägt Übungsaktivitäten vor',
    'tutor.role.description.directive': 'Bietet strukturierte Lektionen und systematische Erklärungen',
  },
  'fr': {
    'tutor.role.reactive': 'Réactif',
    'tutor.role.guided': 'Guidé',
    'tutor.role.directive': 'Directif',
    'tutor.role.description.reactive': 'Suit votre rythme, répond naturellement à vos questions',
    'tutor.role.description.guided': 'Vous guide avec des questions et suggère des activités de pratique',
    'tutor.role.description.directive': 'Fournit des leçons structurées et des explications systématiques',
  },
};

export function t(key: string, locale: string = 'en'): string {
  return translations[locale]?.[key] || translations['en']?.[key] || key;
}

export function getCurrentLocale(): string {
  // TODO: Get from proper i18n context or browser settings
  return navigator.language.split('-')[0] || 'en';
}
