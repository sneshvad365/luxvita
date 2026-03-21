# Mullerthal Theme Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle LuxVita's visual theme to match the Mullerthal reference design — earthy palette, Jost + Cormorant Garamond fonts, organic background texture, and global Quasar component overrides.

**Architecture:** Pure CSS/config changes across 4 files. No component restructuring. Quasar brand variables are overridden in `:root` so all Quasar-coloured components pick up the new palette automatically. Fonts loaded via Google Fonts in index.html. Background texture applied via `body` pseudo-elements in app.scss.

**Tech Stack:** Vue 3 / Quasar / SCSS · Node.js (npm run build to verify)

**Spec:** `docs/superpowers/specs/2026-03-21-mullerthal-theme-design.md`

---

## Chunk 1: Config & Fonts

### Task 1: Update quasar.config.js

**Files:**
- Modify: `frontend/quasar.config.js`

- [ ] Remove `'roboto-font'` from the `extras` array. Change:

```js
extras: ['roboto-font', 'material-icons'],
```

to:

```js
extras: ['material-icons'],
```

- [ ] Update `pwa.manifest.background_color` and `pwa.manifest.theme_color`:

```js
background_color: '#f5f0e8',
theme_color: '#3d4a2e',
```

- [ ] Commit:

```bash
cd /Users/surena/luxvita
git add frontend/quasar.config.js
git commit -m "theme: update Quasar config — remove Roboto, set moss/cream PWA colors"
```

---

### Task 2: Add Google Fonts to index.html

**Files:**
- Modify: `frontend/index.html`

- [ ] Add the Google Fonts `<link>` tag inside `<head>`, after the existing `<link rel="icon">` line:

```html
<link href="https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@0,300;0,400;0,600;1,300;1,400&family=Jost:wght@200;300;400;500&display=swap" rel="stylesheet">
```

The full `<head>` block should now look like:

```html
<head>
  <title>LuxVita</title>

  <meta charset="utf-8">
  <meta name="description" content="LuxVita — AI-powered nutrition tracking">
  <meta name="format-detection" content="telephone=no">
  <meta name="msapplication-tap-highlight" content="no">
  <meta name="viewport" content="user-scalable=no, initial-scale=1, maximum-scale=1, minimum-scale=1, width=device-width, viewport-fit=cover">

  <link rel="icon" type="image/png" href="icons/favicon-128x128.png">
  <link href="https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@0,300;0,400;0,600;1,300;1,400&family=Jost:wght@200;300;400;500&display=swap" rel="stylesheet">
</head>
```

- [ ] Commit:

```bash
git add frontend/index.html
git commit -m "theme: add Cormorant Garamond + Jost via Google Fonts"
```

---

## Chunk 2: App SCSS & InsightCard

### Task 3: Write app.scss

**Files:**
- Modify: `frontend/src/css/app.scss` (currently empty — just add content)

- [ ] Replace the entire contents of `frontend/src/css/app.scss` with:

