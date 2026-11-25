# AnMedia — Winamp-style Android Media Player

A native Android media player inspired by the classic Winamp desktop player: brushed-metal
panels, LCD green/amber displays, a real-time spectrum visualizer, a 5-band graphic
equalizer, a local media + playlist + YouTube library, and background playback with
lock-screen / Bluetooth controls.

Built with Kotlin, Jetpack Compose, Room, AndroidX Media3 (ExoPlayer + MediaSession),
native `Equalizer`/`Visualizer`, and NewPipeExtractor for YouTube stream resolution.

This project was built and verified to compile end-to-end (`./gradlew assembleDebug`
succeeds and produces an installable debug APK) as part of producing this deliverable.

## ฟีเจอร์ทั้งหมดของแอป (ภาษาไทย)

### หน้าเล่นเพลง (Player)
- เล่นเพลง/วิดีโอทั้งไฟล์ในเครื่องและจาก YouTube ในหน้าเดียวกัน พร้อมปุ่มเล่น/หยุดชั่วคราว/เพลงก่อนหน้า/เพลงถัดไป
- แถบ Seek ลากปรับตำแหน่งเพลงได้ พร้อมแสดงเวลาปัจจุบัน/เวลารวม
- ปุ่ม Shuffle (สุ่มเล่น) และ Repeat (ปิด / วนเพลงเดียว / วนทั้งคิว) โดยจดจำค่าที่ตั้งไว้แม้ปิดแอพแล้วเปิดใหม่
- ปรับระดับเสียง (Volume) และสมดุลเสียงซ้าย-ขวา (Balance) ได้จากหน้าเล่นเพลงโดยตรง
- แผง "Playlist" ต่อคิวเพลงที่กำลังเล่นอยู่ในหน้า Player เอง เพิ่มเพลงจากคลังสื่อ ลบออกจากคิว หรือแตะเพื่อข้ามไปเล่นเพลงอื่นในคิวได้ทันที
- พื้นที่แสดงภาพปก/ภาพวิดีโอสด (hero area) ด้านบนสุด พร้อมไอคอนโน้ตดนตรีเมื่อไม่มีภาพปก
- แถบแจ้งเตือนข้อผิดพลาดการเล่น (error banner) เมื่อเล่นไม่สำเร็จ เช่น ไฟล์โคเด็กไม่รองรับ
- สลับธีมได้ 4 แบบ: System / Light / Dark / Winamp Classic (ธีมย้อนยุคสีโลหะพร้อมจอ LCD เขียว) และจดจำธีมที่เลือกไว้ถาวร
- โหมด Debug Log ในตัวแอพ เปิด/ปิดได้ ดูบันทึกการเล่นแบบละเอียด (ความเร็วเน็ตที่วัดได้, เฟรมหลุด, ข้อผิดพลาด, เหตุผลการเลือกคุณภาพสตรีม ฯลฯ) และกดคัดลอกออกมาได้โดยไม่ต้องต่อคอมพิวเตอร์/ใช้ adb
- ตัวแสดงผลสเปกตรัมเสียงแบบเรียลไทม์ (spectrum visualizer)
- หน้าจอปรับขนาดได้ (responsive) เลื่อนดูได้ทั้งหน้าบนจอขนาดเล็ก ไม่มีส่วนใดถูกบีบจนมองไม่เห็น

### วิดีโอ
- เล่นวิดีโอแบบเต็มจอ (fullscreen) พร้อมปุ่มสลับเข้า-ออกโหมดเต็มจอเอง ซ่อนแถบระบบและหมุนจอเป็นแนวนอนให้อัตโนมัติ
- กดปุ่ม Back เพื่อออกจากโหมดเต็มจอได้ทันทีก่อนออกจากหน้าจอ

### อีควอไลเซอร์ (Equalizer)
- อีควอไลเซอร์กราฟิก 5 แถบ เชื่อมต่อกับ Equalizer เสียงของ Android โดยตรง
- มีพรีเซ็ตสำเร็จรูป: Flat, Rock, Pop, Jazz, Bass Boost และโหมด Custom สำหรับปรับเองทีละแถบ
- ปรับ Preamp (ระดับเสียงรวมก่อนเข้าแถบ EQ) ได้ แม้อยู่ในโหมด Custom ก็ไม่ถูกรีเซ็ต
- บันทึกค่า EQ ที่ตั้งไว้ และโหลดกลับมาให้อัตโนมัติเมื่อเปิดแอพใหม่
- แถบปรับ/พรีเซ็ตจะจางลงและกดไม่ได้เมื่อยังไม่มีเสียงให้เชื่อมต่อ กันสับสนว่าปรับแล้วใช้งานได้จริง

