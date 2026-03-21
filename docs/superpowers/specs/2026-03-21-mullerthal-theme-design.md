# Mullerthal Theme Design

**Date:** 2026-03-21
**Status:** Approved

## Overview

Restyle LuxVita's visual theme to match the Mullerthal nutrition HTML reference (`frontend/src/assets/mullerthal-nutrition.html`). Scope is **theme layer only** — colors, fonts, background texture, and global card/button overrides. No component restructuring, no layout changes, no new components.

## Color Palette

Replace Quasar's default blue Material palette with the Mullerthal earthy/organic palette via CSS custom properties in `app.scss` and Quasar brand variable overrides.

| CSS variable | Value | Role |
|---|---|---|
| `--moss` | `#3d4a2e` | Primary — header, active tabs, primary buttons |
| `--moss-light` | `#5a6b42` | Primary hover/pressed states |
| `--fern` | `#7a9160` | Secondary |
| `--sage` | `#a8b899` | Borders, dividers, muted accents |
| `--mist` | `#d8e0cc` | Progress bar tracks, subtle fills |
| `--stone` | `#c4b89a` | Accent |
| `--sandstone` | `#e8dfc8` | Hover backgrounds |
| `--bark` | `#6b5c42` | Warm dark accent |
| `--bark-light` | `#8c7a5e` | Warm mid accent |
| `--cream` | `#f5f0e8` | Page background |
| `--white` | `#fdfbf7` | Card/surface background |
| `--shadow` | `rgba(42,38,28,0.12)` | Box shadows |
| `--text-dark` | `#2a2618` | Primary text |
| `--text-mid` | `#5c5440` | Secondary text |
| `--text-light` | `#8c8068` | Muted/caption text |

Quasar's brand variables are overridden to map onto this palette (set in `:root` in `app.scss`):
- `--q-primary: #3d4a2e`
- `--q-secondary: #7a9160`
- `--q-accent: #c4b89a`
- `--q-dark: #2a2618`
- `--q-positive: #7a9160` (fern — replaces default green)
- `--q-negative: #c4856a` (warm terracotta — replaces default red)
- `--q-warning: #c4b89a` (stone — replaces default amber)
- `--q-info: #5a6b42` (moss-light — replaces default blue)

## Typography

Two fonts loaded from Google Fonts via `<link>` in `frontend/index.html`:

```html
<link href="https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@0,300;0,400;0,600;1,300;1,400&family=Jost:wght@200;300;400;500&display=swap" rel="stylesheet">
```

- **Body / UI text**: `'Jost', sans-serif` at `font-weight: 300` — set on `body` in `app.scss`
- **Decorative / numbers**: `'Cormorant Garamond', serif` — available as a utility class (`.font-cormorant`) for use in page components if desired, but not applied globally to component internals in this phase

Remove `roboto-font` from the `extras` array in `quasar.config.js` (Jost replaces it).

## Page Background

Applied in `app.scss`:

```scss
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

// Organic gradient overlay
body::before {
  content: '';
  position: fixed;
  inset: 0;
  background-image:
    radial-gradient(ellipse at 15% 20%, rgba(122,145,96,0.08) 0%, transparent 50%),
    radial-gradient(ellipse at 85% 70%, rgba(107,92,66,0.07) 0%, transparent 45%),
    radial-gradient(ellipse at 50% 50%, rgba(196,184,154,0.05) 0%, transparent 60%);
  pointer-events: none;
  z-index: 0;
}

// Grain texture overlay
body::after {
  content: '';
  position: fixed;
  inset: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)' opacity='0.04'/%3E%3C/svg%3E");
  opacity: 0.4;
  pointer-events: none;
  z-index: 0;
}

// Lift page content above fixed pseudo-element overlays
// Scoped to avoid breaking Quasar's fixed-position portals (dialogs, drawers, menus, tooltips)
#q-app, #q-app * {
  position: relative;
  z-index: 1;
}
```

**Important:** Do NOT apply `position: relative` to `body` or use a blanket `*` selector. Quasar's `q-dialog`, `q-drawer`, `q-menu`, and `q-tooltip` components render via `document.body` and rely on `position: fixed` with an unrestricted stacking context. Scoping to `#q-app` is sufficient — the pseudo-elements are on `body` at `z-index: 0`, and `#q-app` at `z-index: 1` lifts all app content above them.

## Global Component Overrides (app.scss)

These target Quasar's generated CSS classes globally:

### Cards
```scss
.q-card {
  background: var(--white);
  border: 1px solid rgba(196, 184, 154, 0.3);
  box-shadow: 0 2px 12px var(--shadow);
  border-radius: 12px;
}
```

### Header / Toolbar
The existing `bg-primary` class on `<q-header>` will automatically use the new `--q-primary` moss color. Add a subtle bottom border:
```scss
.q-header {
  border-bottom: 1px solid rgba(168, 184, 153, 0.3); // --sage at 0.3 opacity
}
```

### Bottom nav tabs
```scss
.q-footer .q-tabs {
  background: var(--white);
  border-top: 1px solid rgba(196, 184, 154, 0.25);
}
.q-footer .q-tab__label {
  font-family: 'Jost', sans-serif;
  font-weight: 300;
}
```

### Buttons
```scss
.q-btn--flat {
  color: var(--moss);
}
```

Filled buttons (`q-btn[color="primary"]`) automatically use white text via Quasar's built-in contrast logic against the dark moss background. No additional text color override needed.

### Insight card
`InsightCard.vue`'s root element is `<q-card :class="`bg-${color}-1`">` — it has no static class. Add `class="insight-card"` to that element so `app.scss` can target it:

```html
<!-- InsightCard.vue line 2 — change from: -->
<q-card :class="`bg-${color}-1`">
<!-- to: -->
<q-card class="insight-card" :class="`bg-${color}-1`">
```

Then in `app.scss`:
```scss
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
```

### Input fields
```scss
.q-field__label {
  color: var(--text-light);
  font-family: 'Jost', sans-serif;
}
```

### Page container
```scss
.q-page-container, .q-page {
  background: transparent; // let cream body show through
}
```

## PWA Manifest (quasar.config.js)

Update `pwa.manifest.background_color` and `pwa.manifest.theme_color`:
- `background_color`: `#f5f0e8` (cream)
- `theme_color`: `#3d4a2e` (moss)

## Files Changed

| File | Change |
|---|---|
| `frontend/index.html` | Add Google Fonts `<link>` tag |
| `frontend/quasar.config.js` | Remove `roboto-font` from extras; update PWA `background_color`/`theme_color` |
| `frontend/src/css/app.scss` | All color variables, body styles, background texture, global component overrides |
| `frontend/src/components/InsightCard.vue` | Add `class="insight-card"` to root `<q-card>` (one-line template addition) |

## Out of Scope

- Restructuring any Vue component's template or layout
- Adding new components or pages
- Changing Cormorant Garamond numbers into specific data display components
- Dark mode support
- Changing any route, store, or API logic
