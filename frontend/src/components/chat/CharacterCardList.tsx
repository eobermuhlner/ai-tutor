import type { CharacterCard } from '../../types';
import CharacterCardComponent from './CharacterCard';

interface CharacterCardListProps {
  characterCards: CharacterCard[];
}

export default function CharacterCardList({ characterCards }: CharacterCardListProps) {
  if (!characterCards || characterCards.length === 0) {
    return null;
  }

  return (
    <div className="w-full mt-4">
      <div className="flex items-center gap-2 mb-3">
        <span className="text-lg" role="img" aria-label="Character cards">
          🔤
        </span>
        <h3 className="text-base font-semibold text-slate-700">
          Character Cards
        </h3>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {characterCards.map((card, index) => (
          <CharacterCardComponent key={index} card={card} />
        ))}
      </div>
    </div>
  );
}
