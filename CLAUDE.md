# BoraNutri — CLAUDE.md

## Project Overview

BoraNutri is a personal nutrition tracking mobile app. Users log meals (via text or photo), daily activity, and weight. Claude AI estimates macros from natural language and images, and generates personalized daily and weekly insights based on trends.

The app is for personal use initially, with potential to open to other users later.

---

## Architecture

### Frontend
- **Framework**: Quasar (Vue 3) — mobile-first, targeting iOS via Capacitor
- **Language**: TypeScript
- **State**: Pinia
- **HTTP**: Axios
- **Charts**: Chart.js or lightweight SVG-based custom components

### Backend
- **Language**: Scala 3
- **HTTP server**: Cask
- **JSON**: uPickle
- **Database access**: ScalaSql
- **Database**: PostgreSQL
- **Build tool**: Mill

### AI
- **Provider**: Anthropic Claude API
- **Model**: claude-sonnet-4-20250514
- **Vision**: enabled (for meal photo analysis)
- **Pattern**: stateless calls — no conversation history sent, only structured summaries

### Deployment
- Kubernetes (planned)
- Backend containerized with Docker
- PostgreSQL managed separately

---

## Project Structure

```
boranutr/
├── backend/
│   ├── src/
│   │   ├── BoraNutri.scala          # Cask app entry point
│   │   ├── routes/
│   │   │   ├── MealRoutes.scala     # POST /meals, GET /meals/today
│   │   │   ├── InsightRoutes.scala  # GET /insights/daily, /insights/weekly
│   │   │   ├── ActivityRoutes.scala # POST /activity
│   │   │   └── WeightRoutes.scala   # POST /weight, GET /weight/trend
│   │   ├── services/
│   │   │   ├── ClaudeService.scala  # All Anthropic API calls
│   │   │   ├── MealService.scala
│   │   │   ├── InsightService.scala
│   │   │   └── AggregateService.scala # Computes summaries sent to Claude
│   │   ├── db/
│   │   │   ├── Schema.scala         # ScalaSql table definitions
│   │   │   └── Migrations.scala
│   │   └── models/
│   │       ├── Meal.scala
│   │       ├── Macros.scala
│   │       ├── Activity.scala
│   │       ├── Weight.scala
│   │       └── Insight.scala
│   └── build.sc
│
├── frontend/
│   ├── src/
│   │   ├── pages/
│   │   │   ├── TodayPage.vue        # Main dashboard screen
│   │   │   ├── LogPage.vue          # Meal + activity log screen
│   │   │   └── TrendsPage.vue       # Weight + consistency trends
│   │   ├── components/
│   │   │   ├── CalorieCard.vue
│   │   │   ├── MacroChips.vue
│   │   │   ├── MealList.vue
│   │   │   ├── InsightCard.vue
│   │   │   ├── WeightChart.vue
│   │   │   └── ConsistencyBars.vue
│   │   ├── stores/
│   │   │   ├── today.ts
│   │   │   ├── log.ts
│   │   │   └── trends.ts
│   │   └── api/
│   │       └── client.ts            # Typed Axios wrapper
│   └── quasar.config.ts
│
└── CLAUDE.md
```

---

## Database Schema

### meals
```sql
CREATE TABLE meals (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  logged_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  description TEXT NOT NULL,                -- raw user input
  source      TEXT NOT NULL,                -- 'text' | 'photo_meal' | 'photo_box'
  kcal        INT,
  protein_g   NUMERIC(5,1),
  carbs_g     NUMERIC(5,1),
  fat_g       NUMERIC(5,1),
  fiber_g     NUMERIC(5,1),
  raw_estimate JSONB                        -- full Claude response stored for reference
);
```

### activity_logs
```sql
CREATE TABLE activity_logs (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  logged_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  entry       TEXT NOT NULL,               -- free text: "1h gym, felt strong, 9200 steps"
  parsed      JSONB                        -- Claude-parsed: {type, duration_min, intensity, steps, notes}
);
```

### weight_logs
```sql
CREATE TABLE weight_logs (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  logged_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  weight_kg   NUMERIC(5,2) NOT NULL
);
```

### user_profile
```sql
CREATE TABLE user_profile (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  goal          TEXT NOT NULL,             -- 'fat_loss' | 'muscle_gain' | 'maintenance'
  target_kcal   INT NOT NULL,
  target_protein_g INT NOT NULL,
  target_carbs_g   INT NOT NULL,
  target_fat_g     INT NOT NULL,
  target_fiber_g   INT NOT NULL DEFAULT 25,
  target_water_l   NUMERIC(3,1) NOT NULL DEFAULT 2.5,
  base_weight_kg   NUMERIC(5,2),
  goal_weight_kg   NUMERIC(5,2),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

---

## Claude API Usage

### Core principle
**Never send raw history. Always send computed summaries.**
PostgreSQL aggregates the data. Claude receives only what it needs for the current call.

### Call types

#### 1. Meal estimation (text)
```
System: You are a nutrition expert. Return ONLY valid JSON, no prose.
User:
  Targets: 2200 kcal, 160g protein, 200g carbs, 70g fat, 25g fiber
  Already logged today: 820 kcal, 68g protein, 110g carbs, 28g fat, 8g fiber
  New meal: "grilled chicken breast, cup of basmati rice, roasted vegetables, ~340g total"
  
  Return: { kcal, protein_g, carbs_g, fat_g, fiber_g, description }
