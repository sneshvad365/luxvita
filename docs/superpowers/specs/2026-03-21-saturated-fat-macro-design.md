# Saturated Fat Macro Tracking Design

**Date:** 2026-03-21
**Status:** Approved

## Overview

Add saturated fat as a fully tracked macro — estimated by Claude, stored in the database, shown in the meal log preview, and visible in the daily macro summary with a user-settable daily target.

## Scope

- Store `saturated_fat_g` per meal in the database
- Add `target_saturated_fat_g` to user profiles
- Update the Claude meal estimation prompt to return saturated fat
- Display saturated fat in the meal log preview card, the daily macro chips, and the profile targets form
- Saturated fat tracked at **meal level only** — not added to per-item `BreakdownItem` (keeps the breakdown card readable)

## Database

Create migration files before any backend code changes — Flyway rejects a run if migrations are missing.

**Migration V14: `V14__add_saturated_fat_to_meals.sql`**
```sql
ALTER TABLE meals ADD COLUMN saturated_fat_g NUMERIC(5,1);
```
Nullable — existing meals have no saturated fat value.

**Migration V15: `V15__add_saturated_fat_target.sql`**
```sql
ALTER TABLE user_profile ADD COLUMN target_saturated_fat_g INT NOT NULL DEFAULT 20;
```
Default 20g matches common dietary guidelines.

## Backend

### `models/Macros.scala`
Add `saturatedFatG: Double` to the `Macros` case class. Used for today's totals and all aggregate queries (7-day/30-day averages). Adding this field will cause a compile error in every site that constructs a `Macros(...)` literal — fix all of them.

### `models/User.scala`
Add `targetSaturatedFatG: Int = 20` to the `UserProfile` case class. The default matches the migration default.

### `models/Meal.scala`
Two case classes to update:

1. **`MacroEstimate`** — add `saturatedFatG: Option[Double] = None`. `Option` because Claude may omit the field for some meals. Default `None` preserves backwards compatibility.
2. **`Meal`** — add `saturatedFatG: Option[Double] = None`. Route handlers build `ujson.Obj` directly rather than serializing this case class, but the model must stay consistent with the database schema.

`BreakdownItem` is **not changed** — saturated fat is not tracked per ingredient.

### `services/ClaudeService.scala`
Two changes:

1. **Meal estimation prompt** — add `saturated_fat_g` to the expected JSON return:
   ```
   Return: { kcal, protein_g, carbs_g, fat_g, saturated_fat_g, fiber_g, description, water_ml, breakdown }
   ```
2. **Response parsing** — extract `saturated_fat_g` from Claude's JSON and populate `MacroEstimate.saturatedFatG`. If the field is absent or null, leave it as `None`.

### `services/AggregateService.scala`
Five touch points:

1. **`getTodayMacros` / `getMacrosForDate`** — add `COALESCE(SUM(saturated_fat_g), 0)` to the SELECT; include `saturatedFatG` in the returned `Macros(...)` constructor call.
2. **`getNDayAvgMacros`** — add `COALESCE(AVG(saturated_fat_g), 0)` to the SELECT; include `saturatedFatG` in the returned `Macros(...)` constructor call.
3. **`getProfileOrDefault`** — constructs a `UserProfile(...)` literal; add `targetSaturatedFatG = 20` to match the migration default and compile after the model change.
4. **`getProfile` SELECT** — has a raw SQL SELECT listing all profile columns explicitly; add `target_saturated_fat_g` to that column list.
5. **`profileFromRs`** — private helper that constructs `UserProfile(...)` from a `ResultSet`; add `rs.getInt("target_saturated_fat_g")` and pass it as `targetSaturatedFatG`.

Note: `PUT /api/profile` calls `AggregateService.getProfile(userId)` after the UPSERT to build its response — it does not consume the RETURNING result set directly. The RETURNING clause update is optional and harmless.

### `routes/MealRoutes.scala`

**`POST /api/meals`**
- INSERT: add `saturated_fat_g` column; bind NULL when `estimate.saturatedFatG` is `None`, bind the value when `Some`. Use `st.setNull(N, java.sql.Types.NUMERIC)` / `st.setDouble(N, v)` — do **not** use `getOrElse(0.0)` since 0 and NULL have different meanings (Claude omitted vs. Claude said 0)
- Response `"estimate"` sub-object: add `"saturatedFatG" -> estimate.saturatedFatG.map(ujson.Num(_)).getOrElse(ujson.Null)`
- Response `"meal"` sub-object: add `"saturatedFatG" -> estimate.saturatedFatG.map(ujson.Num(_)).getOrElse(ujson.Null)`

