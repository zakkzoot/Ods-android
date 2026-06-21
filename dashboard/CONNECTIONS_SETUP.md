# ODS Dashboard — connections, links & APIs

Every connection still **deep-links** and renders without any token; a token only lights
up its live status / notification count. Secrets are stored encrypted on-device. You can
type them in **Settings**, or drop them all in at once via **Settings → Import .env file**
(see [`.env.example`](.env.example)).

## What each connection needs

### SOCIALS
| Connection | Opens | Live status needs | API used |
| --- | --- | --- | --- |
| Facebook | facebook.com/outlineddesignsolutions | `META_PAGE_TOKEN` | Meta Graph API (page) |
| Instagram | instagram.com/outlineddesignsolutions | `META_PAGE_TOKEN` | Meta Graph API (page) |
| LinkedIn | linkedin.com/company/outlineddesignsolutions | — (link only) | none |
| Telegram Channel | t.me/OutlinedDesignSolutions | `TELEGRAM_BOT_TOKEN` (bot must be a channel admin) | Telegram Bot API `getChatMemberCount` |

### SERVICES
| Connection | Opens | Live status needs | API used |
| --- | --- | --- | --- |
| GitHub @zakkzoot | github.com/zakkzoot | `GITHUB_PAT` (notifications scope) | `api.github.com/notifications` |
| Vercel | vercel.com/dashboard | `VERCEL_TOKEN` | `api.vercel.com/v6/deployments` |
| ODS Supabase | Supabase dashboard | nothing (keyless health); `SUPABASE_ANON_KEY` optional | `{SUPABASE_URL}/auth/v1/health` |
| Tasjeel | tasjeel.app *(set real URL)* | — (HTTP uptime) | HEAD request |

### SITES (HTTP uptime — no tokens)
| Connection | URL |
| --- | --- |
| outlined-design.com | https://www.outlined-design.com |
| ods-recon | https://ods-recon.com |
| ods-link | https://ods-link-core.vercel.app |
| ods-aether | *set when deployed* |
| ods-assist | *set when deployed* |

### MESSAGES
| Connection | Opens | Live status needs | Notes |
| --- | --- | --- | --- |
| Telegram | web.telegram.org | `TELEGRAM_BOT_TOKEN` | Bot health only — a bot can't read your personal DMs |
| zakkgray1@gmail.com | mailto | `GOOGLE_CLIENT_ID` + in-app **Connect** (OAuth) | unread inbox count — see [`SETUP_GOOGLE_OAUTH.md`](SETUP_GOOGLE_OAUTH.md) |
| info@outlined-design.com | cPanel webmail (`https://s1308.sgp1.mysecurecloudhost.com:2096`) | `IMAP_PASSWORD` (host/user prefilled) | live unread via IMAP over SSL (host `s1308.sgp1.mysecurecloudhost.com`, port 993) |

## All keys (for the .env import)

| Key | Used by | Where to get it |
| --- | --- | --- |
| `VERCEL_TOKEN` | Vercel | vercel.com → Account Settings → Tokens |
| `GITHUB_PAT` | GitHub | github.com → Settings → Developer settings → PAT (notifications scope) |
| `SUPABASE_URL` | ODS Supabase | Supabase project URL (defaults to the Link-Core project) |
| `SUPABASE_ANON_KEY` | ODS Supabase | Supabase → Project Settings → API (optional) |
| `META_PAGE_TOKEN` | Facebook, Instagram | Meta for Developers → Graph API → page access token |
| `TELEGRAM_BOT_TOKEN` | Telegram (both) | @BotFather → /newbot → token |
| `GOOGLE_CLIENT_ID` | Gmail | Google Cloud OAuth client — see `SETUP_GOOGLE_OAUTH.md` |
| `IMAP_HOST` / `IMAP_USER` / `IMAP_PASSWORD` | info@outlined-design.com | cPanel mailbox (host/user prefilled; password from cPanel → Email Accounts) |

> Gmail needs one extra step the env file can't do: after importing `GOOGLE_CLIENT_ID`,
> tap **Connect** next to the Gmail account to complete the Google consent in the browser.