### คลังสื่อ (Library)
- แท็บ **Local**: สแกนเพลงในเครื่องผ่าน MediaStore และกด Rescan เพื่อสแกนใหม่ได้
- แท็บ **Video**: รวมวิดีโอในเครื่อง เล่นได้ทันทีที่แตะ
- แท็บ **Playlists**: สร้าง/ลบเพลย์ลิสต์ (มีกล่องยืนยันก่อนลบ), เพิ่ม/ลบเพลงออกจากเพลย์ลิสต์ (มีกล่องยืนยันก่อนลบ), ลากสลับลำดับเพลงในเพลย์ลิสต์ได้ (drag to reorder)
- ช่องค้นหาคลังสื่อของตัวเอง กรองรายการตามชื่อเพลง/ศิลปิน/ชื่อช่องได้ทันทีที่พิมพ์

### แท็บ YouTube
- **เพิ่มด้วยลิงก์**: วางลิงก์วิดีโอหรือเพลย์ลิสต์ YouTube เพื่อเพิ่มเข้าคิวได้โดยตรง รองรับทั้งลิงก์วิดีโอเดี่ยวและลิงก์เพลย์ลิสต์ทั้งชุด (ดึงทีละวิดีโอให้อัตโนมัติ)
- **ค้นหา YouTube ในแอพ**: พิมพ์คำค้นแล้วกดค้นหา ระบบจะแสดงผลลัพธ์พร้อมภาพปก ชื่อวิดีโอ ชื่อช่อง และความยาวคลิป
- แตะผลการค้นหาเพื่อเล่นทันที หรือกดไอคอน "เพิ่มเข้าเพลย์ลิสต์" เพื่อเลือกว่าจะใส่เพลย์ลิสต์ไหน (สร้างเพลย์ลิสต์ใหม่ หรือเลือกจากที่มีอยู่แล้ว)
- **เลือกโหมดสตรีมได้ก่อนเพิ่ม**: ตอนกดเพิ่มผลการค้นหาเข้าเพลย์ลิสต์ จะมีกล่องให้เลือกก่อนว่าจะเก็บเป็น "Audio only" (เสียงอย่างเดียว ประหยัดเน็ต) หรือ "Video" (มีภาพวิดีโอด้วย) แล้วค่อยเลือกเพลย์ลิสต์ปลายทางต่อ
- **ลบวิดีโอ YouTube ที่เคยเพิ่มไว้ได้**: มีกล่องยืนยันก่อนลบ และลบแล้วจะหายออกจากทุกเพลย์ลิสต์ที่เคยใส่ไว้ด้วยโดยอัตโนมัติ
- ปรับคุณภาพวิดีโอให้เหมาะกับความยาวคลิปโดยอัตโนมัติ (ลดความละเอียด/บิตเรตเสียงสำหรับคลิปยาวมาก) เพื่อลดอาการกระตุกเวลาเน็ตช้า

### การเล่นเบื้องหลัง และความเสถียร
- เล่นต่อเนื่องแบบพื้นหลัง (background playback) พร้อมการแจ้งเตือนควบคุมจากหน้าจอล็อกและปุ่มบนหูฟังบลูทูธ
- ทนทานต่อสัญญาณเน็ตหลุดสั้น ๆ ระหว่างเล่น YouTube (พยายามเชื่อมต่อใหม่อัตโนมัติก่อนจะแจ้งว่าเล่นไม่สำเร็จจริง ๆ)
- แคชผลการแปลงลิงก์ YouTube เป็นสตรีมจริงไว้ชั่วคราว ทำให้เปิดเพลง/วิดีโอเดิมซ้ำได้เร็วโดยไม่ต้องประมวลผลใหม่ทุกครั้ง

### การอัปเดตแอป และหน้า About
- หน้า **About** (ไอคอน "i" บนหน้า Player) แสดงชื่อแอป เวอร์ชันปัจจุบัน คำอธิบายสั้น ๆ และลิงก์ไปซอร์สโค้ดบน GitHub
- แอปตรวจสอบเวอร์ชันใหม่จาก GitHub Releases ของโปรเจกต์โดยอัตโนมัติ (สูงสุดทุก 12 ชั่วโมง) และกดตรวจสอบเองได้ทันทีจากหน้า About
- เมื่อพบเวอร์ชันใหม่ จะมีจุดสีแจ้งเตือนที่ไอคอน About และมีการแจ้งเตือนระบบ (notification) ขึ้นมา กดเพื่อเปิดหน้าดาวน์โหลดได้ทันที

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

### Cutting a release (update channel)

AnMedia is sideloaded, not distributed through the Play Store, so GitHub Releases is the
actual update channel: `app/src/main/java/com/anmedia/player/update/UpdateChecker.kt` polls
`GET /repos/karndiy/an-media/releases/latest` and compares its `tag_name` against the
installed `versionName`, notifying the user (and showing it on the in-app **About** screen)
when the release is newer.

To publish a new version:

1. Bump `versionCode` / `versionName` in `app/build.gradle.kts`.
2. Commit, then tag the commit to match the new version and push the tag:
   ```bash
   git tag v1.0.15
   git push origin v1.0.15
   ```
3. Pushing a `v*` tag triggers `.github/workflows/release.yml`, which builds the debug APK
   and publishes it as a GitHub Release (with the tag's auto-generated notes) — no manual
   upload needed. Users already running the app get the update notification within 12 hours,
   or immediately via "Check for updates" on the About screen.

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
