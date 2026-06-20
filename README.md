# ods-android

Android surfaces for **Outlined Design Solutions** — both styled to match
[outlined-design.com](https://www.outlined-design.com) (charcoal void, graphite,
silver, a single crimson accent).

This is a monorepo of two independent Android Studio projects:

| Folder | What it is |
| --- | --- |
| [`dashboard/`](dashboard) | **ODS Connections Dashboard** — a full-screen home widget + companion app showing live status of every ODS connection (socials, systems, accounts), with notification bubbles and a two-stage tap (status popup → open). |
| [`spotlight-wallpaper/`](spotlight-wallpaper) | **ODS Spotlight** — a live wallpaper for the Galaxy S23 Ultra lock + home screen: a touch-following spotlight reveals the crimson intelligence layer beneath a blurred backdrop, reusing the site's hero mechanic. |

Open each folder as its own project in Android Studio. Each has its own README.

## Build (either project)
```bash
cd dashboard            # or: cd spotlight-wallpaper
./gradlew assembleDebug # gradle wrapper jar is included
```
Or open the folder directly in Android Studio (Koala+). For Gmail unread counts in the
dashboard, follow [`dashboard/SETUP_GOOGLE_OAUTH.md`](dashboard/SETUP_GOOGLE_OAUTH.md).

## The spotlight wallpaper
- Reproduces `lib/spotlight.tsx`: the dark base is shown blurred (the website-menu
  look); a soft radial spotlight follows your finger and reveals the sharp crimson
  layer beneath. It drifts slowly when untouched so the lock screen stays alive.
- Ships the real site hero art (`hero-base` / `hero-reveal`) as the two layers.
- Install: run the app → **Set wallpaper** → apply to lock + home screen. Tunables
  (spotlight radius, blur strength) live in `Prefs.kt`.
- Note: a true *Galaxy Theme* can't do an interactive touch spotlight — that format
  is static. A live wallpaper is the correct, supported way to get this on Samsung.

## Publish
```bash
./publish.sh                 # pushes to zakkzoot/ods-android (private)
./publish.sh ods-android public
```
Handles the repo whether or not it already exists. Requires an authenticated `gh`.
```
