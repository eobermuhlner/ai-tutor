import { useState, useEffect } from 'react';
import { getTutorImage } from '../../api/catalog';

interface TutorImageProps {
  tutorId?: string; // Optional when using previewImageUrl
  tutorEmoji: string;
  tutorName: string;
  size: 'small' | 'medium' | 'large'; // small: 8x8, medium: 10x10, large: 16x16
  rounded?: 'full' | 'lg'; // full for circular, lg for rounded corners
  className?: string;
  disableExpand?: boolean; // Whether to disable expand functionality
  previewImageUrl?: string | null; // Direct image URL for preview mode (skips fetching)
}

const sizeClasses = {
  small: 'w-8 h-8',
  medium: 'w-10 h-10',
  large: 'w-16 h-16',
};

const roundedClasses = {
  full: 'rounded-full',
  lg: 'rounded-lg',
};

export default function TutorImage({
  tutorId,
  tutorEmoji,
  tutorName,
  size,
  rounded = 'full',
  className = '',
  disableExpand = false,
  previewImageUrl,
}: TutorImageProps) {
  const [imageDataUrl, setImageDataUrl] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [showEmoji, setShowEmoji] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [isHovered, setIsHovered] = useState(false);

  useEffect(() => {
    // If previewImageUrl is provided, use it directly (preview mode)
    if (previewImageUrl !== undefined) {
      setIsLoading(false);
      if (previewImageUrl) {
        setImageDataUrl(previewImageUrl);
        setShowEmoji(false);
        console.log('🎨 TutorImage: Using preview image URL');
      } else {
        setShowEmoji(true);
        console.log('🎨 TutorImage: No preview image, using emoji fallback');
      }
      return;
    }

    // Otherwise, fetch image by tutorId (normal mode)
    if (!tutorId) {
      setIsLoading(false);
      setShowEmoji(true);
      return;
    }

    const loadImage = async () => {
      console.log('🎨 TutorImage: Loading image', { tutorId, size });
      setIsLoading(true);
      const dataUrl = await getTutorImage(tutorId);
      if (dataUrl) {
        setImageDataUrl(dataUrl);
        setShowEmoji(false);
        console.log('🎨 TutorImage: Image loaded successfully');
      } else {
        setShowEmoji(true);
        console.log('🎨 TutorImage: Using emoji fallback');
      }
      setIsLoading(false);
    };

    loadImage();
  }, [tutorId, previewImageUrl, size]);

  const sizeClass = sizeClasses[size];
  const roundedClass = roundedClasses[rounded];

  if (isLoading) {
    return (
      <div 
        className={`relative inline-block ${disableExpand ? 'cursor-default' : 'cursor-zoom-in'} ${isExpanded ? 'cursor-zoom-out' : 'cursor-zoom-in'}`}
        onClick={() => !disableExpand && setIsExpanded(!isExpanded)}
        onMouseEnter={() => !disableExpand && setIsHovered(true)}
        onMouseLeave={() => !disableExpand && setIsHovered(false)}
      >
        <div className={`${isExpanded ? 'w-48 h-48' : sizeClass} transition-all duration-300 ease-in-out`}>
          <div className={`${roundedClass} bg-gray-200 w-full h-full flex items-center justify-center ${className}`}>
            <span className="text-xs text-gray-400">...</span>
          </div>
        </div>
        {!disableExpand && (
          <button
            onClick={(e) => {
              e.stopPropagation();
              setIsExpanded(!isExpanded);
            }}
            className={`absolute top-1 right-1 bg-white/80 hover:bg-white rounded-full p-1 shadow-sm transition-all duration-200 ${
              isExpanded || isHovered ? 'opacity-100 visible' : 'opacity-0 invisible'
            }`}
            aria-label={isExpanded ? "Shrink image" : "Expand image"}
          >
            <svg className="w-3 h-3 text-slate-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              {isExpanded ? (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 9V4.5M9 9H4.5M9 9L3.75 3.75M9 15v4.5M9 15H4.5M9 15l-5.25 5.25M15 9h4.5M15 9V4.5M15 9l5.25-5.25M15 15h4.5M15 15v4.5m0-4.5l5.25 5.25" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.75 3.75v4.5m0-4.5h4.5m-4.5 0L9 9M3.75 20.25v-4.5m0 4.5h4.5m-4.5 0L9 15M20.25 3.75h-4.5m4.5 0v4.5m0-4.5L15 9m5.25 11.25h-4.5m4.5 0v-4.5m0 4.5L15 15" />
              )}
            </svg>
          </button>
        )}
      </div>
    );
  }

  if (showEmoji) {
    return (
      <div 
        className={`relative inline-block ${disableExpand ? 'cursor-default' : 'cursor-zoom-in'} ${isExpanded ? 'cursor-zoom-out' : 'cursor-zoom-in'}`}
        onClick={() => !disableExpand && setIsExpanded(!isExpanded)}
        onMouseEnter={() => !disableExpand && setIsHovered(true)}
        onMouseLeave={() => !disableExpand && setIsHovered(false)}
        aria-hidden="true"
      >
        <div className={`${isExpanded ? 'w-48 h-48' : sizeClass} transition-all duration-300 ease-in-out`}>
          <div className={`${roundedClass} bg-gradient-to-br from-slate-400 to-slate-500 w-full h-full flex items-center justify-center text-white ${className}`}>
            <span className={size === 'small' ? 'text-xs' : size === 'medium' ? 'text-sm' : 'text-2xl'}>
              {tutorEmoji}
            </span>
          </div>
        </div>
        {!disableExpand && (
          <button
            onClick={(e) => {
              e.stopPropagation();
              setIsExpanded(!isExpanded);
            }}
            className={`absolute top-1 right-1 bg-white/80 hover:bg-white rounded-full p-1 shadow-sm transition-all duration-200 ${
              isExpanded || isHovered ? 'opacity-100 visible' : 'opacity-0 invisible'
            }`}
            aria-label={isExpanded ? "Shrink image" : "Expand image"}
          >
            <svg className="w-3 h-3 text-slate-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              {isExpanded ? (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 9V4.5M9 9H4.5M9 9L3.75 3.75M9 15v4.5M9 15H4.5M9 15l-5.25 5.25M15 9h4.5M15 9V4.5M15 9l5.25-5.25M15 15h4.5M15 15v4.5m0-4.5l5.25 5.25" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.75 3.75v4.5m0-4.5h4.5m-4.5 0L9 9M3.75 20.25v-4.5m0 4.5h4.5m-4.5 0L9 15M20.25 3.75h-4.5m4.5 0v4.5m0-4.5L15 9m5.25 11.25h-4.5m4.5 0v-4.5m0 4.5L15 15" />
              )}
            </svg>
          </button>
        )}
      </div>
    );
  }

  return (
    <div 
      className={`relative inline-block ${isExpanded ? 'cursor-zoom-out' : 'cursor-zoom-in'}`}
      onMouseEnter={() => !disableExpand && setIsHovered(true)}
      onMouseLeave={() => !disableExpand && setIsHovered(false)}
    >
      <div className={`${isExpanded ? 'w-48' : sizeClass} transition-all duration-300 ease-in-out`}>
        <img
          src={imageDataUrl || undefined}
          alt={tutorName}
          className={`w-full h-full object-cover shadow-md ${className} ${roundedClass}`}
          onClick={() => !disableExpand && setIsExpanded(!isExpanded)}
          onError={() => {
            console.log('❌ TutorImage: Image failed to display');
            setShowEmoji(true);
          }}
        />
      </div>
      {!disableExpand && (
        <button
          onClick={(e) => {
            e.stopPropagation();
            setIsExpanded(!isExpanded);
          }}
          className={`absolute top-1 right-1 bg-white/80 hover:bg-white rounded-full p-1 shadow-sm transition-all duration-200 ${
            isExpanded || isHovered ? 'opacity-100 visible' : 'opacity-0 invisible'
          }`}
          aria-label={isExpanded ? "Shrink image" : "Expand image"}
        >
          <svg className="w-3 h-3 text-slate-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            {isExpanded ? (
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 9V4.5M9 9H4.5M9 9L3.75 3.75M9 15v4.5M9 15H4.5M9 15l-5.25 5.25M15 9h4.5M15 9V4.5M15 9l5.25-5.25M15 15h4.5M15 15v4.5m0-4.5l5.25 5.25" />
            ) : (
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3.75 3.75v4.5m0-4.5h4.5m-4.5 0L9 9M3.75 20.25v-4.5m0 4.5h4.5m-4.5 0L9 15M20.25 3.75h-4.5m4.5 0v4.5m0-4.5L15 9m5.25 11.25h-4.5m4.5 0v-4.5m0 4.5L15 15" />
            )}
          </svg>
        </button>
      )}
    </div>
  );
}
