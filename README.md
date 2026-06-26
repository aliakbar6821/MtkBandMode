# MtkBandMode

Minimal Android app that ports the **Band Mode** section from MTK EngineerMode.

## Features
- GSM / UMTS / LTE / NR tabs with checkboxes (one per band)
- Reads/writes selections to SharedPreferences (same keys as original APK)
- Applies via `TelephonyManager` hidden API (reflection) – works on platform-signed or rooted builds
- GitHub Actions builds a debug APK on every push; add keystore secrets for signed release

## Quick start

```bash
git clone <your-repo>
cd MtkBandMode
# Add Gradle wrapper (required – not committed here):
gradle wrapper --gradle-version 8.7
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions – signed release

Add these repository secrets:
| Secret | Content |
|--------|---------|
| `KEYSTORE_BASE64` | `base64 -w0 your.keystore` |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |
| `STORE_PASSWORD` | store password |

## Architecture

```
MainActivity          – SIM slot picker
BandSelectActivity    – TabLayout + ViewPager2 (GSM | UMTS | LTE | NR)
BandTabFragment       – checkbox list per RAT, calls BandSelectHelper on SET
BandSelectHelper      – TelephonyManager reflection wrapper + SharedPreferences
BandModeContent       – all band bitmask constants (from BandModeContent.smali)
```

## Permission note

`MODIFY_PHONE_STATE` is needed to actually push the band mask to the modem.
On stock ROM this requires platform signature.
On rooted devices you can grant it via:
```
adb shell pm grant com.example.mtkbandmode android.permission.MODIFY_PHONE_STATE
```

## Band bitmasks

Reverse-engineered from `BandModeContent.smali` and `BandSelect$BandModeMap.smali`.
GSM/UMTS use a single 32-bit mask. LTE/NR use two 64-bit longs (low = bands 1-64, high = bands 65+).
