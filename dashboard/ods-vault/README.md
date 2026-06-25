# ODS Vault — knowledge base (read + edit)

Surfaces and edits the ODS knowledge base via secure Supabase edge functions.
Supabase project: `kfbtpcwakpyptwqqhabu` (https://kfbtpcwakpyptwqqhabu.supabase.co).

## Functions
- **`functions/kb`** — READ (verify_jwt=true, anon key). `GET /functions/v1/kb`
  returns `{ categories[], entries[], clients[], total, generated_at }`. Serves
  non-archived `knowledge_base` rows via the service role; `?include_archived=1`
  includes archived; `?limit=`. CORS enabled for the web viewer.
- **`functions/kb-write`** — WRITE (verify_jwt=true + `x-admin-token`).
  `POST /functions/v1/kb-write` with body `{ action, ... }`:
  - `create` — `{ action:"create", client_id, title, question?, answer?, content?, category?, language?, status?, source? }`
  - `update` — `{ action:"update", id, ...fields }`
  - `archive` — `{ action:"archive", id }`
  - `restore` — `{ action:"restore", id }`
  Writable columns are whitelisted; everything else in the body is ignored.

## Is the KB easily updatable? Yes — three ways
1. **In-app editing** (chosen path) — the viewer calls `kb-write` with the admin
   token. Add / edit / archive / restore entries from inside The Program.
   Changes show up immediately on the next read (no caching).
2. **Supabase dashboard** — Table Editor → `knowledge_base`. No code, instant.
3. **Whatever already feeds it** (the `source` column shows origin), and, once
   wired, a life-vault → Supabase sync for git-based editing.

## Auth model
- **Read**: anon key (publishable; on-device only — never committed, since the KB
  holds client content). The table stays RLS-locked; only the service-role
  function reads it.
- **Write**: an **admin token** in addition to the anon key. The token's SHA-256
  is stored in `public.kb_admin` (service-role only); the raw token lives only on
  the operator's device (editor settings) and in your password manager. Rotate by
  inserting a new hash and deleting the old row. Never commit the raw token.

## Deploy
Functions are deployed via the Supabase MCP `deploy_edge_function` (or
`supabase functions deploy kb kb-write`). `kb` is live; `kb-write` deploy may
require an approval click. Migration `kb_admin_tokens` creates the token table.
