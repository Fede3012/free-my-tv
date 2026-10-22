# Version 1: what changed and how

This repository is the base of Free My TV. Other projects were built on top of it with bigger visual changes, so this
one is treated as **version 1**. A version 2 will be a fork of it. These notes record the changes made on the
`peru-setup` branch, how each was achieved and what to watch for when porting them.

Reference TV: **TCL 32S5K**, Android 14 (build `UKN2.241110.001`), 1280x720.

| # | Change | Commits |
|---|---|---|
| 1 | Keep the TV's own screensaver (`screensaver=system`) | `cf52ae2` |
| 2 | Spanish, prices in soles and a Peru-friendly shelf | `11c7846` |
| 3 | The remote's settings button: accessibility service and panel | `0ccbb0a`, `9d55c6b`, `f539a8e`, `ce5228f` |

---

## 1. Keep the TV's original screensaver

**Goal.** Let people keep Google Photos ambient mode (or whatever the TV ships) instead of Free My TV's screensaver.

**How.**

- `config.properties` gets `screensaver=free|system` (default `free`). `build.sh` reads it and writes
  `Config.SYSTEM_SCREENSAVER` into the generated `Config.java`.
- `HomeMode.applyFree`: `useScreensaver()` only sets `screensaver_components` to our dream when the option is `free`;
  with `system` it deletes our value if an earlier switch left it. `targets()` leaves the TV's dream package out of the
  list to turn off, and `applyFree` re-enables it (`pm enable`) if a previous run had disabled it.
- `HomeMode.verifyNow` (boot and update check) follows the same setting, so it doesn't flip the screensaver back.

**Watch for.** The check compares the current `screensaver_components` with our dream, so both directions are covered:
`free` puts ours back, `system` removes ours.

## 2. Spanish, soles and a Peru-friendly shelf

**Goal.** Use the launcher in Peru: Spanish UI, prices in soles, the streaming apps people use there.

**How.**

- `Ui.tr(pt, en, es)` takes a third argument. Every call site was updated, plus dates, weather descriptions, tabs and
  TMDB titles (`es-MX`). `language` accepts `en`, `pt`, `es` or `auto`.
- `PEN` currency. Binance has no sol pairs, so prices are requested in USDT and multiplied by the dollar's price in
  soles from CoinGecko (`HomeData.pricesBody`). The dollar is shown as a fourth coin.
- Shelf order in `HomeApps.SHELF`: Crunchyroll, YouTube, DGO, Netflix, Prime Video, HBO Max.

**Watch for.** The website FAQ still says "English and Portuguese" in the language question.

## 3. The remote's settings button

**Problem.** In Free My TV mode the gear button does nothing. On the TCL it works in Original mode and dies in Free My
TV mode.

### Diagnosis (what the logs showed)

Captured with `adb shell getevent -l` and `adb logcat` while pressing the button in each mode.

1. The remote sends IR `0x0231`. Linux reports `KEY_ROTATE_LOCK_TOGGLE` (scan code 561); Android maps it to
   **`KEYCODE_NOTIFICATION` (83)**, not `KEYCODE_SETTINGS`.
2. `PhoneWindowManager` consumes the key. SystemUI's `TvNotificationPanel` then starts
   `android.app.action.TOGGLE_NOTIFICATION_HANDLER_PANEL` **with `pkg=com.google.android.apps.tv.launcherx`**, aimed at
   `launcherx/.dashboard.DashboardHandler`.
3. Original mode: that opens Google's dashboard. Free My TV mode: launcherx is disabled with `pm disable-user`, and the
   log says `TvNotificationPanel: Not launching notification handler activity: Could not resolve activityInfo`.
4. `com.tcl.globalkeyoverlay` and `com.tcl.dashboard` (in the protected list) are not installed on this model, so TCL
   isn't involved in this button.

### What did not work

`2e54936` registered a `PanelActivity` for `TOGGLE_NOTIFICATION_HANDLER_PANEL`. Reverted in `7616324`: the intent is
package-bound to launcherx, so no other app can receive it, and an app can't see the key either (the system consumes it
before delivery). Keeping launcherx enabled would work but leaves it running in the background.

