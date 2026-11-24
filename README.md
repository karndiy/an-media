# AnMedia — Winamp-style Android Media Player

A native Android media player inspired by the classic Winamp desktop player: brushed-metal
panels, LCD green/amber displays, a real-time spectrum visualizer, a 5-band graphic
equalizer, a local media + playlist + YouTube library, and background playback with
lock-screen / Bluetooth controls.

Built with Kotlin, Jetpack Compose, Room, AndroidX Media3 (ExoPlayer + MediaSession),
native `Equalizer`/`Visualizer`, and NewPipeExtractor for YouTube stream resolution.

This project was built and verified to compile end-to-end (`./gradlew assembleDebug`
succeeds and produces an installable debug APK) as part of producing this deliverable.

## 1. Prerequisites

- **Android Studio** Ladybug (2024.2) or newer.
- **JDK 17+** (JDK 23 was used to verify this build; anything AGP 8.7 supports works).
- **Android SDK Platform 34** and **Build-Tools 34.0.0+** (installed via the SDK Manager).
- A device or emulator running **Android 8.0 (API 26)** or newer.
- An internet connection on first sync (Gradle needs to download dependencies, and
  NewPipeExtractor is fetched from JitPack).

## 2. Opening the project

1. Unzip / clone the project to a local folder.
2. Open Android Studio → **File ▸ Open...** → select the project's root folder
   (the one containing `settings.gradle.kts`).
3. Let Gradle sync. The wrapper (`gradlew` / `gradlew.bat` / `gradle/wrapper/gradle-wrapper.jar`)
   is already included and pinned to **Gradle 8.11.1**, so Android Studio will download that
   exact Gradle distribution automatically — no manual Gradle install needed.
4. If `local.properties` doesn't already point at your SDK, Android Studio will create/fix it
   for you on first sync (it's machine-specific and already gitignored).

### Building from the command line

```bash
# Debug build (unsigned, installable on a device with USB debugging):
./gradlew assembleDebug
# APK lands at app/build/outputs/apk/debug/app-debug.apk

# Install directly on a connected device/emulator:
./gradlew installDebug

# Run unit tests:
./gradlew test
```

On Windows PowerShell use `.\gradlew.bat` instead of `./gradlew`.

A **release** build (`assembleRelease`) will additionally need a real signing config —
`app/build.gradle.kts` has an empty `signingConfigs.create("release") { }` placeholder block
ready for your keystore details before shipping.

## 3. Runtime permissions

On first launch the app requests:

- `READ_MEDIA_AUDIO` / `READ_MEDIA_VIDEO` (Android 13+) or `READ_EXTERNAL_STORAGE`
  (Android 12 and below) — to scan your device's local library via `MediaStore`.
- `RECORD_AUDIO` — required by Android's `Visualizer` API to capture the live audio session
  for the spectrum analyzer. If denied, the player still works, just without the visualizer.
- `POST_NOTIFICATIONS` (Android 13+) — for the lock-screen / background-playback notification.

## 4. Project structure

```
app/src/main/java/com/anmedia/player/
├── data/
│   ├── local/            Room entities, DAOs, AppDatabase, type converters
│   ├── mediastore/       MediaStore scanner for local audio/video
│   └── repository/       MediaRepository, PlaylistRepository, PlayerStateRepository
├── domain/
│   ├── model/            SourceType, StreamMode, RepeatMode, EqPreset
│   └── youtube/          NewPipeExtractor downloader + YouTube stream resolver
├── player/               PlayerManager (MediaController wrapper), AudioEffectsController
│                         (Equalizer + Visualizer), BalanceAudioProcessor, QueueBuilder,
│                         YoutubeResolvingDataSource, CustomRenderersFactory
├── service/              PlaybackService (Media3 MediaSessionService)
├── permissions/          Compose runtime-permissions helper
├── di/                   Hand-rolled dependency container + ViewModel factory
└── ui/
    ├── theme/            Winamp retro palette, LCD typography, shapes
    ├── components/       LCD display, bevel buttons/sliders, spectrum visualizer,
    │                     drag-to-reorder list
    ├── navigation/       Bottom-nav destinations + NavHost
    └── screens/          player/, equalizer/, library/, youtube/, video/, common/
```

## 5. How playback is wired together

- `PlaybackService` is a Media3 `MediaSessionService`: it owns the single `ExoPlayer`
  instance and `MediaSession`, giving you the foreground notification, lock-screen controls,
  and Bluetooth/headset button handling for free.
- The UI talks to it exclusively through a `MediaController` (`PlayerManager`), which is the
  standard, process-safe way to drive an `ExoPlayer` living in a service.
- **YouTube playback is lazy**: a queued YouTube item's `MediaItem` URI is simply the original
  `youtube.com/watch?v=...` URL. A custom `ResolvingDataSource.Resolver`
  (`YoutubeResolvingDataSource`) intercepts it on the loader thread the moment ExoPlayer tries
  to open it, resolves the real CDN stream URL via NewPipeExtractor, and hands back the
  resolved `DataSpec`. Resolutions are cached in memory with a TTL and the CDN URL is also
  cached in Room — so a saved YouTube track replays instantly until the URL expires, at which
  point it's transparently re-resolved.
- **Equalizer & Visualizer** (`AudioEffectsController`) and the **stereo balance processor**
  (`StereoBalanceController`) are process-wide singletons rather than routed through the
  MediaController. Since the service and the UI run in the same app process, Compose collects
  their `StateFlow`s directly — no IPC needed for 20–30 fps visualizer data.

## 6. Known trade-offs worth knowing about

- **YouTube video mode** uses NewPipeExtractor's best *muxed* (audio+video combined) stream.
  Some videos only expose separate video-only/audio-only DASH streams at higher resolutions;
  this build does not merge them, so very high-resolution video playback isn't attempted —
  it prefers a lower muxed rendition instead. Audio-only mode is unaffected.
- The graphic equalizer attaches to Android's native `Equalizer` once an audio session exists
  (i.e., once something starts playing) — the EQ screen shows a short notice until then.
- NewPipeExtractor scrapes YouTube's web client; it can break when YouTube changes its
  internal APIs. Keep the `com.github.TeamNewPipe:NewPipeExtractor` version in
  `app/build.gradle.kts` up to date. Use this only in contexts where that's consistent with
  YouTube's Terms of Service for your use case (e.g., personal/offline use), the same caveat
  that applies to any NewPipe-based player.
- `signingConfigs.release` is intentionally empty — add your keystore before building a
  release artifact.

## 7. Verified build

This exact source tree was compiled with:

- Gradle **8.11.1**, Android Gradle Plugin **8.7.2**, Kotlin **1.9.24**, KSP **1.9.24-1.0.20**
- `./gradlew assembleDebug` → **BUILD SUCCESSFUL**, producing
  `app/build/outputs/apk/debug/app-debug.apk`

If your environment can't reach `repo.maven.apache.org` directly (some corporate networks
block it while allowing other Maven mirrors), `settings.gradle.kts` already points dependency
resolution at `https://repo1.maven.org/maven2` instead, which serves the same Maven Central
content.
