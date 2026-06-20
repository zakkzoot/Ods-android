# Installing the ODS Android apps

There are two apps in this repo. Neither is on the Play Store, so you install
them by **sideloading** the APK that GitHub Actions builds for you. No Android
Studio or computer setup required — just your phone.

| App | Package | What it does |
| --- | --- | --- |
| ODS Spotlight | `com.ods.wallpaper` | Live wallpaper — touch-following crimson spotlight reveal. |
| ODS Dashboard | `com.ods.dashboard` | Connections dashboard app + home-screen widget. |

## 1. Get the APKs

Every push to `main` builds both APKs. To grab them:

1. Open the repo's **Actions** tab → the latest **Build APKs** run
   (<https://github.com/zakkzoot/Ods-android/actions>).
2. Scroll to **Artifacts** at the bottom and download:
   - `spotlight-wallpaper-debug`
   - `dashboard-debug`
3. Each download is a `.zip` containing `app-debug.apk`.

> Tip: you can download directly on the phone from the GitHub mobile site, or
> download on a computer and move the files over. To rebuild any time without a
> code change, use **Actions → Build APKs → Run workflow**.

Unzip each one — you want the `app-debug.apk` inside.

## 2. Allow installing from unknown sources

On the Galaxy S23 Ultra (One UI):

1. Open the `.apk` (from **My Files** or your browser's downloads).
2. Android will warn that this source isn't allowed → tap **Settings**.
3. Toggle **Allow from this source** for the app you're installing from
   (e.g. My Files or your browser).
4. Go back and tap **Install**.

These are **debug** builds, signed with the standard Android debug key. That's
fine for installing on your own device; Play Protect may show a one-time
"unverified app" prompt — choose **Install anyway**.

## 3. Set up each app

### ODS Spotlight (wallpaper)
1. Open the **ODS Spotlight** app.
2. Tap **Set wallpaper** → apply to **Lock and Home screens**.
3. Drag your finger to move the spotlight; it drifts on its own when idle.
   Tunables (spotlight radius, blur) live in `spotlight-wallpaper/.../Prefs.kt`.

### ODS Dashboard (app + widget)
1. Open the **ODS Dashboard** app once so it can initialize.
2. Long-press the home screen → **Widgets** → **ODS** → drag the dashboard
   widget out and resize it.
3. **Gmail unread counts** need a Google OAuth client ID — follow
   [`dashboard/SETUP_GOOGLE_OAUTH.md`](dashboard/SETUP_GOOGLE_OAUTH.md). The app
   runs without it; that step only enables the Gmail tile.

## Updating later
Re-run the workflow (or push a change), download the new artifact, and install
over the top — same package name, so it upgrades in place and keeps your data.
