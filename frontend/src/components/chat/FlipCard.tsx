import { useState, useEffect } from 'react';
import { Volume2, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import type { WordCard } from '../../types';
import { useTTS } from '../../contexts/TTSContext';

interface FlipCardProps {
  card: WordCard;
}

export default function FlipCard({ card }: FlipCardProps) {
  const [isFlipped, setIsFlipped] = useState(false);
  const [imageError, setImageError] = useState(false);
  const [isImageExpanded, setIsImageExpanded] = useState(false);
  const [isPlayingAudio, setIsPlayingAudio] = useState(false);
  const { available: ttsAvailable, playText, voices } = useTTS();

  // Reset image error when card changes
  useEffect(() => {
    setImageError(false);
  }, [card.imageUrl]);

  const handlePlayPronunciation = async (e: React.MouseEvent) => {
    e.stopPropagation(); // Prevent card flip

    if (!ttsAvailable || !voices) {
      return;
    }

    setIsPlayingAudio(true);
    try {
      // Use a friendly voice for vocabulary with slower speed for learning
      const voiceId = 'Warm';
      await playText(card.titleTargetLanguage, voiceId, 0.85);
    } catch (error) {
      console.error('Failed to play pronunciation:', error);
      toast.error('Could not play pronunciation');
    } finally {
      setIsPlayingAudio(false);
    }
  };

  return (
    <div className="w-full min-w-[280px] cursor-pointer" onClick={() => setIsFlipped(!isFlipped)}>
      <div className="relative w-full">
        {/* Front side - Target Language */}
        <div className={`rounded-xl border border-slate-200 bg-gradient-to-br from-blue-50 to-indigo-50 p-4 shadow-soft transition-all duration-500 ${
          isFlipped ? 'hidden' : 'block'
        }`}>
          <div className="flex flex-col">
            {/* Image - 896x1152 aspect ratio (7:9) */}
            {card.imageUrl && !imageError && (
              <div className="mb-3 flex-shrink-0 relative">
                <div className={`aspect-[7/9] ${isImageExpanded ? 'w-full' : 'w-32'} transition-all duration-300`}>
                  <img
                    src={card.imageUrl}
                    alt={card.titleTargetLanguage}
                    className="w-full h-full object-cover rounded-lg"
                    loading="lazy"
                    onError={() => setImageError(true)}
                  />
                </div>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setIsImageExpanded(!isImageExpanded);
                  }}
                  className="absolute top-2 right-2 bg-white/80 hover:bg-white rounded-full p-1.5 shadow-sm transition-colors"
                  aria-label={isImageExpanded ? "Shrink image" : "Expand image"}
                >
                  <svg className="w-4 h-4 text-slate-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    {isImageExpanded ? (
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 9V4.5M9 9H4.5M9 9L3.75 3.75M9 15v4.5M9 15H4.5M9 15l-5.25 5.25M15 9h4.5M15 9V4.5M15 9l5.25-5.25M15 15h4.5M15 15v4.5m0-4.5l5.25 5.25" />
                    ) : (
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.75 3.75v4.5m0-4.5h4.5m-4.5 0L9 9M3.75 20.25v-4.5m0 4.5h4.5m-4.5 0L9 15M20.25 3.75h-4.5m4.5 0v4.5m0-4.5L15 9m5.25 11.25h-4.5m4.5 0v-4.5m0 4.5L15 15" />
                    )}
                  </svg>
                </button>
              </div>
            )}
            <div className="flex items-center justify-between gap-2 mb-2">
              <h3 className="text-xl font-bold text-slate-900 flex-1">
                {card.titleTargetLanguage}
              </h3>
              {ttsAvailable && (
                <button
                  onClick={handlePlayPronunciation}
                  disabled={isPlayingAudio}
                  className="flex-shrink-0 p-1.5 text-slate-600 hover:text-slate-900 hover:bg-white/50 rounded-md transition-colors disabled:opacity-50"
                  aria-label="Hear pronunciation"
                >
                  {isPlayingAudio ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <Volume2 className="w-4 h-4" />
                  )}
                </button>
              )}
            </div>
            <p className="text-sm text-slate-700 flex-1 overflow-y-auto">
              {card.descriptionTargetLanguage}
            </p>
            <div className="mt-2 text-xs text-slate-500 text-center">
              Click to flip
            </div>
          </div>
        </div>

        {/* Back side - Source Language */}
        <div className={`rounded-xl border border-slate-200 bg-gradient-to-br from-slate-50 to-gray-50 p-4 shadow-soft transition-all duration-500 ${
          isFlipped ? 'block' : 'hidden'
        }`}>
          <div className="flex flex-col">
            {/* Image - 896x1152 aspect ratio (7:9) */}
            {card.imageUrl && !imageError && (
              <div className="mb-3 flex-shrink-0 relative">
                <div className={`aspect-[7/9] ${isImageExpanded ? 'w-full' : 'w-32'} transition-all duration-300`}>
                  <img
                    src={card.imageUrl}
                    alt={card.titleSourceLanguage}
                    className="w-full h-full object-cover rounded-lg"
                    loading="lazy"
                    onError={() => setImageError(true)}
                  />
                </div>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setIsImageExpanded(!isImageExpanded);
                  }}
                  className="absolute top-2 right-2 bg-white/80 hover:bg-white rounded-full p-1.5 shadow-sm transition-colors"
                  aria-label={isImageExpanded ? "Shrink image" : "Expand image"}
                >
                  <svg className="w-4 h-4 text-slate-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    {isImageExpanded ? (
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 9V4.5M9 9H4.5M9 9L3.75 3.75M9 15v4.5M9 15H4.5M9 15l-5.25 5.25M15 9h4.5M15 9V4.5M15 9l5.25-5.25M15 15h4.5M15 15v4.5m0-4.5l5.25 5.25" />
                    ) : (
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.75 3.75v4.5m0-4.5h4.5m-4.5 0L9 9M3.75 20.25v-4.5m0 4.5h4.5m-4.5 0L9 15M20.25 3.75h-4.5m4.5 0v4.5m0-4.5L15 9m5.25 11.25h-4.5m4.5 0v-4.5m0 4.5L15 15" />
                    )}
                  </svg>
                </button>
              </div>
            )}
            <h3 className="text-xl font-bold text-slate-900 mb-2">
              {card.titleSourceLanguage}
            </h3>
            <p className="text-sm text-slate-700 flex-1 overflow-y-auto">
              {card.descriptionSourceLanguage}
            </p>
            <div className="mt-2 text-xs text-slate-500 text-center">
              Click to flip back
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
