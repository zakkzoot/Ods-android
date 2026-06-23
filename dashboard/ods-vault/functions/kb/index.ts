import "jsr:@supabase/functions-js/edge-runtime.d.ts";

// ODS Vault — knowledge base read API for the dashboard app.
// Serves the public.knowledge_base table via the service role (the table is RLS-locked,
// so the app can't read it directly). verify_jwt=true — the app calls with the anon key.
//
// This is the secure proxy: when life-vault is wired, a sync function will populate the
// source table(s); this read endpoint and the app UI stay the same shape.

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const REST = `${SUPABASE_URL}/rest/v1`;

const svc: Record<string, string> = {
  apikey: SERVICE_KEY,
  Authorization: `Bearer ${SERVICE_KEY}`,
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

Deno.serve(async (req: Request) => {
  const q = new URL(req.url).searchParams;
  const limit = Math.min(Number(q.get("limit") ?? "300") || 300, 1000);
  try {
    const select =
      "select=id,title,question,answer,content,category,language,status,updated_at";
    const r = await fetch(
      `${REST}/knowledge_base?${select}&archived_at=is.null&order=updated_at.desc.nullslast&limit=${limit}`,
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

    return json({ categories, entries, total: entries.length, generated_at: new Date().toISOString() });
  } catch (e) {
    return json({ error: e instanceof Error ? e.message : String(e) }, 500);
  }
});
