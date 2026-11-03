# Gemini Pro

A lightweight WebView-based application for accessing **Google AI Studio** with a few extra features to enhance your workflow.

# Features

*   **Instant Diagram Viewing**: Automatically detect and render Mermaid diagrams from copied text.
*   **HTML Preview**: Instantly view and interact with generated HTML code snippets.
*   **Save to File**: Quick option to save copied text directly to a `.txt` file.
*   **Caffeine Mode**: Prevent the screen from turning off while you're deep in your workflow.
*   **Media Downloads**:
    *   **Images**: Press and hold on any image to save it.
    *   **Videos**: Enable **Video Selection Mode** from the menu, then tap and hold on a video to download it.
    *   **Audio**: Enable **Audio Selection Mode** from the menu, then tap and hold on an audio element to download it.
*   **Quick Access Tools**: Direct links to **Google Docs** and **Google Flow (Fx)** via the additional menu.
*   **Navigation & Sharing**: Share the current page, copy links, and navigate forward/backward easily.
*   **Customizable UI**: Choose between left or right side placement for the menu handle to suit your preference.
*   **Gestures**:
    *   **Reload**: Swipe down from the top center handle.
    *   **Menu**: Swipe from the side handle to open additional features.
*   **Update System**: Built-in notifications to keep you on the latest version.

# How to use

**1. Grant Google Drive permission**

Open Chrome, go to [this webpage](https://aistudio.google.com/prompts/new_chat) and log into your Google account.

Grant permission to Google Drive.

**2. Launch Gemini Pro**

Once permissions are set, you can start using the app immediately.

**3. Gestures & Navigation**

*   **Reload Page**: Drag down from the top center of the screen.
*   **Open Additional Menu**: Drag the side handle (left or right).
*   **Download Video / Audio**: Open the additional menu, tap **Download Video** or **Download Audio**, then tap and hold the media element on the page.

> **Note:**
The initial Google Drive permission step is required due to Google’s security policy, which cannot be bypassed in a custom WebView.

# Screenshots

<img src="https://github.com/user-attachments/assets/f3037666-4945-492f-b06a-65e9d7c96d1e" alt="Main screen" width="250">
<img src="https://github.com/user-attachments/assets/c49c921e-9c06-4a5a-ba7e-c88b2cecd60a" alt="Additional menu" width="250">
<img src="https://github.com/user-attachments/assets/edeb14b6-dff6-4df1-98c5-0c9541475105" alt="Diagram demonstration" width="250">


# Development Environment

**Gemini Pro** uses the Gradle build system and can be imported directly into Android Studio (make sure you are using the latest stable version).

Change the run configuration to `app`.

![image](https://user-images.githubusercontent.com/873212/210559920-ef4a40c5-c8e0-478b-bb00-4879a8cf184a.png)

The `Debug` and `Release` build variants can be built and run.

![image](https://github.com/user-attachments/assets/99c8078d-37b7-45ce-a721-ede96289ee2e)

# Security & Privacy

Gemini Pro prioritizes user security and data privacy:
* **Scoped Storage**: Blob and media downloads use Android's `MediaStore` without requiring broad storage permissions.
* **Hardened Sandbox**: Local file access, cross-origin file URLs, and legacy web databases are strictly disabled within the WebView.
* **Intent Validation**: External links and `intent://` schemes are sanitized to prevent launching unexported application components.

For more details, see our [Security Policy](SECURITY.md) and full [Security Audit Report](docs/SECURITY_AUDIT.md).

