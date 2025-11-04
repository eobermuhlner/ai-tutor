/**
 * Chat-related constants
 */

// Timing constants
export const AUTO_PLAY_DELAY = 300; // ms - delay before auto-playing audio to ensure message is rendered
export const WELCOME_MESSAGE_DELAY = 10; // ms - delay before initiating welcome message
export const SSE_TIMEOUT = 10000; // ms - timeout for SSE (Server-Sent Events) connections

// Phase display names
export const PHASE_NAMES = {
  AUTO: 'Adaptive mode',
  FREE: 'Free conversation',
  CORRECTION: 'Correction mode',
  DRILL: 'Practice mode',
} as const;

// Teaching style display names
export const TEACHING_STYLE_NAMES = {
  Reactive: 'Reactive style',
  Guided: 'Guided style',
  Directive: 'Directive style',
} as const;

// TTS speed options
export const TTS_SPEED_OPTIONS = [0.75, 1.0, 1.25] as const;

// Re-engagement threshold (days)
export const REENGAGE_THRESHOLD_DAYS = 7;
