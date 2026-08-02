# ODS Dashboard — icon generation prompts

Prompts for every icon the dashboard can use: the app logo/launcher, the four category
glyphs, and a tile icon for each of the 16 connections. They're tuned to the ODS visual
language so a generated set looks like one family.

Set each generated PNG in the app via **Settings → Customise → Tile icons** (per
connection) and **Logo**.

---

## How to use

Paste **[STYLE]** + one **subject** line into any image generator (Firefly, Midjourney,
DALL·E, etc.). Generate at the spec below, export PNG, import in the app.

### Output spec (use for all)
- **1024 × 1024 px**, 1:1 square.
- **Transparent background** (the tile already sits on a charcoal chip). If your tool
  can't do transparency, use a solid `#0E0E10` charcoal background.
- Single centred glyph filling ~70% of the canvas with even padding.
- Flat, minimal, **line/geometric** icon — crisp edges, consistent stroke weight, no
  photorealism, no 3D bevels, no drop shadows, no gradients (one subtle inner glow max).
- **No text or lettering** unless the subject explicitly says "monogram".

### [STYLE] — paste before every subject
> Minimalist operator-grade line icon, dark-industrial aesthetic. Single subject, centred,
> flat vector look, geometric, uniform 6–8px stroke on a 1024px canvas, sharp corners
> softened only slightly. Palette: silver `#C9CDD4` linework on a transparent background,
> with a single restrained crimson `#D42B2B` accent on one detail only. No text, no
> wordmark, no background scene, no gradient, no shadow. Quiet, precise, premium.

### Trademark note
For third-party brands (Facebook, Instagram, LinkedIn, GitHub, Vercel, Supabase, Telegram,
Gmail) the cleanest legal/visual route is the **official brand icon**. The prompts below
produce an **ODS-styled, on-brand alternative** (a recognisable motif, not the exact
logo) if you'd rather keep one consistent family. ODS-owned items (logo, sites, Tasjeel,
info@) are fully bespoke.

---

## Logo & launcher

**ODS logo / launcher** — subject:
> an outlined isometric cube drawn as a single continuous wireframe, hollow centre, the
> nearest vertical edge highlighted in crimson; the rest silver. Emblem only.

---

## Category glyphs (4)

**SOCIALS** — subject:
> three small circular nodes connected by thin lines into a triangle network, the top node
> filled crimson, the others silver outlines.

**SERVICES** — subject:
> a clean stack of three horizontal server/rack bars with rounded ends, a single crimson
> status dot on the top bar.

**SITES** — subject:
> a minimal browser window frame with a thin top bar and one crimson dot among three
> window dots, an abstract globe meridian arc inside.

**MESSAGES** — subject:
> a simple envelope outline merged with a chat bubble tail, the bubble tail tipped crimson.

---

## Connection tile icons (16)

### Socials
**facebook** — subject:
> a rounded square speech node with a single stylised lowercase serif-free "f" stroke
> formed from one continuous silver line, crimson dot in the corner. (Or use the official
> Facebook icon.)

**instagram** — subject:
> a rounded square camera outline with a centred concentric-circle lens and one small
> crimson corner dot. (Or use the official Instagram icon.)

**linkedin** — subject:
> a rounded square tile with an abstract "in" formed by a single dot and two vertical
> strokes in silver, crimson baseline. (Or use the official LinkedIn icon.)

**telegram_channel** — subject:
> a paper plane made of two clean silver triangles emitting from a small broadcast arc,
> the arc crimson — signalling a broadcast channel.

### Services
**github** — subject:
> a minimal silhouetted cat-robot head inside a circle, one eye a crimson dot — a generic
> code-host mascot, not the GitHub logo. (Or use the official GitHub mark.)

**vercel** — subject:
> a single solid upward-pointing equilateral triangle, silver, with a thin crimson
> underline. (Or use the official Vercel triangle.)

**supabase** — subject:
> a lightning bolt inside a rounded shield outline, bolt silver with a crimson tip. (Or
> use the official Supabase bolt.)

**tasjeel** — subject:
> a clipboard outline with a registration stamp/check mark, the check stroke crimson —
> evoking "registration / records".

### Sites
**website (outlined-design.com)** — subject:
> the ODS wireframe cube sitting inside a thin browser window frame, one window dot crimson.

**recon (ods-recon — past / historical intel)** — subject:
> a radar sweep: concentric arcs with a single rotating line and one crimson blip on a
> faint timeline tick at the base.

**link (ods-link — present / real-time ops)** — subject:
> two interlocking chain links overlaid with a thin live pulse/heartbeat line, the pulse
> peak crimson.

**aether (ods-aether — future / predictive)** — subject:
> three thin orbital ellipses around a central point with one small crimson planet/node on
> an outer orbit — a predictive/forecast motif.

**assist (ods-assist — conversational AI)** — subject:
> a rounded chat bubble containing a four-point spark/asterisk, the spark crimson — an AI
> assistant glyph.

### Messages
**telegram** — subject:
> a single clean paper plane built from two silver triangles, a short crimson motion
> dash behind it.

**email_gmail (zakkgray1@gmail.com)** — subject:
> an envelope outline whose flap forms a subtle "M"/chevron, one inner edge crimson — a
> generic mail glyph. (Or use the official Gmail icon.)

**email_ods (info@outlined-design.com)** — subject:
> an envelope outline with a small "@" symbol where a stamp would sit, the @ ring crimson —
> the ODS business mailbox.

---

## Launcher grid marks (12)

The **ODS Launcher** widget ships bundled vector glyphs for all twelve cells (see
`res/drawable/ic_launch_*.xml`). Use these subjects to generate replacement art, then set
it per cell in **Settings → Customise → Edit launcher grid**. Aether, Link, Recon and
Assist reuse the connection subjects above; the rest are new.

The label is already printed under each icon, so the mark must carry **no lettering** —
it has to read as a silhouette at roughly 34 dp.

**leads (ODS Leads / lead-gen)** — subject:
> a wide funnel narrowing to a short stem, drawn as a single silver outline, the top
> intake rim thickened in crimson.

**socials (the ODS Link socials queue)** — subject:
> three circular nodes wired into a triangle, the top broadcast node filled crimson and
> the two lower nodes silver outlines.

**aani** — subject:
> two interlocking rings side by side, the left ring silver and the right ring crimson —
> a paired-account motif.

**print3d (ODS-3Dprint)** — subject:
> an isometric cube drawn in silver wireframe with its three visible faces implied by
> internal edges, the two base edges thickened in crimson like a freshly laid first layer.

**home (outlined-design.com)** — subject:
> the ODS wireframe cube emblem, silver, nothing else. (The bundled default is the real
> ODS mark — replace only with the mark itself.)

**tools (ODS Link tools hub)** — subject:
> three stacked horizontal slider rails with a round knob on each, the middle knob filled
> crimson and the outer two silver outlines.

**docs (ODS TheView)** — subject:
> a document page with a folded top-right corner and two short text rules, overlaid at the
> lower right by a crimson magnifier lens.

**vercel** — subject:
> a single solid upward-pointing equilateral triangle in silver, with a thin crimson
> underline beneath it. (Or use the official Vercel triangle.)

---

## Optional: full monochrome variant
If you prefer pure monochrome (no crimson), drop the crimson clause from **[STYLE]** and
render all-silver; then let the app's accent colour provide the only colour. Keep the set
internally consistent — generate all icons in one session with the same [STYLE] block.
