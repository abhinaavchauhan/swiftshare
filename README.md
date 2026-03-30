# 🚀 SwiftShare — High-Speed Bluetooth File Transfer

<p align="center">
  <b>Lightning-Fast Offline File Sharing</b>
</p>

SwiftShare is a premium Android application for high-speed offline file transfer using Bluetooth RFCOMM sockets. Built with Material Design 3, MVVM architecture, and production-ready code.

---

## ✨ Features

- **📤 Send Files** — Select files via system picker and send to nearby Bluetooth devices
- **📥 Receive Files** — Enable discoverability and accept incoming file transfers
- **📊 Transfer Progress** — Real-time circular progress, speed (MB/s), and ETA display
- **📜 Transfer History** — Complete history with filter chips (All/Sent/Received)
- **🔍 Device Discovery** — Animated scanning with signal strength indicators
- **🌙 Dark Mode** — Full dark theme support with runtime switching
- **🔔 Notifications** — Foreground service with progress notification
- **🔒 Permissions** — Smart runtime permission handling for all API levels 

## 🏗️ Architecture

```
SwiftShare follows MVVM (Model-View-ViewModel) architecture:

┌─────────────────────────────────────────┐
│              UI Layer                    │
│  Activities ← Fragments ← ViewModels   │
├─────────────────────────────────────────┤
│           Repository Layer               │
│  TransferRepository ← TransferDao       │
├─────────────────────────────────────────┤
│            Data Layer                    │
│  Room Database ← SharedPreferences      │
├─────────────────────────────────────────┤
│          Service Layer                   │
│  FileTransferService ← BTConnection    │
├─────────────────────────────────────────┤
│        Bluetooth Layer                   │
│  ConnectionManager ← TransferManager   │
└─────────────────────────────────────────┘
```

## 📁 Project Structure

```
app/src/main/
├── java/com/swiftshare/app/
│   ├── SwiftShareApp.java              # Application class
│   ├── bluetooth/
│   │   ├── BluetoothConnectionManager.java  # RFCOMM socket management
│   │   └── FileTransferManager.java         # File streaming protocol
│   ├── data/
│   │   ├── dao/TransferDao.java        # Room DAO
│   │   ├── database/AppDatabase.java   # Room database
│   │   ├── model/
│   │   │   ├── TransferEntity.java     # Transfer record entity
│   │   │   ├── DeviceItem.java         # Bluetooth device model
│   │   │   └── FileItem.java           # Selected file model
│   │   ├── preferences/AppPreferences.java  # SharedPreferences
│   │   └── repository/TransferRepository.java
│   ├── service/
│   │   ├── FileTransferService.java    # Foreground transfer service
│   │   └── BluetoothConnectionService.java  # Discovery service
│   ├── ui/
│   │   ├── MainActivity.java           # Navigation host
│   │   ├── SplashActivity.java         # Splash screen
│   │   ├── adapter/
│   │   │   ├── TransferHistoryAdapter.java
│   │   │   └── DeviceAdapter.java
│   │   ├── viewmodel/
│   │   │   ├── HomeViewModel.java
│   │   │   ├── HistoryViewModel.java
│   │   │   ├── DeviceDiscoveryViewModel.java
│   │   │   └── TransferViewModel.java
│   │   ├── home/HomeFragment.java
│   │   ├── send/SendFragment.java
│   │   ├── receive/ReceiveFragment.java
│   │   ├── discovery/DeviceDiscoveryFragment.java
│   │   ├── transfer/TransferFragment.java
│   │   ├── history/HistoryFragment.java
│   │   └── settings/SettingsFragment.java
│   └── utils/
│       ├── FileUtils.java              # File size/speed formatting
│       └── PermissionUtils.java        # Runtime permissions
├── res/
│   ├── layout/                         # XML layouts
│   ├── navigation/nav_graph.xml        # Navigation graph
│   ├── menu/bottom_nav_menu.xml
│   ├── drawable/                       # Gradients, vectors, shapes
│   ├── anim/                           # Transitions, pulse effects
│   ├── values/                         # Colors, strings, themes, dimens
│   └── xml/file_paths.xml             # FileProvider paths
└── AndroidManifest.xml
```

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| **Java 17** | Core application logic |
| **Groovy DSL** | Gradle build system |
| **Android SDK 34** | Target platform |
| **Material Design 3** | Premium UI components |
| **Room Database** | Transfer history persistence |
| **Navigation Component** | Fragment navigation |
| **LiveData + ViewModel** | Reactive MVVM |
| **Bluetooth RFCOMM** | File transfer protocol |
| **Foreground Services** | Background transfers |
| **ViewBinding** | Type-safe view access |

## 📱 Minimum Requirements

- Android 8.0 (API 26) or higher
- Bluetooth hardware support
- Storage access

## 🚀 Getting Started

1. **Open in Android Studio** (Hedgehog 2023.1.1 or later)
2. **Sync Gradle** — Android Studio will download dependencies
3. **Build and Run** on a physical device (Bluetooth requires real hardware)

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

## 📄 License

This project is available for personal and portfolio use.

---

<p align="center">
  Built with ❤️ by SwiftShare
</p>
