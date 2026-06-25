import "jsr:@supabase/functions-js/edge-runtime.d.ts";

// ODS Vault — KB WRITE API (admin-token gated). Create / update / archive / restore
// entries in public.knowledge_base. Pairs with the read-only `kb` function.
//
// Auth: caller sends the Supabase anon key (verify_jwt=true) AND an `x-admin-token`
// header. The token is sha256'd and matched against public.kb_admin (service-role only).
// Anyone with just the anon key can READ via `kb` but CANNOT write here.

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const REST = `${SUPABASE_URL}/rest/v1`;

const svc: Record<string, string> = {
  apikey: SERVICE_KEY,
  Authorization: `Bearer ${SERVICE_KEY}`,
  "Content-Type": "application/json",
};
const CORS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-admin-token",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

// Columns an editor is allowed to write. Anything else in the body is ignored.
const ALLOWED = ["title", "question", "answer", "content", "category", "language", "status", "source", "client_id"];

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { ...CORS, "Content-Type": "application/json" } });
}

async function sha256Hex(s: string): Promise<string> {
  const buf = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(s));
  return [...new Uint8Array(buf)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

function pick(body: Record<string, unknown>): Record<string, unknown> {
  const o: Record<string, unknown> = {};
  for (const k of ALLOWED) if (k in body && body[k] !== undefined) o[k] = body[k];
  return o;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: CORS });
  if (req.method !== "POST") return json({ error: "POST only" }, 405);

  const token = req.headers.get("x-admin-token") ?? "";
  if (!token) return json({ error: "missing admin token" }, 401);
  const hash = await sha256Hex(token);
  const a = await fetch(`${REST}/kb_admin?select=id&token_hash=eq.${hash}`, { headers: svc });
  const admins: any[] = a.ok ? await a.json() : [];
  if (!admins.length) return json({ error: "invalid admin token" }, 401);
  fetch(`${REST}/kb_admin?id=eq.${admins[0].id}`, { method: "PATCH", headers: svc, body: JSON.stringify({ last_used_at: new Date().toISOString() }) }).catch(() => {});

  const body = (await req.json().catch(() => ({}))) as Record<string, any>;
  const action = body.action;
  const id = body.id;

  try {
    if (action === "create") {
      const row = pick(body);
      if (!row.title) return json({ error: "title required" }, 400);
      if (!row.client_id) return json({ error: "client_id required" }, 400);
      const r = await fetch(`${REST}/knowledge_base`, { method: "POST", headers: { ...svc, Prefer: "return=representation" }, body: JSON.stringify(row) });
      return json(await r.json(), r.ok ? 200 : 400);
    }
    if (action === "update") {
      if (!id) return json({ error: "id required" }, 400);
      const row = pick(body);
      row.updated_at = new Date().toISOString();
      const r = await fetch(`${REST}/knowledge_base?id=eq.${id}`, { method: "PATCH", headers: { ...svc, Prefer: "return=representation" }, body: JSON.stringify(row) });
      return json(await r.json(), r.ok ? 200 : 400);
    }
    if (action === "archive") {
      if (!id) return json({ error: "id required" }, 400);
      const r = await fetch(`${REST}/knowledge_base?id=eq.${id}`, { method: "PATCH", headers: { ...svc, Prefer: "return=representation" }, body: JSON.stringify({ archived_at: new Date().toISOString(), status: "archived" }) });
      return json(await r.json(), r.ok ? 200 : 400);
    }
    if (action === "restore") {
      if (!id) return json({ error: "id required" }, 400);
      const r = await fetch(`${REST}/knowledge_base?id=eq.${id}`, { method: "PATCH", headers: { ...svc, Prefer: "return=representation" }, body: JSON.stringify({ archived_at: null, status: "active" }) });
      return json(await r.json(), r.ok ? 200 : 400);
    }
    return json({ error: "unknown action (use create|update|archive|restore)" }, 400);
  } catch (e) {
    return json({ error: e instanceof Error ? e.message : String(e) }, 500);
  }
});
