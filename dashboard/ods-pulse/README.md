# ODS Pulse

A tiny site-telemetry service on the ODS **Link-Core** Supabase project
(`kfbtpcwakpyptwqqhabu`). It gives the dashboard real **uptime %, last status, and load
time (TTFB)** for the ODS web properties, in one call.

This is **v1 — server-side probing only** (uptime / latency / status). Visitor counts,
real-user load time, and JS errors need a site beacon, which is deferred until you pick a
telemetry method.

## Pieces
- **`migrations/0001_pulse_tables.sql`** — `pulse_targets` (the URLs to watch) and
  `pulse_checks` (time-series results). Default-deny RLS. *(Already applied.)*
- **`functions/pulse/index.ts`** — the edge function. On read it returns a per-site
  summary (ok, status, ttfb_ms, uptime_24h, checks_24h); if the newest check is older than
  2 min it probes all targets first (lazy refresh). `?action=probe` forces a probe.

## How the app uses it
The dashboard calls `GET {SUPABASE_URL}/functions/v1/pulse` with the **anon key**
(`apikey` + `Authorization: Bearer`). With the key set (Settings → Supabase anon key, or
`SUPABASE_ANON_KEY` in the imported `.env`), the **Sites** tiles show status + load time +
24h uptime. Without it, they fall back to the device-side HTTP check.

## Deploy / update the function
The table migration is already applied. To deploy or update the function:

```bash
# with the Supabase CLI
supabase functions deploy pulse --project-ref kfbtpcwakpyptwqqhabu
```

or use the MCP `deploy_edge_function` tool, or paste `functions/pulse/index.ts` into the
Supabase dashboard → Edge Functions → pulse. The function relies only on the built-in
`SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` env vars — no secrets to set.

## Keep data fresh when the app is closed (optional)
Lazy-on-read keeps data fresh whenever the app refreshes (~every 15 min). To probe on a
fixed schedule regardless, enable `pg_cron` + `pg_net` and add a job that GETs
`…/functions/pulse?action=probe` every few minutes with the anon key.

## Roadmap
- **Beacon** endpoint for web-vitals (load time), pageviews (visitors), and JS errors —
  once the telemetry method is chosen.
- SSL-expiry (needs a TLS-cert source the edge runtime can read, or a 3rd-party check).
- pg_cron scheduled probes.
