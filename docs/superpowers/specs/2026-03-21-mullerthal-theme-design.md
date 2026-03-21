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

Quasar's brand variables are overridden to map onto this palette:
- `--q-primary: #3d4a2e`
- `--q-secondary: #7a9160`
- `--q-accent: #c4b89a`
- `--q-dark: #2a2618`

## Typography

Two fonts loaded from Google Fonts via `<link>` in `frontend/index.html`:

```html
<link href="https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@0,300;0,400;0,600;1,300;1,400&family=Jost:wght@200;300;400;500&display=swap" rel="stylesheet">
```

- **Body / UI text**: `'Jost', sans-serif` at `font-weight: 300` — set on `body` in `app.scss`
- **Decorative / numbers**: `'Cormorant Garamond', serif` — available as a utility class (`.font-cormorant`) for use in page components if desired, but not applied globally to component internals in this phase

Remove `roboto-font` from the `extras` array in `quasar.config.js` (Jost replaces it).

## Page Background

Applied to `body` in `app.scss`:

```scss
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
  background-image: url("data:image/svg+xml,..."); // same SVG noise as reference
  opacity: 0.4;
  pointer-events: none;
  z-index: 0;
}
```

All page content must sit above these overlays. Quasar's `#q-app` and `body > *` get `position: relative; z-index: 1`.

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
The existing `bg-primary` class on `<q-header>` will automatically use the new `--q-primary` moss color. No additional override needed.

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

Update `background_color` and `theme_color` in the PWA manifest to match:
- `background_color`: `#f5f0e8` (cream)
- `theme_color`: `#3d4a2e` (moss)

## Files Changed

| File | Change |
|---|---|
| `frontend/index.html` | Add Google Fonts `<link>` tag |
| `frontend/quasar.config.js` | Remove `roboto-font` from extras; update PWA `background_color`/`theme_color` |
| `frontend/src/css/app.scss` | All color variables, body styles, background texture, global component overrides |

## Out of Scope

- Restructuring any Vue component's template or layout
- Adding new components or pages
- Changing Cormorant Garamond numbers into specific data display components
- Dark mode support
- Changing any route, store, or API logic
