# Gemini Pro

A lightweight WebView-based application for accessing **Google AI Studio** with a few extra features to enhance your workflow.

# Features

* Instant diagram viewing when copying mermaid code
* Instant HTML page viewing when copying generated HTML code
* The ability to immediately save the copied text to a text file
* **Caffeine mode** - does not allow the phone to turn off the screen
* **Split Screen** - allows you to work in two dialog windows at once in parallel

# How to use

**1. Grant Google Drive permission**

Open Chrome, go to [this webpage](https://aistudio.google.com/prompts/new_chat) and log into your Google account.

Grant permission to Google Drive.

**2. Launch Gemini Pro**

Once permissions are set, you can start using the app immediately.


> Note:
This step is required due to Google’s security policy, which cannot be bypassed.
The same policy also prevents certain downloads within the app, except for audio files.
Generated videos can be saved directly to your Google Drive.

# Screenshots

<img src="https://github.com/user-attachments/assets/f3037666-4945-492f-b06a-65e9d7c96d1e" alt="Main screen" width="250">
<img src="https://github.com/user-attachments/assets/90a572b2-24c1-4cd0-b950-d7351c1e86a3" alt="Additional menu" width="250">
<img src="https://github.com/user-attachments/assets/edeb14b6-dff6-4df1-98c5-0c9541475105" alt="Diagram demonstration" width="250">


# Development Environment

**Gemimi Pro** uses the Gradle build system and can be imported directly into Android Studio (make sure you are using the latest stable version available [here](https://developer.android.com/studio)). 

Change the run configuration to `app`.

![image](https://user-images.githubusercontent.com/873212/210559920-ef4a40c5-c8e0-478b-bb00-4879a8cf184a.png)

The `Debug` and `Release` build variants can be built and run.

![image](https://github.com/user-attachments/assets/99c8078d-37b7-45ce-a721-ede96289ee2e)

