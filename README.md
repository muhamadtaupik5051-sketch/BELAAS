# VeloxSport Android

Android Studio project wrapping the supplied VeloxSport interface in a WebView with Android location permissions.

## Features
- GPS position via high-accuracy browser geolocation (real location, no simulated speed/distance)
- OpenStreetMap / Leaflet live map and route trace
- Distance calculated from accepted GPS points; speed from point-to-point GPS deltas; elapsed timer
- Activity history and profile saved locally on device with localStorage
- Premium interface, route presets, group tab and profile

## Build APK
Open this folder in Android Studio and build `app` > `assembleDebug`, or push it to GitHub and run the **Build VeloxSport APK** workflow. The workflow uploads `app-debug.apk` as an artifact.

The map tiles and existing Tailwind/Leaflet/Font Awesome/Chart.js assets use public HTTPS CDNs and require internet. GPS requires granting location permission and enabling device location. Tracking is intended for while the app is open; background tracking service is not implemented.
