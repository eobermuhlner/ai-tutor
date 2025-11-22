interface ChatBackdropProps {
  isVisible: boolean;
  onClick: () => void;
}

export default function ChatBackdrop({ isVisible, onClick }: ChatBackdropProps) {
  if (!isVisible) return null;

  return (
    <div
      className="absolute inset-0 bg-black/50 transition-opacity duration-200 md:hidden z-40"
      onClick={onClick}
      aria-hidden="true"
    />
  );
}
