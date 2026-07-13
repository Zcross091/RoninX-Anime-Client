<div align="center">

   <img src="assets/images/app_icon.png" alt="Ronin Logo" width="120"/>

# Ronin

### Read. Watch. Track.

[![Flutter](https://img.shields.io/badge/Flutter-≥3.8.1-02569B?logo=flutter)](https://flutter.dev/)
[![Dart](https://img.shields.io/badge/Dart-≥3.8.1-0175C2?logo=dart)](https://dart.dev)

**The official frontend client for the Ronin API.**

An elegant, open-source anime companion app natively connected to the Ronin API for lightning-fast media fetching. Syncs cleanly with MyAnimeList and AniList for seamless tracking and discovery.

</div>

---

## ✨ Features

- **Direct API Integration:** Connects directly to your Ronin backend via Vercel proxy. No third-party extension installations required!
- **Omni-Sync Tracking:** Native bidirectional integration with MyAnimeList and AniList.
- **Custom Player:** Low-latency media playback with customizable flow controls and AMOLED dark mode support.
- **Cross-Platform:** High-performance native builds for Android, Windows, and Linux.

---

## 🛠️ Technology Stack

**Framework**: Flutter ≥3.8.1 | **Language**: Dart ≥3.8.1

<details>
<summary><b>View Key Dependencies</b></summary>

```yaml
dependencies:
  flutter_riverpod: ^3.0.1
  go_router: ^14.7.1
  media_kit: ^1.2.6
  media_kit_video: ^2.0.1
  isar_community: ^3.3.0
  flex_color_scheme: ^8.4.0
```

</details>

---

## 🚀 Installation & Building

**Prerequisites**: Flutter SDK ≥3.8.1, Git, Node.js (Optional for some tools)

```bash
git clone <your-repo-url>
cd ronin-app
fvm flutter pub get
fvm flutter pub run build_runner build --delete-conflicting-outputs
fvm flutter run # Select your device/platform
```

---

## 🎨 Theme & Customization

Ronin comes pre-loaded with a dedicated **Ronin Theme** that perfectly matches the dark base, striking crimson, and gold accents of the official website template. 

To activate the theme:
1. Open the App
2. Navigate to **Settings** -> **Appearance**
3. Select **Ronin** from the custom exclusive schemes list!

---

## ⚖️ Legal Disclaimer

This is a frontend application for the Ronin API. The app itself does not host, upload, or own any of the media displayed. All metadata (covers, synopses, schedules) is pulled from public APIs like AniList and MyAnimeList. Video streams are served directly via the connected backend API.

---

<div align="center">

**Made for the Ronin Ecosystem.**

</div>
