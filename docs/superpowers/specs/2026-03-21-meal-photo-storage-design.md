# Meal Photo Storage & Viewer Design

**Date:** 2026-03-21
**Status:** Approved

## Overview

When a user logs a meal with a photo, the compressed base64 image is currently sent to Claude for macro estimation and then discarded. This feature persists that photo in the database so users can view it later by tapping a camera icon on the meal entry.

## Scope

- Store the compressed base64 JPEG photo in the `meals` table when a meal is logged
- Serve it via a dedicated lazy-fetch endpoint `GET /api/meals/:id/photo`
- Show a camera icon button on meals that have a photo; tapping it fetches and displays the image in a dialog

## Database

**Migration: `V13__add_meal_photo.sql`** (V11 and V12 already exist)

```sql
ALTER TABLE meals ADD COLUMN photo_data TEXT;
```

- Nullable — meals without photos have `photo_data = NULL`
- `has_photo` boolean is retained as a lightweight flag for list rendering (no change)
- Only JPEG images are stored (the frontend always compresses to JPEG before sending); the data URI prefix `data:image/jpeg;base64,` is therefore hardcoded on the frontend

## Backend

### `Meal.scala`
Add `photoData: Option[String]` to the `Meal` case class and its uPickle `ReadWriter`. Note: route handlers build `ujson.Obj` values directly and do not use this case class for serialization — changes to the actual response JSON require editing the `ujson.Obj` construction sites.

### `MealRoutes.scala` — `POST /api/meals`
During the existing INSERT, write the base64 photo string into `photo_data`. The value is already in scope as the `photo` variable.

The manually-constructed `ujson.Obj` response (lines ~59–84) does **not** need `photoData` added — the photo viewer is only accessible from the Today screen (via `GET /api/meals/today` flow), not from the log confirmation card. This is intentional: the confirmation card shows macros, not the photo.

### `GET /api/meals/today`
**No changes.** `photo_data` is **excluded** from the SELECT and response to keep the payload lean. At ~400–500 KB per meal photo as base64, including all photos in every Today screen load would be impractical on a mobile connection.

### `GET /api/meals/:id/photo` *(new endpoint)*
Lazy-fetch endpoint for the photo viewer dialog. Returns:

```json
{ "photoData": "<base64 string>" }
```

- Protected by JWT middleware (same as all other routes)
- Queries `SELECT photo_data FROM meals WHERE id = ? AND user_id = ?` — the `user_id` filter ensures users cannot access each other's photos
- Returns 404 if meal not found or `photo_data` is NULL

### `POST /api/meals/copy`
The copy route does **not** copy `photo_data`. Copied meals always have `has_photo = false` and `photo_data = NULL`. This is intentional — the user is reusing macros, not the photo.

## Frontend

### Meal type
Do **not** add `photoData` to the existing `Meal` interface — `GET /api/meals/today` never returns it. Instead, define a separate type for the photo endpoint response:

```typescript
interface MealPhotoResponse { photoData: string }
```

Add a dedicated API helper `fetchMealPhoto(mealId: string): Promise<MealPhotoResponse>` in `client.ts` that calls `GET /api/meals/:id/photo`.

### `MealList.vue`
- Add a small camera icon button to each meal row, visible only when `meal.hasPhoto === true`
- `hasPhoto` is already returned by `GET /api/meals/today` and present on the `Meal` interface — no changes to the list response needed
- The defensive case where `has_photo = true` but `photo_data IS NULL` is handled at the endpoint level: `GET /api/meals/:id/photo` returns 404, and the dialog shows "Photo not available"
- Tapping the icon triggers a lazy fetch and opens the photo viewer dialog

### Photo viewer dialog
- Triggered by tapping the camera icon
- On open: calls `fetchMealPhoto(meal.id)`, shows a loading spinner
- On success: renders `<img :src="'data:image/jpeg;base64,' + photoData" />`
- On error (404 or network failure): shows a brief error message ("Photo not available")
- Close button to dismiss

## Data Flow

```
POST /api/meals (description + base64 photo)
  → ClaudeService.estimateMealMacros (photo sent to Claude, macros returned)
  → INSERT into meals (..., has_photo = true, photo_data = base64_string)
  → Return meal + macros (no photoData in response — not needed at log time)

GET /api/meals/today
  → SELECT id, description, has_photo, kcal, ... FROM meals  [photo_data excluded]
  → Return lean meal list; has_photo flag drives camera icon visibility

User taps camera icon on a meal (has_photo = true)
  → GET /api/meals/:id/photo
  → SELECT photo_data FROM meals WHERE id = ? AND user_id = ?
  → Return { photoData: "<base64>" }
  → <img src="data:image/jpeg;base64,{photoData}" />
```

## What Is Not Changing

- Claude API call: unchanged — photo is still sent as base64 in the message content
- Meal logging UX: unchanged
- `has_photo` boolean: retained as-is
- `GET /api/meals/today` response shape: unchanged (no photoData added)

## Out of Scope

- Removing a photo from a meal without deleting the meal (privacy use case — not needed for MVP)
- Support for non-JPEG formats (PNG, HEIC, etc.) — frontend always normalises to JPEG
- Full-resolution photo storage — the compressed version (~100–300 KB decoded) is sufficient
- `POST /api/meals` confirmation card showing the photo — the photo is already visible on LogPage before submission; no need to re-fetch it post-confirm
