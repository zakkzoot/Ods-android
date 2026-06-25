import "jsr:@supabase/functions-js/edge-runtime.d.ts";

// ODS Vault — knowledge base READ API for the viewer apps.
// Serves public.knowledge_base via the service role (the table is RLS-locked, so clients
// can't read it directly). verify_jwt=true — callers send the Supabase anon key.
// Pairs with `kb-write` (admin-token-gated edits).

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const REST = `${SUPABASE_URL}/rest/v1`;

const svc: Record<string, string> = {
  apikey: SERVICE_KEY,
  Authorization: `Bearer ${SERVICE_KEY}`,
};
const CORS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "GET, OPTIONS",
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { ...CORS, "Content-Type": "application/json" } });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: CORS });
  const q = new URL(req.url).searchParams;
  const limit = Math.min(Number(q.get("limit") ?? "300") || 300, 1000);
  const includeArchived = q.get("include_archived") === "1";
  try {
    const select = "select=id,title,question,answer,content,category,language,status,client_id,updated_at";
    const archiveFilter = includeArchived ? "" : "archived_at=is.null&";
    const r = await fetch(
      `${REST}/knowledge_base?${select}&${archiveFilter}order=updated_at.desc.nullslast&limit=${limit}`,
      { headers: svc },
    );
    const entries: any[] = r.ok ? await r.json() : [];

    const counts = new Map<string, number>();
    for (const e of entries) {
      const c = (e.category && String(e.category).trim()) || "Uncategorised";
      counts.set(c, (counts.get(c) ?? 0) + 1);
    }
    const categories = [...counts.entries()]
      .map(([category, count]) => ({ category, count }))
      .sort((a, b) => a.category.localeCompare(b.category));

    // Clients (for the editor's client picker)
    const cr = await fetch(`${REST}/clients?select=id,name,emoji,color,status&order=name.asc`, { headers: svc });
    const clients: any[] = cr.ok ? await cr.json() : [];

    return json({ categories, entries, clients, total: entries.length, generated_at: new Date().toISOString() });
  } catch (e) {
    return json({ error: e instanceof Error ? e.message : String(e) }, 500);
  }
});
