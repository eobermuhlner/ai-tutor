import type { WordCard } from '../../types';
import FlipCard from './FlipCard';

interface WordCardListProps {
  wordCards: WordCard[];
  sessionId?: string;
}

export default function WordCardList({ wordCards }: WordCardListProps) {
  if (!wordCards || wordCards.length === 0) {
    return null;
  }

  return (
    <div className="w-full">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {wordCards.map((card, index) => (
          <FlipCard key={index} card={card} />
        ))}
      </div>
    </div>
  );
}
