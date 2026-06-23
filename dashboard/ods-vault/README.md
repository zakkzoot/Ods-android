# ODS Vault — knowledge base

Surfaces the ODS knowledge base in the dashboard app via a secure Supabase proxy.

- **`functions/kb/index.ts`** — edge function (deployed, ACTIVE). Serves the RLS-locked
  `public.knowledge_base` table via the service role: returns non-archived entries
  (`title, question, answer, content, category, language, status, updated_at`) plus a
  per-category count. `verify_jwt=true` — the app calls it with the Supabase anon key.
- **App**: `data/Kb.kt` (`KbClient`) reads `{SUPABASE_URL}/functions/v1/kb`; the
  **Knowledge Base** screen (`ui/VaultScreen.kt`, the book icon on the home header) shows
  entries with search + category filter, each expandable to question / answer / content.

## Security
The KB contains client content, so the anon key is **never committed** and the app holds
it on-device only (Settings → Supabase anon key, or `SUPABASE_ANON_KEY` in an imported
`.env`). The table stays RLS-locked; only the service-role edge function can read it.

## life-vault (pending)
When the `zakkzoot/life-vault` repo is granted to the session, the plan is a second edge
function that **syncs** life-vault's content into Supabase (server-side GitHub token, never
in the app), and the app UI is shaped to match life-vault's layout. This read endpoint +
screen are the foundation that sync will feed.