```

#### 2. Meal estimation (photo)
```
Same as above but include base64 image.
Claude reads: nutrition label, barcode product name, or visual meal — automatically.
```

#### 3. Daily insight
```
System: You are a personal nutrition coach. Be specific and actionable. Max 2 sentences per insight.
User:
  Profile: goal=fat_loss, targets={kcal:2200, protein:160g, carbs:200g, fat:70g, fiber:25g}
  Today: {kcal:1440, protein:142g, carbs:98g, fat:44g, fiber:18g, water:1.6L}
  Activity: "1h chest and back, felt strong, ~9200 steps, legs sore from Tuesday"
  Weight today: 83.2kg
  7-day averages: {kcal:1980, protein:138g, fiber:16g}
  Pattern notes: protein missed 3/7 days (all weekends), calories trending +80kcal/week
  
  Return: { insight: string, type: 'protein'|'timing'|'fiber'|'hydration'|'recovery'|'weight' }
```

#### 4. Weekly insight (sent once per week)
```
Richer context: 30-day aggregates, weight trend, consistency scores, notable patterns.
Returns: array of 3 insights ordered by impact.
```

---

## API Routes

```
POST   /api/meals              # Log a meal (text or photo)
GET    /api/meals/today        # Get today's meals + running totals
POST   /api/activity           # Log activity entry (free text)
POST   /api/weight             # Log weight
GET    /api/weight/trend       # Weight trend data for chart
GET    /api/insights/daily     # Today's AI insight
GET    /api/insights/weekly    # Weekly summary insights
GET    /api/profile            # Get user profile + targets
PUT    /api/profile            # Update targets / goal
GET    /api/trends/summary     # Aggregated stats for trends screen
```

---

## Key Behaviours

### Meal logging flow
1. User submits text or photo (+ optional weight in grams)
2. Backend calls Claude with today's totals + new input
3. Claude returns structured macros JSON
4. Backend stores meal, returns estimated macros to frontend
5. Frontend shows confirmation card — user can adjust before final save
6. On confirm, macros are locked and totals updated

### Activity parsing
- Free text entry: *"1h gym chest and back, felt strong, 9200 steps, legs sore"*
- Claude parses into structured JSON on save: `{type, duration_min, intensity, steps, mood, notes}`
- Stored both as raw text and parsed JSON
- Parsed fields used for calorie target adjustment and insight generation

### Calorie target adjustment
- Rest day: base target
- Light activity (<30 min, low intensity): base + 100–150 kcal
- Moderate workout (30–60 min): base + 200–300 kcal
- Hard session (>60 min or high intensity): base + 300–500 kcal
- Logic lives in `AggregateService.scala`, not in Claude prompt

### Weight trend
- Daily log, morning preferred
- 7-day rolling average computed in PostgreSQL
- Weekly pace (kg/week) computed server-side
- ETA to goal computed server-side
- Claude only receives summary values, never raw daily weights

---

## Development Guidelines

### Scala backend
- Use Cask for routing, uPickle for all JSON serialization/deserialization
- Define case classes for all request/response models with uPickle `ReadWriter`
- Use ScalaSql for all DB queries — no raw SQL strings except migrations
- Claude API calls in `ClaudeService.scala` only — never inline in routes
- All Claude responses parsed and validated before returning to frontend
- Return structured error responses with `code` and `message` fields

### Vue frontend
- Composition API with `<script setup>` throughout
- TypeScript strict mode — type all API responses explicitly
- One Pinia store per screen (today, log, trends)
- No business logic in components — computed values in stores
- Camera access via Capacitor Camera plugin for photo input
- All API calls go through `src/api/client.ts` — never raw fetch/axios in components

### General
- All dates stored and handled in UTC, displayed in user's local time
- Macro values always stored as NUMERIC in DB, rounded to 1 decimal
- Claude JSON responses always validated — if parsing fails, return error to user, do not guess
- Keep Claude prompts in dedicated prompt builder functions, not inline strings
- Never log Claude API responses to console in production (may contain food photos)

---

## Environment Variables

```bash
# Backend
ANTHROPIC_API_KEY=sk-...
DATABASE_URL=postgresql://localhost:5432/boranutr
PORT=8080

# Frontend
VITE_API_BASE_URL=http://localhost:8080
```

---

## First Session Checklist

- [ ] Sbt project scaffold with Cask + uPickle + ScalaSql dependencies
- [ ] PostgreSQL schema migrations (meals, activity_logs, weight_logs, user_profile)
- [ ] `ClaudeService.scala` with meal estimation (text) call
- [ ] `POST /api/meals` route — text input → Claude → store → return macros
- [ ] `GET /api/meals/today` route — return meals + running totals
- [ ] Quasar project scaffold with Capacitor
- [ ] `TodayPage.vue` — calorie card + macro chips + meal list (static data first)
- [ ] `LogPage.vue` — text input + weight field + macro preview card
- [ ] Wire frontend to backend for meal log flow end to end
- [ ] Test full loop: type meal → see macros → confirm → totals update
