# FuelTrack — get the APK without Android Studio

The app lives in `app/src/main/assets/index.html`, wrapped in a native Android
WebView so the camera/gallery photo picker works. Pick one of the two free routes
below — neither needs Android Studio installed on your computer.

---

## Route A — GitHub builds the APK for you (free, recommended)

This project includes a workflow (`.github/workflows/android.yml`) that compiles the
APK on GitHub's servers. You just upload the files once.

1. Make a free account at https://github.com and click **New repository**
   (name it anything, e.g. `fueltrack`, keep it Private, click Create).
2. On the new repo page, click **uploading an existing file**, then drag in
   **all the contents of this folder** (the `app` folder, `.github` folder,
   `build.gradle`, `settings.gradle`, `gradle.properties`). Commit.
3. Go to the **Actions** tab. A build called "Build APK" starts automatically
   (give it ~3–5 minutes). Open it when the green tick appears.
4. At the bottom of that run, under **Artifacts**, download **FuelTrack-apk**.
   Inside the zip is `app-debug.apk`.
5. Copy that APK to your phone, tap it, allow "install from this source". Done.

If a run ever fails, open it, read the red step, tell me what it says and I'll fix it.

---

## Route B — online web-to-APK converter (fastest, no account coding)

If you'd rather not touch GitHub, use a website that turns the web app into an APK.
Upload the separate **fueltrack-web.zip** I sent (it has `index.html` at the top).

Examples (free tiers; may add a small splash/ad): WebIntoApp, Median.co, AppsGeyser.
Steps are the same on each: create app → upload the HTML zip (or paste a hosted URL)
→ set app name "FuelTrack" → Build → download the APK.

Note: these simple wrappers may restrict the camera on some converters; if photo
capture doesn't work there, use Route A, which handles it properly.

---

## What this build does vs. next step

Working now: dashboard, scan/confirm flow, trips (start, resume, photo upload),
history, stats, all calculations, camera + gallery photo capture, offline, data on
device. It does **not** yet auto-read the digits from a photo — you confirm the
numbers. That OCR auto-fill needs Google ML Kit added to `MainActivity`; ask me and
I'll add it to this project.
