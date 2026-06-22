import "jsr:@supabase/functions-js/edge-runtime.d.ts";

// ODS Pulse v1 — server-side uptime/latency for the ODS web properties.
// Reads/writes via the service role (bypasses RLS). The Android app calls this with the
// project anon key (verify_jwt=true). Probes run lazily on read, throttled to FRESH_MS,
// so history accumulates whenever the app refreshes.
//
// Deploy: supabase functions deploy pulse  (or the MCP deploy_edge_function tool).

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const REST = `${SUPABASE_URL}/rest/v1`;
const FRESH_MS = 120_000;

const svc: Record<string, string> = {
  apikey: SERVICE_KEY,
  Authorization: `Bearer ${SERVICE_KEY}`,
  "Content-Type": "application/json",
};

type Target = { id: string; label: string; url: string };

async function getTargets(): Promise<Target[]> {
  const r = await fetch(`${REST}/pulse_targets?select=id,label,url&enabled=eq.true`, { headers: svc });
  return r.ok ? await r.json() : [];
}

async function newestCheckMs(): Promise<number> {
  const r = await fetch(`${REST}/pulse_checks?select=checked_at&order=checked_at.desc&limit=1`, { headers: svc });
  if (!r.ok) return 0;
  const rows = await r.json();
  return rows.length ? new Date(rows[0].checked_at).getTime() : 0;
}

async function probeOne(t: Target) {
  const start = Date.now();
  let status = 0;
  let ok = false;
  let error: string | null = null;
  try {
    const ctrl = new AbortController();
    const to = setTimeout(() => ctrl.abort(), 8000);
    const resp = await fetch(t.url, {
      method: "GET",
      redirect: "follow",
      signal: ctrl.signal,
      headers: { "User-Agent": "ODS-Pulse" },
    });
    clearTimeout(to);
    status = resp.status;
    ok = status >= 200 && status < 400;
    try { await resp.body?.cancel(); } catch (_) { /* ignore */ }
  } catch (e) {
    error = (e instanceof Error ? e.message : String(e)).slice(0, 300);
  }
  return {
    target_id: t.id,
    url: t.url,
    status_code: status,
    ok,
    ttfb_ms: Date.now() - start,
    error,
    checked_at: new Date().toISOString(),
  };
}

async function probeAll(): Promise<number> {
  const ts = await getTargets();
  const rows = await Promise.all(ts.map(probeOne));
  if (rows.length) {
    await fetch(`${REST}/pulse_checks`, {
      method: "POST",
      headers: { ...svc, Prefer: "return=minimal" },
      body: JSON.stringify(rows),
    });
  }
  return rows.length;
}

async function summary() {
  const ts = await getTargets();
  const since = new Date(Date.now() - 24 * 3600 * 1000).toISOString();
  const r = await fetch(
    `${REST}/pulse_checks?select=target_id,checked_at,status_code,ok,ttfb_ms&checked_at=gte.${since}&order=checked_at.desc`,
    { headers: svc },
  );
  const checks: any[] = r.ok ? await r.json() : [];
  const byTarget = new Map<string, any[]>();
  for (const c of checks) {
    const a = byTarget.get(c.target_id) ?? [];
    a.push(c);
    byTarget.set(c.target_id, a);
  }
  return ts.map((t) => {
    const cs = byTarget.get(t.id) ?? [];
    const latest = cs[0];
    const total = cs.length;
    const up = cs.filter((c) => c.ok).length;
    return {
      id: t.id,
      label: t.label,
      url: t.url,
      ok: latest ? latest.ok : null,
      status: latest ? latest.status_code : null,
      ttfb_ms: latest ? latest.ttfb_ms : null,
      uptime_24h: total ? Math.round((100 * up) / total) : null,
      checks_24h: total,
      last_checked: latest ? latest.checked_at : null,
    };
  });
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

Deno.serve(async (req: Request) => {
  const action = new URL(req.url).searchParams.get("action") ?? "read";
  try {
    if (action === "probe") {
      return json({ probed: await probeAll() });
    }
    if (Date.now() - (await newestCheckMs()) > FRESH_MS) {
      await probeAll();
    }
    return json({ sites: await summary(), generated_at: new Date().toISOString() });
  } catch (e) {
    return json({ error: e instanceof Error ? e.message : String(e) }, 500);
  }
});
