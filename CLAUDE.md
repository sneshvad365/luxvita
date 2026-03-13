# LuxVita — CLAUDE.md

## Project Overview

LuxVita is a personal nutrition tracking mobile app. Users log meals (via text or photo), daily activity, and weight. Claude AI estimates macros from natural language and images, and generates personalized daily and weekly insights based on trends.

Multiple users can register and use the app independently. All data is fully isolated per user.

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
- **Build tool**: sbt

### AI
- **Provider**: Anthropic Claude API
- **Model**: claude-sonnet-4-20250514
- **Vision**: enabled (for meal photo analysis)
- **Pattern**: stateless calls — no conversation history sent, only structured summaries

### Auth
- Email + password authentication
- Passwords hashed with BCrypt
- JWT tokens for session management
- All API routes protected except `/api/auth/register` and `/api/auth/login`

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
│   │   └── main/
│   │       └── scala/
│   │           ├── LuxVita.scala            # Cask app entry point
│   │           ├── routes/
│   │           │   ├── AuthRoutes.scala        # POST /auth/register, /auth/login
│   │           │   ├── MealRoutes.scala        # POST /meals, GET /meals/today
│   │           │   ├── InsightRoutes.scala     # GET /insights/daily, /insights/weekly
│   │           │   ├── ActivityRoutes.scala    # POST /activity
│   │           │   ├── WeightRoutes.scala      # POST /weight, GET /weight/trend
│   │           │   └── ProfileRoutes.scala     # GET/PUT /profile
│   │           ├── services/
│   │           │   ├── ClaudeService.scala     # All Anthropic API calls
│   │           │   ├── AuthService.scala       # JWT + BCrypt logic
│   │           │   ├── MealService.scala
│   │           │   ├── InsightService.scala
│   │           │   └── AggregateService.scala  # Computes summaries sent to Claude
│   │           ├── middleware/
│   │           │   └── AuthMiddleware.scala    # JWT validation, extract userId
│   │           ├── db/
│   │           │   ├── Schema.scala            # ScalaSql table definitions
│   │           │   └── Migrations.scala
│   │           └── models/
│   │               ├── User.scala
│   │               ├── Meal.scala
│   │               ├── Macros.scala
│   │               ├── Activity.scala
│   │               ├── Weight.scala
│   │               └── Insight.scala
│   └── build.sbt
│
├── frontend/
│   ├── src/
│   │   ├── pages/
│   │   │   ├── LoginPage.vue          # Email + password login
│   │   │   ├── RegisterPage.vue       # New user registration
│   │   │   ├── TodayPage.vue          # Main dashboard screen
│   │   │   ├── LogPage.vue            # Meal + activity log screen
│   │   │   ├── TrendsPage.vue         # Weight + consistency trends
│   │   │   └── ProfilePage.vue        # User profile + targets + bio
│   │   ├── components/
│   │   │   ├── CalorieCard.vue
│   │   │   ├── MacroChips.vue
│   │   │   ├── MealList.vue
│   │   │   ├── InsightCard.vue
│   │   │   ├── WeightChart.vue
│   │   │   └── ConsistencyBars.vue
│   │   ├── stores/
│   │   │   ├── auth.ts                # JWT token, current user
│   │   │   ├── today.ts
│   │   │   ├── log.ts
│   │   │   ├── trends.ts
│   │   │   └── profile.ts
│   │   └── api/
│   │       └── client.ts              # Typed Axios wrapper, injects JWT header
│   └── quasar.config.ts
│
├── .env
├── .gitignore
└── CLAUDE.md
```

---

## Database Schema

### users
```sql
CREATE TABLE users (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email         TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### user_profile
```sql
CREATE TABLE user_profile (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  bio              TEXT,                            -- free text profile for AI context
  goal             TEXT NOT NULL,                  -- 'fat_loss' | 'muscle_gain' | 'maintenance'
  target_kcal      INT NOT NULL,
  target_protein_g INT NOT NULL,
  target_carbs_g   INT NOT NULL,
  target_fat_g     INT NOT NULL,
  target_fiber_g   INT NOT NULL DEFAULT 25,
  target_water_l   NUMERIC(3,1) NOT NULL DEFAULT 2.5,
  base_weight_kg   NUMERIC(5,2),
  goal_weight_kg   NUMERIC(5,2),
  updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(user_id)
);
```

### meals
```sql
CREATE TABLE meals (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  logged_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  description  TEXT,                              -- optional text input from user
  has_photo    BOOLEAN NOT NULL DEFAULT false,    -- whether a photo was submitted
  kcal         INT,
  protein_g    NUMERIC(5,1),
  carbs_g      NUMERIC(5,1),
  fat_g        NUMERIC(5,1),
  fiber_g      NUMERIC(5,1),
  raw_estimate JSONB                              -- full Claude response stored for reference
);
```

### activity_logs
```sql
CREATE TABLE activity_logs (
  id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  logged_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  entry     TEXT NOT NULL,                        -- free text: "1h gym, felt strong, 9200 steps"
  parsed    JSONB                                 -- Claude-parsed: {type, duration_min, intensity, steps, notes}
);
```

### weight_logs
```sql
CREATE TABLE weight_logs (
  id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id   UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  logged_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  weight_kg NUMERIC(5,2) NOT NULL
);
```

---

## User Profile Bio

The `bio` field in `user_profile` is a free-text description the user writes about themselves. It is included in every insight and weekly report call to give Claude personal context.

### Profile page placeholder text
The textarea for the bio should display the following example to guide users on what is useful to include:

```
Male, 33, 180cm, 83kg. Goal is to lose fat while keeping muscle.
Train 4x a week — gym (chest/back, legs) and tennis on weekends.
Desk job, mostly sedentary outside workouts. Tend to snack a lot
in the evening. Lactose intolerant. Trying to eat more protein
but struggle on weekends.
```

This signals that lifestyle context, dietary constraints, and habits are just as useful as the numbers.

---

## Claude API Usage

### Core principle
**Never send raw history. Always send computed summaries.**
PostgreSQL aggregates the data. Claude receives only what it needs for the current call.

### Call types

#### 1. Meal estimation (text only)
```
System: You are a nutrition expert. Return ONLY valid JSON, no prose.
User:
  Targets: 2200 kcal, 160g protein, 200g carbs, 70g fat, 25g fiber
  Already logged today: 820 kcal, 68g protein, 110g carbs, 28g fat, 8g fiber
  New meal: "grilled chicken breast, cup of basmati rice, roasted vegetables"

  Return: { kcal, protein_g, carbs_g, fat_g, fiber_g, description }
```

#### 2. Meal estimation (photo only, or photo + text)
```
Same structure as above. Include base64 image in the message content.
If text is also provided, treat it as additional context about the photo
(e.g. "500g, fried in lots of oil", "shared with someone", "restaurant portion").
Claude automatically handles: visual meal, nutrition label, barcode, or product name.
Text + photo together always produce a more accurate estimate than either alone.
```

#### 3. Daily insight
```
System: You are a personal nutrition coach. Be specific and actionable. Max 2 sentences per insight.
User:
  Profile: "Male, 33, 180cm, 83kg. Desk job, trains 4x/week, lactose intolerant. Goal: fat loss."
  Targets: {kcal:2200, protein:160g, carbs:200g, fat:70g, fiber:25g}
  Today: {kcal:1440, protein:142g, carbs:98g, fat:44g, fiber:18g, water:1.6L}
  Activity: "1h chest and back, felt strong, ~9200 steps, legs sore from Tuesday"
  Weight today: 83.2kg
  7-day averages: {kcal:1980, protein:138g, fiber:16g}
  Pattern notes: protein missed 3/7 days (all weekends), calories trending +80kcal/week

  Return: { insight: string, type: 'protein'|'timing'|'fiber'|'hydration'|'recovery'|'weight' }
```

#### 4. Weekly insight (sent once per week)
```
Same structure but with richer context:
- 30-day aggregates and trends
- Weight trend + pace toward goal
- Consistency scores per metric
- Notable patterns (e.g. weekend dips, post-training gaps)
- Full user bio included

Returns: array of 3 insights ordered by impact.
```

---

## API Routes

```
POST   /api/auth/register       # Create account (email + password)
POST   /api/auth/login          # Login, returns JWT

# All routes below require Authorization: Bearer <token>

POST   /api/meals               # Log a meal (text, photo, or both)
GET    /api/meals/today         # Get today's meals + running totals
POST   /api/activity            # Log activity entry (free text)
POST   /api/weight              # Log weight
GET    /api/weight/trend        # Weight trend data for chart
GET    /api/insights/daily      # Today's AI insight
GET    /api/insights/weekly     # Weekly summary insights
GET    /api/profile             # Get user profile + targets + bio
PUT    /api/profile             # Update profile, targets, bio
GET    /api/trends/summary      # Aggregated stats for trends screen
```

---

## Key Behaviours

### Auth flow
1. User registers with email + password
2. Password hashed with BCrypt before storing
3. Login returns a signed JWT containing `userId`
4. All protected routes validate JWT and extract `userId` via `AuthMiddleware`
5. Every DB query filters by `userId` — users never see each other's data
6. JWT expiry: 30 days, no refresh token needed for MVP

### Meal logging flow
1. User submits text description and/or a photo (at least one required)
2. If both provided, text is treated as additional context for the photo
3. Backend calls Claude with today's totals + text + optional base64 image
4. Claude returns structured macros JSON
5. Backend stores meal, returns estimated macros to frontend
6. Frontend shows confirmation card — user can adjust before final save
7. On confirm, macros are locked and totals updated

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

### User profile bio in AI calls
- Bio is included verbatim in the user context of every insight call
- It is never sent during meal estimation calls (not needed, keeps cost low)
- If bio is empty, insight calls proceed without it — no error

---

## Development Guidelines

### Scala backend
- Use Cask for routing, uPickle for all JSON serialization/deserialization
- Define case classes for all request/response models with uPickle `ReadWriter`
- Use ScalaSql for all DB queries — no raw SQL strings except migrations
- Claude API calls in `ClaudeService.scala` only — never inline in routes
- All Claude responses parsed and validated before returning to frontend
- Return structured error responses with `code` and `message` fields
- Extract `userId` from JWT in middleware — never trust `userId` from request body

### Vue frontend
- Composition API with `<script setup>` throughout
- TypeScript strict mode — type all API responses explicitly
- One Pinia store per screen (auth, today, log, trends, profile)
- JWT stored in `localStorage`, injected into every request via Axios interceptor
- If JWT missing or expired, redirect to LoginPage automatically
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
# Backend (.env — never commit this file)
ANTHROPIC_API_KEY=sk-ant-...
DATABASE_URL=postgresql://localhost:5432/boranutr
JWT_SECRET=your-secret-key-here
PORT=8080

# Frontend
VITE_API_BASE_URL=http://localhost:8080
```

---

## First Session Checklist

- [ ] sbt project scaffold with Cask + uPickle + ScalaSql + BCrypt + JWT dependencies
- [ ] PostgreSQL schema migrations (users, user_profile, meals, activity_logs, weight_logs)
- [ ] `AuthService.scala` — BCrypt hashing + JWT sign/verify
- [ ] `AuthMiddleware.scala` — JWT validation, extract userId for protected routes
- [ ] `POST /api/auth/register` and `POST /api/auth/login` routes
- [ ] `ClaudeService.scala` with meal estimation (text) call
- [ ] `POST /api/meals` route — auth protected, text input → Claude → store → return macros
- [ ] `GET /api/meals/today` route — return meals + running totals for current user
- [ ] Quasar project scaffold with Capacitor
- [ ] `LoginPage.vue` and `RegisterPage.vue`
- [ ] Auth Pinia store — JWT storage + Axios interceptor
- [ ] `TodayPage.vue` — calorie card + macro chips + meal list (static data first)
- [ ] `LogPage.vue` — text input + photo button + macro preview card
- [ ] Wire frontend to backend for full auth + meal log flow end to end
- [ ] Test full loop: register → login → log meal → see macros → totals update
