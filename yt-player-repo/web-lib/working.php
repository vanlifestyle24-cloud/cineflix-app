<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Plyr Player</title>
    <!-- Plyr CSS -->
    <link rel="stylesheet" href="plyr.css" />
    <style>
        /* Modern Dark Theme Variables */
        :root {
            --plyr-color-main: #f00;
            /* YouTube Red Accent */
            --plyr-video-background: #000;
            --plyr-menu-background: rgba(20, 20, 20, 0.9);
            --plyr-menu-color: #fff;
            --plyr-menu-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
            --plyr-font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
            --plyr-control-icon-size: 18px;
            --plyr-control-spacing: 10px;
        }

        /* Input Controls Styling */
        .controls-container {
            margin-bottom: 20px;
            text-align: center;
            padding: 20px;
            background: #f8f9fa;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }

        #videoUrl {
            padding: 12px;
            width: 300px;
            border: 2px solid #ddd;
            border-radius: 6px;
            font-size: 14px;
            transition: border-color 0.3s;
            outline: none;
        }

        #videoUrl:focus {
            border-color: #f00;
        }

        .btn {
            padding: 12px 24px;
            cursor: pointer;
            color: white;
            border: none;
            border-radius: 6px;
            font-weight: 600;
            font-size: 14px;
            transition: transform 0.1s, opacity 0.2s;
            margin: 0 5px;
        }

        .btn:active {
            transform: scale(0.98);
        }

        .btn-load {
            background: #212121;
        }

        .btn-live {
            background: #f00;
        }

        /* Plyr Customization */
        .plyr--full-ui input[type=range] {
            color: var(--plyr-color-main);
        }

        .plyr__control--overlaid {
            background: rgba(240, 0, 0, 0.8) !important;
        }

        .plyr--video .plyr__controls {
            background: linear-gradient(rgba(0, 0, 0, 0), rgba(0, 0, 0, 0.75));
            border-bottom-left-radius: inherit;
            border-bottom-right-radius: inherit;
        }

        /* CSS to hide YouTube Branding */
        .plyr__video-embed iframe {
            top: -50%;
            height: 200%;
            /* pointer-events: none; - Enabling pointer events so users can interact if needed, or keep disabled if strict no-interaction is desired. 
               User asked for "look exactly as older one" but custom. Keeping the crop hack. */
            pointer-events: none;
        }

        .plyr__video-wrapper {
            overflow: hidden;
            border-radius: 8px;
            /* Rounded corners for the player */
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
        }
    </style>
</head>

<body>

    <!-- 
    # Quick setup 
    ## HTML
    ### YouTube (Progressive Enhancement)
    We recommend progressive enhancement with the embedded players. 
    You can elect to use an <iframe> as the source element (which Plyr will progressively enhance).
    -->
    <!-- Video Loading Controls -->
    <!-- Video Loading Controls -->
    <div class="controls-container">
        <input type="text" id="videoUrl" placeholder="Enter YouTube URL, Shorts, or ID" value="https://youtube.com/shorts/Oke5OqwRFeM?si=AvHjlMYT0vDvnW46">
        <button id="loadBtn" class="btn btn-load">Load Video</button>
        <button id="liveBtn" class="btn btn-live">Live Stream</button>
    </div>

    <!-- Plyr Player Container -->
    <div class="plyr__video-embed" id="player">
        <iframe
            src="https://www.youtube.com/embed/Oke5OqwRFeM?origin=https://plyr.io&amp;iv_load_policy=3&amp;modestbranding=1&amp;playsinline=1&amp;showinfo=0&amp;rel=0&amp;enablejsapi=1"
            allowfullscreen allowtransparency allow="autoplay"></iframe>
    </div>

    <!-- 
    ## JavaScript
    Alternatively you can include the plyr.js script before the closing </body> tag 
    and then in your JS create a new instance of Plyr as below.
    -->
    <script src="plyr.js"></script>
    <script>
        const player = new Plyr('#player', {
            iconUrl: 'plyr.svg',
            controls: [
                'play-large', // The large play button in the center
                'restart', // Restart playback
                'rewind', // Rewind by the seek time (default 10 seconds)
                'play', // Play/pause playback
                'fast-forward', // Fast forward by the seek time (default 10 seconds)
                'progress', // The progress bar and scrubber for playback and buffering
                'current-time', // The current time of playback
                'duration', // The full duration of the media
                'mute', // Toggle mute
                'volume', // Volume control
                'captions', // Toggle captions
                'settings', // Settings menu
                'pip', // Picture-in-picture (currently Safari only)
                'airplay', // Airplay (currently Safari only)
                'fullscreen', // Toggle fullscreen
            ],
            // Default settings to show in the menu
            settings: ['captions', 'quality', 'speed', 'loop'],
            // Speed options
            speed: { selected: 1, options: [0.5, 0.75, 1, 1.25, 1.5, 1.75, 2, 4] },
            // Quality options
            quality: {
                default: 576,
                options: [4320, 2880, 2160, 1440, 1080, 720, 576, 480, 360, 240]
            },
            // Keyboard shortcuts
            keyboard: { focused: true, global: false },
            // Tooltips
            tooltips: { controls: true, seek: true },
            // Other options
            clickToPlay: true,
            autopause: true,
            resetOnEnd: false,
            invertTime: true,
            toggleInvert: true,
            disableContextMenu: true,
            ratio: '16:9',
            youtube: {
                noCookie: false,
                rel: 0,
                showinfo: 0,
                iv_load_policy: 3,
                modestbranding: 1,
                controls: 0,
                fs: 0
            }
        });

        // Function to extract YouTube Video ID
        function extractVideoId(url) {
            const regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|\&v=|shorts\/)([^#\&\?]*).*/;
            const match = url.match(regExp);
            return (match && match[2].length === 11) ? match[2] : url; // Return input if it looks like an ID
        }

        // Function to load video
        function loadVideo(id) {
            console.log('Loading video:', id);
            player.source = {
                type: 'video',
                sources: [
                    {
                        src: id,
                        provider: 'youtube',
                    },
                ],
            };
        }

        // Event Listeners
        document.getElementById('loadBtn').addEventListener('click', () => {
            const url = document.getElementById('videoUrl').value;
            const id = extractVideoId(url);
            if (id && id.length === 11) {
                loadVideo(id);
            } else {
                alert('Invalid YouTube URL or ID');
            }
        });

        document.getElementById('liveBtn').addEventListener('click', () => {
            // Use the live stream ID mentioned in context
            const liveStreamId = 'NRHIBKNNzAk';
            document.getElementById('videoUrl').value = liveStreamId;
            loadVideo(liveStreamId);
        });
    </script>
</body>

</html>