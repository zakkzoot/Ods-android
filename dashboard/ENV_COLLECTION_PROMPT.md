# ODS Dashboard — env collection

Two parts: (1) a **copy-paste prompt** to run in another session that has your repos +
service integrations connected, so an agent gathers every value for you; and (2) the
**reference list** of exactly what each key is and where it lives.

The dashboard imports the result via **Settings → Import .env file** (only the keys below
are read; see `.env.example`).

---

## 1. Paste this prompt into a new session (with repos/integrations connected)

> You have access to my connected repositories and service integrations (GitHub, Vercel,
> Supabase, and my repos including `zakkzoot/ods-android`, `ods-recon`, `ods-link`, and the
> outlined-design.com website). Help me assemble a complete `.env` for the ODS Connections
> Dashboard.
>
> Produce a single fenced `ods.env` file containing exactly these keys, filled with real
> values where you can retrieve or derive them, and left blank (with an inline `# how to
> get it` comment) where they require a secret only I can mint:
>
> ```
> VERCEL_TOKEN=
> GITHUB_PAT=
> SUPABASE_URL=
> SUPABASE_ANON_KEY=
> META_PAGE_TOKEN=
> TELEGRAM_BOT_TOKEN=
> GOOGLE_CLIENT_ID=
> IMAP_HOST=s1308.sgp1.mysecurecloudhost.com
> IMAP_USER=info@outlined-design.com
> IMAP_PASSWORD=
> ```
>
> For each key, do the following and report it in a short table (key · value-or-status ·
> source · action-needed):
> - **SUPABASE_URL / SUPABASE_ANON_KEY** — read from the ODS Supabase project (use the
>   Supabase integration: get the project URL and the anon/publishable key). Confirm which
>   project is the live one (Link-Core ref `kfbtpcwakpyptwqqhabu` vs Recon).
> - **VERCEL_TOKEN** — you can't read an existing token; give me the exact click-path to
>   mint one (Vercel → Account Settings → Tokens) and the minimum scope.
> - **GITHUB_PAT** — same: the click-path to create a fine-grained PAT with the
>   `notifications` read scope for `zakkzoot`.
> - **META_PAGE_TOKEN** — the steps to generate a long-lived Page access token for the
>   Facebook/Instagram pages `outlineddesignsolutions`.
> - **TELEGRAM_BOT_TOKEN** — the @BotFather steps; also note the bot must be added as an
>   admin of the channel `@OutlinedDesignSolutions` for member counts.
> - **GOOGLE_CLIENT_ID** — confirm against `dashboard/SETUP_GOOGLE_OAUTH.md` in
>   `ods-android`; output the client ID if one already exists in my Google Cloud project,
>   else the steps.
> - **IMAP_PASSWORD** — the cPanel path to the mailbox password for
>   `info@outlined-design.com` (Email Accounts → Manage). Don't guess it.
>
> Then ALSO verify these non-secret placeholders in `ods-android`'s
> `dashboard/app/src/main/java/com/ods/dashboard/model/Connection.kt` and tell me the
> correct values (check the actual deployments in Vercel / the repos):
> - `tasjeel` URL (currently `https://tasjeel.app` — placeholder)
> - `aether` URL (`ods-aether` — set the real prod URL once deployed)
> - `assist` URL (`ods-assist` — set the real prod URL once deployed)
> - `link` URL (currently the Vercel app URL — confirm the canonical prod domain)
> - the Facebook / Instagram / LinkedIn page handles
> - the Supabase dashboard project ref used in the `supabase` tile URL
>
> Output: (a) the filled `ods.env` block, (b) the status table, (c) a short list of the
> Connection.kt URL/handle corrections (old → new) so I can apply them.

---

## 2. Reference — what each key is

| Key | Required for | What it is / where to get it | Secret? |
| --- | --- | --- | --- |
| `VERCEL_TOKEN` | Vercel tile (deploy state) | Vercel → Account Settings → Tokens → Create | yes |
| `GITHUB_PAT` | GitHub tile (unread notifications + headlines) | github.com → Settings → Developer settings → Personal access token, `notifications` read scope | yes |
| `SUPABASE_URL` | ODS Supabase tile | Project URL `https://<ref>.supabase.co` (defaults to Link-Core `kfbtpcwakpyptwqqhabu`) | no |
| `SUPABASE_ANON_KEY` | optional (health works without it) | Supabase → Project Settings → API → anon/publishable key | yes-ish |
| `META_PAGE_TOKEN` | Facebook + Instagram tiles | Meta for Developers → Graph API → long-lived Page access token | yes |
| `TELEGRAM_BOT_TOKEN` | Telegram tiles (channel members + bot health) | @BotFather → `/newbot` → token; add bot as admin of `@OutlinedDesignSolutions` | yes |
| `GOOGLE_CLIENT_ID` | Gmail tile (unread) | Google Cloud OAuth client — see `SETUP_GOOGLE_OAUTH.md`; still tap **Connect** in-app after import | yes |
| `IMAP_HOST` | info@ tile | `s1308.sgp1.mysecurecloudhost.com` (prefilled) | no |
| `IMAP_USER` | info@ tile | `info@outlined-design.com` (prefilled) | no |
| `IMAP_PASSWORD` | info@ tile (unread) | the mailbox password from cPanel → Email Accounts | yes |

### No token needed (HTTP uptime only)
`outlined-design.com`, `ods-recon`, `ods-link`, `ods-aether`, `ods-assist`, `tasjeel` —
these just need their **correct URLs** in `Connection.kt` (LinkedIn is link-only too).

> After importing, finish two things the file can't do: tap **Connect** for Gmail (OAuth
> consent), and **Save and refresh** in Settings.
