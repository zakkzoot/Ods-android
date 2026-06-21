# ODS Connections Dashboard

A full-screen Android **home-screen widget** plus a **companion app** that surface the
live status of every Outlined Design Solutions connection at a glance — socials,
systems and accounts — styled to look exactly like
[outlined-design.com](https://www.outlined-design.com).

Dark-industrial, operator-grade: charcoal void, graphite floating tiles, a single
crimson accent, Audiowide numerals and mono uppercase labels.

---

## What it shows

**SOCIALS** — direct access, with notification bubbles
- Facebook · Instagram · LinkedIn

**SYSTEMS** — uptime / health, with status dots (green up · amber degraded · crimson down · grey unknown)
- outlined-design.com · Tasjeel · Vercel · ODS Supabase · ods-recon · ods-link · ods-assist · ods-aether

**ACCOUNTS** — unread / activity counts
- GitHub @zakkzoot · info@outlined-design.com · zakkgray1@gmail.com

Each connection is a small logo tile showing its notification bubble + status dot.
**First tap** expands an inline popup with the detailed figures; **tap again** opens the
site/link. (The widget tiles deep-link into the app, where the two-stage interaction
lives — Android widgets can't hold transient popup state in place.)

---

## Architecture

| Layer | Where |
| --- | --- |
| Connection registry (single source of truth) | `model/Connection.kt` |
| Per-type status checks (HTTP/Vercel/Supabase/GitHub/Gmail/Meta) | `data/StatusProvider.kt` |
| Concurrent refresh + persistence | `data/ConnectionRepository.kt`, `data/StatusStore.kt` |
| Encrypted token store | `data/SecureConfig.kt` |
| Background refresh (~15 min) + widget nudge | `work/StatusRefreshWorker.kt` |
| Full-screen Compose dashboard | `ui/DashboardScreen.kt`, `ui/Tiles.kt` |
| Glance home-screen widget (resizable to a full page) | `widget/OdsWidget.kt` |
| Brand theme (palette, fonts, elevation) | `ui/theme/` |

Status is cached in DataStore so both surfaces render instantly and never block on
the network.

---

## Build

```bash
cd dashboard
./gradlew assembleDebug        # wrapper jar is included
```
Or just open the `dashboard/` folder in Android Studio (Koala+).
Min SDK 26, target/compile SDK 35. Kotlin 2.0, Compose, Glance 1.1.

---

## Configure (no secrets in source)

Tokens are entered in-app via the **Settings screen** (gear icon, top-right of the
dashboard) and stored encrypted (`SecureConfig`, EncryptedSharedPreferences). Saving
triggers an immediate refresh. With nothing configured, a connection still renders and
deep-links — it just shows an "unknown" dot.

**Works with zero setup:** all HTTP tiles (outlined-design.com, ods-recon, ods-link,
Tasjeel) and **ODS Supabase** (keyless `/auth/v1/health`, URL prefilled to the
Link-Core project).

**Needs a token (paste in Settings):**

| Source | What it lights up |
| --- | --- |
| Vercel API token | latest-deployment state |
| GitHub PAT | unread-notification badge |
| Meta page token | Facebook / Instagram activity |
| Google OAuth client ID + per-account connect | Gmail unread counts — see [SETUP_GOOGLE_OAUTH.md](SETUP_GOOGLE_OAUTH.md) |

> LinkedIn has no consumer notification API, so it is deep-link only.

## Remaining placeholders
`ods-assist` and `ods-aether` have no Vercel project yet — their URLs in
`model/Connection.kt` are guesses and will read "down" until those are deployed.
Confirm the Tasjeel URL and the social page handles there too.

---

## Install the widget
Long-press the home screen → Widgets → **ODS Connections** → drag on and resize up to
a full home page.

---

## Publish to GitHub
This project lives in the `ods-android` monorepo — push from the repo root with the
top-level `../publish.sh` (requires an authenticated `gh` CLI).

---

## Design system
Locked to the site (see `ui/theme/Color.kt` / `Type.kt`):
charcoal `#0E0E10` · graphite `#1A1A1E` · silver `#C9CDD4` · crimson `#D42B2B` ·
Audiowide / Share Tech Mono / Sora. Crimson is the only accent — used with restraint.

Fonts are **bundled** as OFL TrueType files in `res/font/` (Audiowide, Share Tech Mono,
and the Sora variable font), so the app renders identically everywhere with no runtime
download or Google Play Services dependency. (The home-screen widget uses the system
font — Glance can't embed custom fonts.)
