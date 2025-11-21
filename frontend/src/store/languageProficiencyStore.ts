import { create } from 'zustand';

interface LanguageProficiencyState {
  refreshTrigger: number;
  triggerRefresh: () => void;
}

export const useLanguageProficiencyStore = create<LanguageProficiencyState>((set) => ({
  refreshTrigger: 0,
  triggerRefresh: () => set((state) => ({ refreshTrigger: state.refreshTrigger + 1 })),
}));