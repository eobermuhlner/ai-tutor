import { useState } from 'react';
import type { CharacterCard as CharacterCardType } from '../../types';

interface CharacterCardProps {
  card: CharacterCardType;
}

export default function CharacterCard({ card }: CharacterCardProps) {
  const [isFlipped, setIsFlipped] = useState(false);

  return (
    <div
      className="w-full min-h-[280px] cursor-pointer perspective-1000"
      onClick={() => setIsFlipped(!isFlipped)}
      onKeyPress={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          setIsFlipped(!isFlipped);
        }
      }}
      role="button"
      tabIndex={0}
      aria-label={`Character card: ${card.character}`}
    >
      <div className={`flip-card-inner ${isFlipped ? 'flipped' : ''}`}>

        {/* FRONT - Character Display */}
        <div className="flip-card-front rounded-xl border border-slate-200 bg-white p-6 shadow-soft flex flex-col">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4">
            FRONT
          </div>
          <div className="flex-1 flex items-center justify-center">
            <div
              className="text-7xl md:text-8xl font-medium text-slate-900"
              style={{
                fontFamily: '"Noto Sans JP", "Noto Sans KR", "Noto Sans SC", "Noto Sans", "Hiragino Sans", "Malgun Gothic", "Microsoft YaHei", sans-serif',
                lineHeight: 1.1
              }}
            >
              {card.character}
            </div>
          </div>
          <div className="text-xs text-slate-400 text-center mt-4">
            Tap to reveal
          </div>
        </div>

        {/* BACK - Pronunciation & Description */}
        <div className="flip-card-back rounded-xl border border-slate-200 bg-gradient-to-br from-slate-50 to-gray-50 p-6 shadow-soft flex flex-col">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-4">
            BACK
          </div>
          <div className="flex-1 flex flex-col gap-4">
            <div>
              <span className="text-sm font-normal text-slate-600">Pronunciation: </span>
              <span className="text-lg font-semibold text-slate-900">{card.pronunciation}</span>
            </div>
            <div className="text-sm text-slate-700 leading-relaxed max-w-md">
              {card.description}
            </div>
          </div>
          <div className="text-xs text-slate-400 text-center mt-4">
            Tap to flip back
          </div>
        </div>
      </div>
    </div>
  );
}
