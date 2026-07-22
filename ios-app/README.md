# Smart Estate iOS push app

Native SwiftUI app for the smart-estate Spring backend. Handles normal push notifications and
**fire-alarm "force" notifications** (loud looping siren + haptics + full-screen alarm).

## What's here

```
ios-app/
├─ project.yml                 # XcodeGen project spec (generates SmartEstatePush.xcodeproj)
├─ Sources/
│  ├─ App/                     # @main app, AppDelegate (APNs + notification delegate), RootView
│  ├─ Config/AppConfig.swift   # backend base URL + push environment  (EDIT baseURL for a device)
│  ├─ Localization/            # LocalizationManager (runtime EN / 繁中 switch) + AppLanguage
│  ├─ Networking/              # APIClient, models, AuthStore, Keychain
│  ├─ Push/                    # PushManager, AlarmController (siren/haptics), NotificationRouter
│  └─ Views/                   # Login, Inbox, NotificationRow, AlarmView, Settings
└─ Resources/
   ├─ Info.plist               # background remote-notification, dev ATS
   ├─ SmartEstatePush.entitlements  # aps-environment + Time Sensitive
   ├─ fire_alarm.caf           # siren (push sound + in-app looping alarm)
   ├─ en.lproj/Localizable.strings       # English UI strings
   ├─ zh-Hant.lproj/Localizable.strings  # Traditional Chinese UI strings
   └─ Assets.xcassets
```

## Bilingual UI (English / 繁體中文)

The app chrome is fully bilingual and switchable at runtime from **Settings > Language**
(Follow system / English / 繁體中文), independent of the phone's language.

- All UI strings live in `Resources/{en,zh-Hant}.lproj/Localizable.strings` (keys kept in sync).
- `LocalizationManager` (an `@EnvironmentObject`) loads strings from the selected language's
  `.lproj` bundle and re-renders the tree when the choice changes. Views call `loc.t("key")`;
  non-view code uses `LocalizationManager.shared`.
- **Push content is not translated on the device.** The backend delivers each notification's
  title/body already in the recipient's account language (`User.language`), so incoming pushes
  are rendered as received. Only app-generated wording (tabs, buttons, the fire-alarm fallback
  text, category/severity chips) is localised here.
- To add a string: add the key to **both** `.strings` files, then reference it via `loc.t("key")`.
  Enum labels (category/severity/emergency type) use `enum.<NAME>` keys mirroring the web UI's
  `frontend/src/lib/enums.js`.

## Generate & open the project

Requires [XcodeGen](https://github.com/yonwoo9/XcodeGen) (`brew install xcodegen`):

```bash
cd ios-app
xcodegen generate
open SmartEstatePush.xcodeproj
```

`SmartEstatePush.xcodeproj` is generated; don't hand-edit it. Change `project.yml` and re-run
`xcodegen generate`.

## Run against the local backend

1. Start the backend (`cd ../smart-estate && ./run.sh`, serves on `:8080`).
2. **Simulator:** `AppConfig.baseURL` = `http://localhost:8080` works as-is. Build & run the
   `SmartEstatePush` scheme on any iPhone simulator.
   - Push tokens don't work in the simulator, but you can exercise the whole UI and use
     **Settings > "Test fire alarm (local)"** to see/hear the alarm screen.
3. **Real device:** set `AppConfig.baseURL` to your Mac's LAN IP (e.g. `http://192.168.1.20:8080`)
   and complete the APNs setup below.

## APNs setup (real push)

Backend side: set these in `smart-estate/src/main/resources/application.properties` (or env):

```
app.push.apns.enabled=true
app.push.apns.environment=SANDBOX
app.push.apns.team-id=<your 10-char Team ID>
app.push.apns.key-id=<your .p8 Key ID>
app.push.apns.bundle-id=hk.smartestate.app
app.push.apns.p8-key-path=/path/to/AuthKey_XXXXX.p8
```

Apple side:

1. Create an **App ID** `hk.smartestate.app` with **Push Notifications** enabled.
2. Create an **APNs Auth Key (.p8)** (Keys > APNs), then note the **Key ID** and your **Team ID**.
3. In Xcode target **Signing & Capabilities**, set your team, add **Push Notifications** and
   **Time Sensitive Notifications** capabilities.
4. Build to a real device, sign in, allow notifications, and the app registers its token via
   `POST /api/mobile/devices`. Declare a **FIRE** emergency in the estate admin UI to trigger it.

## Fire-alarm behavior and limitations

- Fire pushes are sent **time-sensitive** with `fire_alarm.caf` and an `alarm: true` flag. On tap /
  foreground the app shows the full-screen `AlarmView` with a **looping siren** (plays through the
  ring/silent switch while the app is open, via an `.playback` audio session) + continuous haptics,
  until the resident taps **ACKNOWLEDGE**.
- Time-Sensitive breaks through Focus/DND, but **cannot override the hardware silent switch while
  the app is backgrounded**; only Apple's **Critical Alerts** entitlement can. The payload is built
  so upgrading later is a one-line backend change.