```scss
/* ── Mullerthal colour palette ── */
:root {
  --moss:       #3d4a2e;
  --moss-light: #5a6b42;
  --fern:       #7a9160;
  --sage:       #a8b899;
  --mist:       #d8e0cc;
  --stone:      #c4b89a;
  --sandstone:  #e8dfc8;
  --bark:       #6b5c42;
  --bark-light: #8c7a5e;
  --cream:      #f5f0e8;
  --white:      #fdfbf7;
  --shadow:     rgba(42, 38, 28, 0.12);
  --text-dark:  #2a2618;
  --text-mid:   #5c5440;
  --text-light: #8c8068;

  /* Override Quasar brand variables */
  --q-primary:  #3d4a2e;
  --q-secondary:#7a9160;
  --q-accent:   #c4b89a;
  --q-dark:     #2a2618;
  --q-positive: #7a9160;
  --q-negative: #c4856a;
  --q-warning:  #c4b89a;
  --q-info:     #5a6b42;
}

/* ── Base typography ── */
html {
  font-size: 16px;
  scroll-behavior: smooth;
}

body {
  background: var(--cream);
  color: var(--text-dark);
  font-family: 'Jost', sans-serif;
  font-weight: 300;
}

/* ── Organic background: gradient overlay ── */
body::before {
  content: '';
  position: fixed;
  inset: 0;
  background-image:
    radial-gradient(ellipse at 15% 20%, rgba(122, 145, 96, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 85% 70%, rgba(107, 92, 66, 0.07)  0%, transparent 45%),
    radial-gradient(ellipse at 50% 50%, rgba(196, 184, 154, 0.05) 0%, transparent 60%);
  pointer-events: none;
  z-index: 0;
}

/* ── Organic background: grain texture ── */
body::after {
  content: '';
  position: fixed;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='0.04'/%3E%3C/svg%3E");
  opacity: 0.4;
  pointer-events: none;
  z-index: 0;
}

/*
 * Lift all app content above the fixed pseudo-element overlays.
 * Scoped to #q-app only — do NOT apply to body or use a blanket * selector.
 * Quasar's q-dialog / q-menu / q-tooltip render outside #q-app via document.body
 * portals and rely on position:fixed with no stacking-context restriction.
 */
#q-app,
#q-app * {
  position: relative;
  z-index: 1;
}

/* ── Page container: let cream body show through ── */
.q-page-container,
.q-page {
  background: transparent;
}

/* ── Cards ── */
.q-card {
  background: var(--white);
  border: 1px solid rgba(196, 184, 154, 0.3);
  box-shadow: 0 2px 12px var(--shadow);
  border-radius: 12px;
}

/* ── Header ── */
.q-header {
  border-bottom: 1px solid rgba(168, 184, 153, 0.3);
}

/* ── Bottom nav tabs ── */
.q-footer .q-tabs {
  background: var(--white);
  border-top: 1px solid rgba(196, 184, 154, 0.25);
}

.q-footer .q-tab__label {
  font-family: 'Jost', sans-serif;
  font-weight: 300;
}

/* ── Flat buttons ── */
.q-btn--flat {
  color: var(--moss);
}

/* ── Input labels ── */
.q-field__label {
  color: var(--text-light);
  font-family: 'Jost', sans-serif;
}

/* ── Insight card (dark moss panel) ── */
.insight-card {
  background: var(--moss) !important;
  border-color: transparent !important;
  color: var(--cream);
}

.insight-card .text-body2 {
  color: var(--sandstone) !important;
}

.insight-card .text-caption {
  color: var(--sage) !important;
}

/* ── Utility: Cormorant Garamond for decorative numbers ── */
.font-cormorant {
  font-family: 'Cormorant Garamond', serif;
  font-weight: 300;
}
```

- [ ] Commit:

```bash
git add frontend/src/css/app.scss
git commit -m "theme: add Mullerthal palette, fonts, background texture, and component overrides"
```

---

### Task 4: Add insight-card class to InsightCard.vue

**Files:**
- Modify: `frontend/src/components/InsightCard.vue` line 2

- [ ] Change line 2 from:

```html
  <q-card :class="`bg-${color}-1`">
```

to:

```html
  <q-card class="insight-card" :class="`bg-${color}-1`">
```

This is a one-line change. Do not touch anything else in the file.

- [ ] Commit:

```bash
git add frontend/src/components/InsightCard.vue
git commit -m "theme: add insight-card class to InsightCard root element"
```

---

### Task 5: Build and verify

- [ ] Build the frontend:

```bash
cd /Users/surena/luxvita/frontend && npm run build
```

Expected: build completes with no errors. TypeScript errors would indicate an accidental edit to a `.ts` or `.vue` script block — only CSS and HTML were changed so there should be none.

If the build fails, read the error output and check that no non-CSS change was accidentally introduced in Tasks 3–4.
