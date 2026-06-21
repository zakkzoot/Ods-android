# ODS Dashboard — what each item can actually show

This is the reality check: for every connection, what is **physically possible** to pull,
split into three tiers, plus a recommendation for the one thing that unlocks real site
telemetry (visitors / load time / errors / reporting): a small **ODS Pulse** service.

**Tiers**
- **T0 — client-only** (no token, the app just makes a request): up/down, HTTP status,
  latency/TTFB, SSL-cert expiry, redirect chain.
- **T1 — provider API** (paste a token): whatever that vendor's API exposes.
- **T2 — instrumented** (needs code in the site or a backend collecting data): real-user
  load time, visitors, error rates, custom events.

---

## Your web properties (Sites): outlined-design.com, ods-recon, ods-link, ods-aether, ods-assist

| Want | Tier | How |
| --- | --- | --- |
| Up / down, status code | T0 | already done (HTTP HEAD) |
| Response latency (server TTFB) | T0 | already done |
| **SSL certificate expiry** | T0 | read the TLS cert on connect — cheap, very useful |
| Last deploy state / build status | T1 | Vercel API (for Vercel-hosted ones) — already wired for the Vercel tile |
| **Uptime % (24h/7d) + incident history** | T2 | needs something probing on a schedule and storing history → **ODS Pulse** or an uptime SaaS |
| **Real-user load time (LCP/TTFB/CLS)** | T2 | Vercel Speed Insights (no public API — dashboard only) *or* a web-vitals beacon → ODS Pulse |
| **Visitors / pageviews / live now / top pages** | T2 | privacy analytics with an API: Plausible / Umami / PostHog, *or* Vercel Web Analytics (Pro), *or* a beacon → ODS Pulse |
| **JS errors / error rate / latest issues** | T2 | Sentry (has a clean API) *or* a window.onerror beacon → ODS Pulse |

**Verdict:** the client can show health + latency + SSL today. Everything you actually
asked for — visitors, load time, errors, reporting — is T2 and needs a collector. See
**ODS Pulse** below.

## Services

| Connection | Most useful, and how |
| --- | --- |
| **GitHub** | T1 — unread notifications (done) + **headlines** (done); can add: PRs awaiting your review, assigned issues, latest Actions run status per repo, rate-limit. Rich + reliable. |
| **Vercel** | T1 — latest deployment state (done); can add: per-project prod state, last build duration, domains, and (Pro) Web Analytics visitors. |
| **Supabase** | T0 health (done); T1 with a service key (Management API): DB size, active connections, **auth users count**, edge-function invocations, recent logs/errors. |
| **Tasjeel** | T0 uptime today. T1 only if it exposes an API — tell me what Tasjeel is and I'll map it. |

## Socials

| Connection | What's physically possible |
| --- | --- |
| **Facebook** | T1 (Meta Graph + Page token): follower count, **unread Page messages**, new comments, post reach/impressions. Some need app-review permissions. No *personal* notifications. |
| **Instagram** | T1 (IG Graph, linked to the FB Page): followers, media insights, **unread IG Direct** (with messaging perms), comment counts. |
| **LinkedIn** | Realistically link-only. Company-page follower stats need LinkedIn Marketing Developer Platform approval; there's no personal-notification API. |
| **Telegram Channel** | T1 (Bot API, bot = channel admin): **member count** (done), admin list. Per-post **view counts are not in the Bot API** — they need an MTProto userbot (Telethon). |

## Messages

| Connection | What's physically possible |
| --- | --- |
| **Telegram (DMs)** | A **bot cannot read your personal DMs**. Real unread/previews need an MTProto userbot (your own account via Telethon/TDLib) running as a small service — doable, but it's its own backend. |
| **Gmail** | T1 (done): unread count; can add recent **subjects/snippets**, per-label counts. |
| **info@ (IMAP)** | T1 (done): unread (done); can add recent **subjects/from**, total, flagged. |

---

## The missing piece: "ODS Pulse" (recommended)

A small **Supabase Edge Function + table** (keeps to the DESIGN.md rule that all such calls
live in Supabase, not the client). One service gives you the site telemetry you want and
the app reads it in a single call.

**What it does**
1. **Scheduled probe** (cron, every 1–5 min) of each site → store `{ts, url, status, ttfb_ms, ssl_days_left}` in a `pulse_checks` table. → uptime %, latency trend, SSL countdown, incidents.
2. **Beacon endpoint** the sites POST to (tiny snippet on outlined-design.com etc.):
   - `web-vitals` → real-user **LCP / TTFB / CLS** (load time).
   - page views / sessions → **visitors / live now / top pages**.
   - `window.onerror` / unhandled rejections → **JS errors** with message + count.
3. **Read endpoint** `/pulse` → one JSON the dashboard renders: per-site health, 24h
   uptime, p75 load time, today's visitors, error count, last deploy, SSL days left.
4. **Reporting**: a daily/weekly roll-up (e.g. emailed or shown in-app).

**Alternative (no build):** wire existing SaaS instead — **BetterStack/UptimeRobot**
(uptime + alerts + SSL), **Plausible/Umami** (visitors, has API), **Sentry** (errors).
The app reads their APIs. Faster to stand up; monthly cost; data spread across vendors.

**Recommendation:** ODS Pulse on your existing Supabase for uptime/latency/SSL/errors +
a web-vitals beacon (cheap, owned, one endpoint), and optionally Plausible/Umami for
visitors if you don't want to build session analytics. I can build the edge function +
table + the beacon snippet + the app tile.

---

## What's most useful "in one place" (proposed default view)

1. **Site board** (top): each property with a status light, 24h uptime %, p75 load time,
   today's visitors, error count, SSL days-left, last deploy. *(needs ODS Pulse)*
2. **Inbox roll-up**: Gmail + info@ unread with subjects; FB/IG Page unread. *(T1)*
3. **Dev ops**: GitHub notifications + PRs-to-review + CI status; Vercel deploys; Supabase
   health/users. *(T1, mostly done)*
4. **Social pulse**: follower counts + new comments. *(T1)*

---

## Build roadmap (pick the order)

- **A. Bug:** fix the dashboard launch hang *(done this round — hardened startup)*.
- **B. ODS Pulse v1:** Supabase function + table + scheduled probes (uptime/latency/SSL)
  + read endpoint + a richer Sites tile. *(unlocks the headline feature)*
- **C. Beacon:** web-vitals + pageview + error beacon snippet for the sites → load time,
  visitors, errors. *(depends on B)*
- **D. Deeper T1:** GitHub PRs/Actions, Vercel per-project, Supabase users/logs, Gmail/
  IMAP subjects, Meta follower + unread. *(independent, incremental)*
- **E. Customisation:** widget configuration + drag-to-rearrange / resize tiles in the app.
- **F. Telegram userbot** (optional): MTProto service for real DM unread + post views.
