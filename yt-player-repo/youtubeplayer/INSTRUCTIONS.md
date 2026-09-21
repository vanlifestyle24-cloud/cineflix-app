# YouTube Player Module Setup

This module includes a custom Plyr-based YouTube player designed for Android WebView integration.

## Files
The following files have been updated in `src/main/assets`:
- **`player.html`**: The main HTML file containing the player structure, custom CSS, and logic.
- **`plyr.css`**: The core Plyr stylesheet (v3.7.8).
- **`plyr.js`**: The core Plyr JavaScript library (v3.7.8).
- **`plyr.svg`**: SVG sprite for player icons.

## Android Integration

To use this player in your Android application's WebView:

1.  **Enable JavaScript & DOM Storage**:
    ```java
    WebView webView = findViewById(R.id.webview);
    WebSettings webSettings = webView.getSettings();
    webSettings.setJavaScriptEnabled(true);
    webSettings.setDomStorageEnabled(true);
    webSettings.setMediaPlaybackRequiresUserGesture(false); // Optional: for autoplay
    ```

2.  **Load the Player**:
    ```java
    webView.loadUrl("file:///android_asset/player.html");
    ```

3.  **Handling Video Loading (Optional)**:
    Since `player.html` includes its own input controls for testing, you can hide them or interact with the player programmatically via `evaluateJavascript`.

    *Example to load a video programmatically:*
    ```java
    String videoId = "Qt0-9ZtRwHg"; // Example ID
    webView.evaluateJavascript("loadVideo('" + videoId + "');", null);
    ```

## Customization
- The player logic is contained within the `<script>` tag in `player.html`.
- Custom styles (Red accent, Dark theme) are in the `<style>` tag in the `<head>` of `player.html`.