### What worked: an accessibility service

An `AccessibilityService` with `flagRequestFilterKeyEvents` sees key events **before** the system policy handles them.

- `KeyService` (`0ccbb0a`) overrides `onKeyEvent`, returns `true` for `KEYCODE_NOTIFICATION` (so SystemUI never
  starts the missing launcherx panel) and opens the panel on key-up. It reads no window content
  (`canRetrieveWindowContent=false`) and ignores every other key.
- `res/xml/key_service.xml` declares it; `AndroidManifest.xml` registers it with `BIND_ACCESSIBILITY_SERVICE`.
- Proven first as a prototype that only logged the key, then wired up.

### The panel

`PanelActivity` (`9d55c6b`, restyled in `ce5228f`). The stock panel's buttons were found by pressing each one in
Original mode and reading `ActivityTaskManager: START u0` in the log:

| Entry | Intent |
|---|---|
| Picture | `android.settings.DISPLAY_SETTINGS`, component `com.android.tv.settings/.partnercustomizer.picture.PictureActivity` |
| Sound | `com.android.tv.action.VIEW_SOUND_SETTINGS`, component `com.android.tv.settings/.device.sound.SoundActivity` |
| Wi-Fi | `Settings.ACTION_WIFI_SETTINGS` |
| Bluetooth | `Settings.ACTION_BLUETOOTH_SETTINGS` |
| All settings | `Settings.ACTION_SETTINGS` |
| Back to Original mode | `HomeMode.request(ORIGINAL)` and the Free My TV settings screen |

If the TV refuses one of them (`RuntimeException` on `startActivity`) the panel opens the main settings and writes
`panel: ... refused` to `adb logcat -s FreeMyTV`. On the TCL 32S5K all entries opened their own page.

Look: full-height panel on the right with a soft scrim, Inter fonts from `Ui`, and the white focused row of
`SettingsActivity`. Sizes scale with `heightPixels / 1080`. Theme `FreeMyTV.Panel` is translucent with no window
animation.

### Turning it on and off (`f539a8e`)

`HomeMode.keyService(adb, on)` edits `settings secure enabled_accessibility_services`:

- keeps the services already enabled, adds or removes only `io.freemytv/io.freemytv.KeyService`;
- on, it also sets `accessibility_enabled 1`; off with nothing left, it deletes the list and sets it to `0`;
- called from `applyFree` (on), `applyOriginal` (off), `revertHomeNow` (off, the watchdog gives the stock home back, so
  Google's panel must answer again) and `verifyNow` (on, unless the home was reverted).

### Gotchas

- **`adb install -r` turns the service off.** `verifyNow` runs on `MY_PACKAGE_REPLACED` and turns it back on within a
  minute. While testing by hand, check `adb shell settings get secure enabled_accessibility_services`.
- The service is enabled through the app's own ADB (shell uid), so no Accessibility prompt appears for the user.
- Debug: `adb logcat -s FreeMyTV`, and `adb shell dumpsys accessibility | grep -i KeyService`.
- Other TVs may send a different key or route the panel elsewhere: repeat the diagnosis (`getevent -l`, logcat for
  `START u0`) before assuming `KEYCODE_NOTIFICATION`.

---

## Building on Windows

No SDK on Windows here. The build runs in WSL (Ubuntu) with the SDK in `~/sdk`, a JDK in `~/jdk` and `zip` in `~/bin`:

```bash
ANDROID_HOME=$HOME/sdk JAVA_HOME=$HOME/jdk PATH=$HOME/bin:$HOME/jdk/bin:/usr/bin:/bin ./build.sh
```

`freemytv.keystore` must be the same as the one used for the installed APK, or updates are refused.

## Starting version 2

When forking from this base, keep the pieces above that are not visual: `HomeMode.keyService` with `KeyService`, the
panel's intents, `screensaver=system` and the `Ui.tr` three-language convention. `PanelActivity` and `HomeView` are where
the look lives.