**`GET /api/meals/today`**
- SELECT: add `saturated_fat_g` to the column list
- Row-mapping loop: read using the same `wasNull()` guard pattern as all other nullable numerics:
  ```scala
  val satFatVal = rs.getDouble("saturated_fat_g")
  val satFatOpt = if rs.wasNull() then ujson.Null else ujson.Num(satFatVal)
  ```
  Include `"saturatedFatG" -> satFatOpt` in the `ujson.Obj`
- Totals object: include `"saturatedFatG" -> totals.saturatedFatG`

**`PUT /api/meals/:id`**
- UPDATE SET: add `saturated_fat_g = ?`
- Accept `saturatedFatG` from the request body; bind to the statement
- Response `ujson.Obj`: add `"saturatedFatG"` to keep response shape consistent

**`POST /api/meals/copy`** — four touch points:
1. Source SELECT: add `saturated_fat_g` to the column list
2. Source read: use `wasNull()` guard — `val satFatOpt = { val v = rs.getDouble("saturated_fat_g"); if rs.wasNull() then None else Some(v) }`
3. Destination INSERT: add `saturated_fat_g` column; bind NULL (`setNull`) when `None`, bind value when `Some`
4. Response `ujson.Obj`: include `"saturatedFatG"`

### `routes/ProfileRoutes.scala`

**`profileToJson` helper** — this private function serialises `UserProfile` to JSON for both GET and PUT responses. Add `"targetSaturatedFatG" -> profile.targetSaturatedFatG` to its output object.

**`PUT /api/profile`** — uses a positional `INSERT ... ON CONFLICT DO UPDATE` with `?` placeholders. Adding `target_saturated_fat_g` requires:
- Adding the column to the INSERT column list (currently 11 columns → 12)
- Adding a `?` to the VALUES list
- Adding `target_saturated_fat_g = EXCLUDED.target_saturated_fat_g` to the ON CONFLICT SET clause
- Adding `st.setInt(N, targetSaturatedFatG)` at the correct new position N
- Reading `targetSaturatedFatG` from the request body before the statement is built

## Frontend

### `src/api/client.ts`
- **`MacroEstimate`** — add `saturatedFatG: number | null` (nullable because Claude may not always return it)
- **`Macros`** — add `saturatedFatG: number`
- **`UserProfile`** — add `targetSaturatedFatG: number`

### `src/pages/LogPage.vue`
Add an SF chip to the macro estimate preview card. Guard with `v-if` since `saturatedFatG` may be null:
```html
<q-chip v-if="estimate.saturatedFatG != null">SF {{ estimate.saturatedFatG.toFixed(1) }}g</q-chip>
```

### `src/components/MacroChips.vue`
Add saturated fat to the chips array:
```typescript
{ label: 'Sat. Fat', value: totals.saturatedFatG, target: profile?.targetSaturatedFatG ?? 20 }
```

### `src/pages/ProfilePage.vue`
Add a "Saturated fat (g)" number input for `targetSaturatedFatG` alongside the other macro target inputs.

## Data Flow

```
User logs meal
  → Claude estimates: { kcal, protein_g, carbs_g, fat_g, saturated_fat_g, fiber_g, ... }
  → INSERT into meals (..., saturated_fat_g)  [NULL if Claude omitted it]
  → POST response includes saturatedFatG in both "meal" and "estimate" objects
  → LogPage shows SF chip (only when non-null)

GET /api/meals/today
  → SELECT ..., saturated_fat_g FROM meals
  → Row mapped with wasNull() guard → saturatedFatG in each meal ujson.Obj
  → Totals include saturatedFatG (SUM via AggregateService)
  → MacroChips renders SF chip with progress bar

User sets target in profile
  → PUT /api/profile with targetSaturatedFatG
  → Stored in user_profile.target_saturated_fat_g
  → profileToJson serialises it back in response
  → MacroChips uses targetSaturatedFatG for progress bar
```

## What Is Not Changing

- `BreakdownItem` — saturated fat not tracked per ingredient
- Insight prompts (`ClaudeService.dailyInsight`, `ClaudeService.weeklyInsight`) — out of scope; the new `saturatedFatG` field on `Macros` will compile and run correctly but simply will not appear in insight context strings
- `dailyInsightChat` system message — out of scope for same reason
- `TrendsRoutes` — out of scope
- Water tracking — unchanged

## Out of Scope

- Saturated fat in daily/weekly insight prompts
- Saturated fat in the trends summary page
- Per-item saturated fat in the meal breakdown dialog
- Saturated fat in `dailyInsightChat` system message
