# Fog Alarm

Android app that watches the weather and wakes you up before fog arrives. No account needed. Runs in background indefinitely.

## Download & Install

1. Go to [Releases](https://github.com/edleeman17/fog-alarm/releases/latest)
2. Download `fog-alarm.apk`
3. On your phone: **Settings → Apps → Special app access → Install unknown apps**  
   Find your browser (or Files app), enable "Allow from this source"
4. Tap the downloaded APK → Install

> Re-download from the same link whenever a new build is pushed.

## First Launch

Open the app. You'll see one screen:

```
Fog Alarm
Wakes you up before fog arrives

[Enable Monitoring]  ○──●

Wake up before fog by:
[ 60 min ▾ ]

Check weather every:
[ 60 min ▾ ]

Not running

[ Test Alarm Now ]
```

**Toggle "Enable Monitoring" on.** Android will fire up to 4 permission prompts in sequence — approve all of them:

| Prompt | Why |
|--------|-----|
| Location (precise) | Needed to know where to check weather |
| Background location | Allows checks when app is closed |
| Notifications | Delivers the alarm |
| Battery optimisation | Keeps background checks running reliably on Samsung/Xiaomi/Huawei |

On Android 12+ you may also be redirected to **Alarms & reminders** in Settings — enable it for Fog Alarm.

Once done, the status area updates:

```
Last check: 14:32 02/05
No fog in next 3 hours
```

## Settings

| Setting | Options | What it does |
|---------|---------|--------------|
| Wake up before fog by | 30 / 60 / 90 / 120 min | Lead time before fog arrives that the alarm fires |
| Check weather every | 30 / 60 / 120 min | How often to poll Open-Meteo in the background |

**Example:** fog expected at 08:00, lead time = 60 min → alarm fires at 07:00.

## Status Area

Shows current state after each background check:

- `No fog in next 3 hours` — clear, nothing scheduled
- `Fog expected at 08:00` — fog detected, alarm being set
- `Alarm set: 07:00` — exact alarm scheduled, will fire even if phone is in Doze

## Test Alarm

Tap **Test Alarm Now** to fire a test alarm immediately. Verifies:
- Notification appears on lock screen
- Alarm sound plays
- Vibration works

If nothing happens, check notification permissions for Fog Alarm in Android Settings.

## How It Works

1. WorkManager wakes the app every N minutes (your chosen interval)
2. Gets your current GPS location
3. Calls [Open-Meteo](https://open-meteo.com) — free, no account
4. Checks the next 3 hourly forecasts for fog weather codes (45, 48) or visibility < 1000m
5. If fog found: schedules an exact `AlarmManager` alarm at `fog_time − lead_time`
6. Alarm fires via full-screen notification + alarm sound + vibration
7. On reboot: WorkManager automatically re-registers, no action needed

## Fog Detection

Open-Meteo weather codes that trigger the alarm:

| Code | Condition |
|------|-----------|
| 45 | Fog |
| 48 | Depositing rime fog |

Also triggers if forecast visibility drops below **1000 metres**.

## Building from Source

Push to `main` branch → GitHub Actions builds the APK → appears in [Releases](https://github.com/edleeman17/fog-alarm/releases/latest) within ~3 minutes.

```bash
git clone https://github.com/edleeman17/fog-alarm
cd fog-alarm
# make changes
git push origin main
```
