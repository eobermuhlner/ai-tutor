import type { Tutor } from '../types';
import { t, getCurrentLocale } from './translations';

export type TutorRole = 'reactive' | 'guided' | 'directive';

export function getRoleKey(tutor: Tutor): TutorRole {
  // Map teaching style directly to role
  return tutor.teachingStyle.toLowerCase() as TutorRole;
}

export function getRoleIcon(tutor: Tutor): string {
  const role = getRoleKey(tutor);
  const icons: Record<TutorRole, string> = {
    'reactive': '💬',
    'guided': '🎯',
    'directive': '📚',
  };
  return icons[role];
}

export function getRoleLabel(tutor: Tutor, locale?: string): string {
  const role = getRoleKey(tutor);
  return t(`tutor.role.${role}`, locale || getCurrentLocale());
}

export function getRoleDescription(tutor: Tutor, locale?: string): string {
  const role = getRoleKey(tutor);
  return t(`tutor.role.description.${role}`, locale || getCurrentLocale());
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
export function getRoleColorClass(_tutor: Tutor): string {
  return 'bg-slate-100 text-slate-700';
}
