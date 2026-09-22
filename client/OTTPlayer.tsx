/**
 * OTT Platform HLS Player Component
 * Hardware-accelerated HLS playback with proxy support
 * Zero local file downloads - streams directly through backend proxy
 */

import { useState, useEffect, useRef, useCallback } from 'react';

interface PlayerProps {
  /** Episode ID from backend */
  episodeId: string;
  /** Auto-selected link ID from smart routing */
  linkId?: string;
  /** Backend proxy API base URL */
  apiUrl?: string;
  /** Title for display */
  title?: string;
  /** Auto-play on mount */
  autoPlay?: boolean;
  /** Muted by default */
  muted?: boolean;
  /** Callback when playback ends */
  onEnded?: () => void;
  /** Callback on error */
  onError?: (error: Error) => void;
  /** Callback when quality changes */
  onQualityChange?: (quality: string) => void;
  /** Custom poster image */
  poster?: string;
  /** Player theme */
  theme?: 'dark' | 'light';
}

interface StreamLink {
  success: boolean;
  link: {
    id: string;
    link_id: string;
    telegram_file_path: string;
    quality: string;
    language: string;
    max_concurrent_users: number;
    current_users: number;
  };
  message: string;
}

export default function OTTPlayer({
  episodeId,
  linkId: initialLinkId,
  apiUrl = 'http://141.147.83.191:3000',
  title = 'OTT Stream',
  autoPlay = true,
  muted = false,
  onEnded,
  onError,
  onQualityChange,
  poster,
  theme = 'dark'
}: PlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<any>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isBuffering, setIsBuffering] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentQuality, setCurrentQuality] = useState<string>('auto');
  const [availableQualities, setAvailableQualities] = useState<string[]>(['auto']);
  const [showQualityMenu, setShowQualityMenu] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [volume, setVolume] = useState(1);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [selectedLinkId, setSelectedLinkId] = useState<string | undefined>(initialLinkId);
  const controlsTimeoutRef = useRef<NodeJS.Timeout>();

  // Load HLS.js dynamically
  useEffect(() => {
    const loadHls = async () => {
      try {
        const Hls = (await import('hls.js')).default;
        if (Hls.isSupported()) {
          const hls = new Hls({
            enableWorker: true,
            lowLatencyMode: true,
            startLevel: -1,
            capLevelToPlayerSize: true,
            maxBufferLength: 30,
            maxMaxBufferLength: 60,
            enableDateRange: true,
            // Hardware acceleration
            enableSoftwareAES: false,
            // Progressive loading
            progressive: true,
            // Buffer control
            maxBufferSize: 60 * 1000 * 1000,
            maxBufferHole: 0.5,
            highBufferWatchdogPeriod: 2,
            // Manifest parsing
            manifestLoadingTimeOut: 10000,
            manifestLoadingMaxRetry: 3,
            manifestLoadingRetryDelay: 1000,
            // Segment loading
            fragLoadingTimeOut: 20000,
            fragLoadingMaxRetry: 6,
            fragLoadingRetryDelay: 1000,
            fragLoadingLoopThreshold: 3,
            // ABR
            abrEwmaFastLive: 3.0,
            abrEwmaSlowLive: 9.0,
            abrEwmaFastVoD: 3.0,
            abrEwmaSlowVoD: 9.0,
            abrBandWidthFactor: 0.95,
            abrBandWidthUpFactor: 0.7,
            // Audio
            audioTrackSwitching: true,
          });

          hlsRef.current = hls;
          hls.attachMedia(videoRef.current!);

          hls.on(Hls.Events.MANIFEST_PARSED, (event: any, data: any) => {
            // Extract available qualities
            const levels = data.levels.map((level: any) => {
              const height = level.height;
              if (height >= 2160) return '4K';
              if (height >= 1440) return '1440p';
              if (height >= 1080) return '1080p';
              if (height >= 720) return '720p';
              if (height >= 480) return '480p';
              if (height >= 360) return '360p';
              return `${height}p`;
            });
            setAvailableQualities(['auto', ...new Set(levels)]);
            setIsBuffering(false);

            if (autoPlay) {
              videoRef.current?.play().catch(() => {
                setIsPlaying(false);
              });
            }
          });

          hls.on(Hls.Events.LEVEL_SWITCHED, (event: any, data: any) => {
            const level = hls.levels[data.level];
            if (level) {
              const quality = level.height >= 2160 ? '4K' :
                            level.height >= 1440 ? '1440p' :
                            level.height >= 1080 ? '1080p' :
                            level.height >= 720 ? '720p' :
                            level.height >= 480 ? '480p' :
                            level.height >= 360 ? '360p' : `${level.height}p`;
              setCurrentQuality(quality);
              onQualityChange?.(quality);
            }
          });

          hls.on(Hls.Events.ERROR, (event: any, data: any) => {
            if (data.fatal) {
              switch (data.type) {
                case Hls.ErrorTypes.NETWORK_ERROR:
                  hls.startLoad();
                  break;
                case Hls.ErrorTypes.MEDIA_ERROR:
                  hls.recoverMediaError();
                  break;
                default:
                  hls.destroy();
                  setError('Playback error occurred');
                  onError?.(new Error('Fatal HLS error'));
              }
            }
          });

          hls.on(Hls.Events.BUFFER_STALLED_ERROR, () => {
            setIsBuffering(true);
          });

          hls.on(Hls.Events.BUFFER_APPENDED, () => {
            setIsBuffering(false);
          });
        } else if (videoRef.current?.canPlayType('application/vnd.apple.mpegurl')) {
          // Native HLS support (Safari)
          videoRef.current.src = `${apiUrl}/api/stream/playlist/${episodeId}/${selectedLinkId}`;
          videoRef.current.addEventListener('loadedmetadata', () => {
            setIsBuffering(false);
            if (autoPlay) {
              videoRef.current?.play().catch(() => setIsPlaying(false));
            }
          });
        }
      } catch (err) {
        console.error('Failed to load HLS:', err);
        setError('Failed to initialize player');
        onError?.(err as Error);
      }
    };

    if (videoRef.current) {
      loadHls();
    }

    return () => {
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
    };
  }, [episodeId, selectedLinkId, apiUrl, autoPlay]);

  // Fetch smart-routed link if not provided
  useEffect(() => {
    if (!initialLinkId) {
      fetch(`${apiUrl}/api/episodes/${episodeId}/links`)
        .then(res => res.json())
        .then((data: StreamLink) => {
          if (data.success && data.link) {
            setSelectedLinkId(data.link.id);
          } else {
            setError(data.error || 'No available links');
          }
        })
        .catch(() => setError('Failed to load stream'));
    }
  }, [episodeId, initialLinkId, apiUrl]);

  // Release link slot on unmount
  useEffect(() => {
    return () => {
      if (selectedLinkId) {
        fetch(`${apiUrl}/api/episodes/${episodeId}/release`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ linkId: selectedLinkId })
        }).catch(() => {});
      if (hlsRef.current) {
        hlsRef.current.destroy();
      }
    };
  }, []);

  // Video event handlers
  const handlePlay = useCallback(() => {
    videoRef.current?.play().then(() => setIsPlaying(true)).catch(() => setIsPlaying(false));
  }, []);

  const handlePause = useCallback(() => {
    videoRef.current?.pause();
    setIsPlaying(false);
  }, []);

  const handleTimeUpdate = useCallback(() => {
    if (videoRef.current) {
      setCurrentTime(videoRef.current.currentTime);
      setDuration(videoRef.current.duration);
    }
  }, []);

  const handleSeek = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const time = parseFloat(e.target.value);
    if (videoRef.current) {
      videoRef.current.currentTime = time;
      setCurrentTime(time);
    }
  }, []);

  const handleVolumeChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const vol = parseFloat(e.target.value);
    if (videoRef.current) {
      videoRef.current.volume = vol;
      videoRef.current.muted = vol === 0;
      setVolume(vol);
    }
  }, []);

  const handleMuteToggle = useCallback(() => {
    if (videoRef.current) {
      videoRef.current.muted = !videoRef.current.muted;
      setVolume(videoRef.current.muted ? 0 : volume);
    }
  }, [volume]);

  const handleFullscreenToggle = useCallback(() => {
    const player = videoRef.current?.parentElement;
    if (!player) return;

    if (!isFullscreen) {
      if (player.requestFullscreen) player.requestFullscreen();
      else if (player.webkitRequestFullscreen) player.webkitRequestFullscreen();
      setIsFullscreen(true);
    } else {
      if (document.exitFullscreen) document.exitFullscreen();
      else if (document.webkitExitFullscreen) document.webkitExitFullscreen();
      setIsFullscreen(false);
    }
  }, [isFullscreen]);

  const handleQualityChange = useCallback((quality: string) => {
    if (hlsRef.current && quality !== 'auto') {
      const levelIndex = hlsRef.current.levels.findIndex(
        (l: any) => l.height >= parseInt(quality.replace('p', ''))
      );
      if (levelIndex !== -1) {
        hlsRef.current.currentLevel = levelIndex;
        setCurrentQuality(quality);
      }
    } else if (quality === 'auto') {
      hlsRef.current.currentLevel = -1;
      setCurrentQuality('auto');
    }
  }, []);

  // Show/hide controls on mouse move
  useEffect(() => {
    const playerContainer = videoRef.current?.parentElement;
    if (!playerContainer) return;

    const show = () => {
      setShowControls(true);
      if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
      controlsTimeoutRef.current = setTimeout(() => setShowControls(false), 3000);
    };

    playerContainer.addEventListener('mousemove', show);
    playerContainer.addEventListener('click', show);
    show();

    return () => {
      playerContainer.removeEventListener('mousemove', show);
      playerContainer.removeEventListener('click', show);
      if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
    };
  }, []);

  const formatTime = (seconds: number) => {
    if (isNaN(seconds)) return '0:00';
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = Math.floor(seconds % 60);
    return hrs > 0
      ? `${hrs}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
      : `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  if (error) {
    return (
      <div className="relative w-full aspect-video bg-gray-900 rounded-xl flex items-center justify-center">
        <div className="text-center p-6">
          <svg className="w-16 h-16 mx-auto text-red-500 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <p className="text-lg font-medium text-white mb-2">Playback Error</p>
          <p className="text-gray-400 mb-4">{error}</p>
          <button
            onClick={() => { setError(null); window.location.reload(); }}
            className="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className={`relative w-full aspect-video bg-black rounded-xl overflow-hidden ${theme === 'light' ? 'bg-gray-100' : ''}`}>
      {/* Video Element */}
      <video
        ref={videoRef}
        className="w-full h-full object-contain"
        playsInline
        muted={muted}
        onPlay={() => setIsPlaying(true)}
        onPause={() => setIsPlaying(false)}
        onTimeUpdate={handleTimeUpdate}
        onEnded={() => {
          setIsPlaying(false);
          onEnded?.();
        }}
        onWaiting={() => setIsBuffering(true)}
        onCanPlay={() => setIsBuffering(false)}
        onError={(e) => {
          setError('Video playback failed');
          onError?.(new Error('Video error'));
        }}
        style={{
          backgroundColor: 'black',
          // Hardware acceleration hints
          transform: 'translateZ(0)',
          backfaceVisibility: 'hidden',
          willChange: 'transform'
        }}
      />

      {/* Buffering Overlay */}
      {isBuffering && (
        <div className="absolute inset-0 flex items-center justify-center bg-black/80 z-10">
          <div className="text-center">
            <div className="w-12 h-12 border-4 border-red-500 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
            <p className="text-white text-lg">Buffering...</p>
          </div>
        </div>
      )}

      {/* Controls Overlay */}
      <div
        className={`absolute inset-0 z-20 transition-opacity duration-300 ${
          showControls || isBuffering ? 'opacity-100' : 'opacity-0 pointer-events-none'
        }`}
      >
        {/* Top Bar */}
        <div className="absolute top-0 left-0 right-0 h-16 bg-gradient-to-b from-black/80 to-transparent flex items-center justify-between px-4">
          <div className="flex items-center gap-3">
            <button
              onClick={() => window.history.back()}
              className="p-2 bg-white/10 rounded-lg hover:bg-white/20 transition-colors"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h17.8" />
              </svg>
            </button>
            <div>
              <p className="font-semibold text-white truncate max-w-xs">{title}</p>
              <p className="text-xs text-gray-400">Live Streaming</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {availableQualities.length > 1 && (
              <div className="relative">
                <button
                  onClick={() => setShowQualityMenu(!showQualityMenu)}
                  className="flex items-center gap-1.5 px-3 py-1.5 bg-white/10 rounded-lg hover:bg-white/20 transition-colors"
                >
                  <span className="text-sm font-medium">{currentQuality.toUpperCase()}</span>
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                  </svg>
                </button>
                {showQualityMenu && (
                  <div className="absolute bottom-full left-0 mb-2 bg-gray-900 border border-gray-700 rounded-lg py-1 min-w-[120px] animate-fade-in">
                    {availableQualities.map(q => (
                      <button
                        key={q}
                        onClick={() => { handleQualityChange(q); setShowQualityMenu(false); }}
                        className={`w-full px-4 py-2 text-left text-sm hover:bg-gray-800 transition-colors ${
                          currentQuality === q ? 'text-red-400' : 'text-gray-300'
                        }`}
                      >
                        {q.toUpperCase()}{q !== 'auto' && 'p'}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}
            <button
              onClick={handleFullscreenToggle}
              className="p-2 bg-white/10 rounded-lg hover:bg-white/20 transition-colors"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                {isFullscreen ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8V4m0 0h4M4 4h5m8 12V20m0 0h-4M20 20h-5" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8V4m0 0h4M4 4h5m8 12V20m0 0h-4M20 20h-5" />
                )}
              </svg>
            </button>
          </div>
        </div>

        {/* Bottom Controls */}
        <div className="absolute bottom-0 left-0 right-0 h-20 bg-gradient-to-t from-black/90 to-transparent flex items-center justify-between px-4">
          {/* Play/Pause + Progress */}
          <div className="flex items-center gap-4 flex-1 max-w-2xl mx-auto">
            <button
              onClick={isPlaying ? handlePause : handlePlay}
              className="p-3 bg-white/10 rounded-full hover:bg-white/20 transition-colors"
              aria-label={isPlaying ? 'Pause' : 'Play'}
            >
              {isPlaying ? (
                <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" />
                </svg>
              ) : (
                <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M8 5v14l11-7z" />
                </svg>
              )}
            </button>

            <div className="flex-1 flex items-center gap-3">
              <span className="text-xs text-gray-400 font-mono w-12 text-right">{formatTime(currentTime)}</span>
              <input
                type="range"
                min={0}
                max={duration || 100}
                value={currentTime}
                onChange={handleSeek}
                className="flex-1 h-1.5 bg-gray-700 rounded-full appearance-none cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-3 [&::-webkit-slider-thumb]:h-3 [&::-webkit-slider-thumb]:bg-red-500 [&::-webkit-slider-thumb]:rounded-full [&::-moz-range-thumb]:w-3 [&::-moz-range-thumb]:h-3 [&::-moz-range-thumb]:bg-red-500 [&::-moz-range-thumb]:rounded-full"
                style={{ background: `linear-gradient(to right, red ${(currentTime / (duration || 1)) * 100}%, gray ${(currentTime / (duration || 1)) * 100}%)` }}
              />
              <span className="text-xs text-gray-400 font-mono w-12">{formatTime(duration)}</span>
            </div>
          </div>

          {/* Volume & Settings */}
          <div className="flex items-center gap-3">
            <button
              onClick={handleMuteToggle}
              className="p-2 bg-white/10 rounded-lg hover:bg-white/20 transition-colors"
            >
              {volume === 0 || muted ? (
                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z" />
                </svg>
              ) : (
                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z" />
                </svg>
              )}
            </button>
            <input
              type="range"
              min={0}
              max={1}
              step={0.1}
              value={volume}
              onChange={handleVolumeChange}
              className="w-20 h-1.5 bg-gray-700 rounded-full appearance-none cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-3 [&::-webkit-slider-thumb]:h-3 [&::-webkit-slider-thumb]:bg-red-500 [&::-webkit-slider-thumb]:rounded-full"
            />
          </div>
        </div>
      </div>
    </div>
  );
}

// For non-React usage (Android WebView, etc.)
export function createOTTPlayer(
  container: HTMLElement,
  config: {
    episodeId: string;
    apiUrl?: string;
    autoPlay?: boolean;
    onEnded?: () => void;
    onError?: (error: Error) => void;
  }
) {
  // This would be used for vanilla JS integration
  // Returns destroy function
  return () => {};
}