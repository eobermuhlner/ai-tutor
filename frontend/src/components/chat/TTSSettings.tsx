import { useTTS } from '../../contexts/TTSContext';

export default function TTSSettings() {
  const { available, loading, preferences, updatePreferences } = useTTS();

  const handleToggle = (key: keyof typeof preferences, value: boolean) => {
    updatePreferences({ [key]: value });
  };

  const handleSpeedChange = (speed: number) => {
    updatePreferences({ defaultSpeed: speed });
  };

  if (loading) {
    return (
      <div className="text-sm text-slate-500">
        Loading audio settings...
      </div>
    );
  }

  if (!available) {
    return (
      <div className="text-sm text-slate-500">
        Text-to-speech is not available with the current language provider.
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h4 className="text-sm font-semibold text-slate-900">Audio Settings</h4>

      {/* Enable/Disable TTS */}
      <div className="flex items-center justify-between">
        <div className="flex-1">
          <label className="text-sm text-slate-700">Enable audio playback</label>
          <p className="text-xs text-slate-500 mt-0.5">
            Allow playing tutor messages and vocabulary
          </p>
        </div>
        <label className="relative inline-flex items-center cursor-pointer ml-3">
          <input
            type="checkbox"
            checked={preferences.enabled}
            onChange={(e) => handleToggle('enabled', e.target.checked)}
            className="sr-only peer"
          />
          <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-brand-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-brand-600"></div>
        </label>
      </div>

      {preferences.enabled && (
        <>
          {/* Auto-play */}
          <div className="flex items-center justify-between pt-3 border-t border-slate-200">
            <div className="flex-1">
              <label className="text-sm text-slate-700">Auto-play tutor messages</label>
              <p className="text-xs text-slate-500 mt-0.5">
                Automatically play audio when tutor responds
              </p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer ml-3">
              <input
                type="checkbox"
                checked={preferences.autoPlay}
                onChange={(e) => handleToggle('autoPlay', e.target.checked)}
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-brand-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-brand-600"></div>
            </label>
          </div>

          {/* Default Speed */}
          <div className="pt-3 border-t border-slate-200">
            <label className="block text-sm text-slate-700 mb-2">
              Default playback speed
            </label>
            <div className="flex items-center gap-2">
              {[0.75, 1.0, 1.25].map((speed) => (
                <button
                  key={speed}
                  onClick={() => handleSpeedChange(speed)}
                  className={`flex-1 px-3 py-2 text-sm font-medium rounded-md transition-colors ${
                    preferences.defaultSpeed === speed
                      ? 'bg-brand-600 text-white'
                      : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                  }`}
                >
                  {speed}x
                </button>
              ))}
            </div>
            <p className="text-xs text-slate-500 mt-2">
              {preferences.defaultSpeed === 0.75
                ? 'Slower - Better for learning new words'
                : preferences.defaultSpeed === 1.0
                  ? 'Normal - Natural speaking pace'
                  : 'Faster - Quick review'}
            </p>
          </div>

          {/* Wi-Fi Only */}
          <div className="flex items-center justify-between pt-3 border-t border-slate-200">
            <div className="flex-1">
              <label className="text-sm text-slate-700">Wi-Fi only</label>
              <p className="text-xs text-slate-500 mt-0.5">
                Only play audio when connected to Wi-Fi
              </p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer ml-3">
              <input
                type="checkbox"
                checked={preferences.wifiOnly}
                onChange={(e) => handleToggle('wifiOnly', e.target.checked)}
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-slate-200 peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-brand-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-brand-600"></div>
            </label>
          </div>
        </>
      )}
    </div>
  );
}
