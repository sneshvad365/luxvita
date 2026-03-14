#!/usr/bin/env python3
"""Seed a test user with 2 weeks of realistic data."""

import subprocess, uuid, json, datetime, random
import bcrypt

CONTAINER = "luxvita-postgres"
DB_USER   = "luxvita"
DB_NAME   = "luxvita"

def psql(sql):
    result = subprocess.run(
        ["docker", "exec", "-i", CONTAINER, "psql", "-U", DB_USER, "-d", DB_NAME, "-c", sql],
        capture_output=True, text=True
    )
    if result.returncode != 0:
        raise RuntimeError(f"psql error: {result.stderr}\nSQL: {sql[:200]}")
    return result.stdout

def psql_val(sql):
    result = subprocess.run(
        ["docker", "exec", "-i", CONTAINER, "psql", "-U", DB_USER, "-d", DB_NAME, "-t", "-A", "-c", sql],
        capture_output=True, text=True
    )
    if result.returncode != 0:
        raise RuntimeError(result.stderr)
    return result.stdout.strip().split('\n')[0].strip()

# ---------------------------------------------------------------------------
# User
# ---------------------------------------------------------------------------
USER_EMAIL    = "alex@luxvita.com"
USER_PASSWORD = "Test1234!"
pw_hash = bcrypt.hashpw(USER_PASSWORD.encode(), bcrypt.gensalt(10)).decode().replace('$2b$', '$2a$')

# Remove existing user if present
psql(f"DELETE FROM users WHERE email = '{USER_EMAIL}';")

user_id = psql_val(
    f"INSERT INTO users (email, password_hash) VALUES ('{USER_EMAIL}', '{pw_hash}') RETURNING id;"
)
print(f"Created user: {USER_EMAIL} / {USER_PASSWORD}  (id={user_id})")

# ---------------------------------------------------------------------------
# Profile
# ---------------------------------------------------------------------------
bio = ("Male, 32, 180cm, 82kg. Goal is fat loss while keeping muscle. "
       "Plays tennis twice a week (Tue & Thu, 1h each). Gym on weekends (2h sessions). "
       "Desk job, averages 11k steps daily. Tries to eat balanced but has cheat meals. "
       "Doesn't drink alcohol. Drinks protein shakes after workouts.")

psql(f"""
INSERT INTO user_profile
  (user_id, bio, goal, target_kcal, target_protein_g, target_carbs_g,
   target_fat_g, target_fiber_g, target_water_l, base_weight_kg, goal_weight_kg)
VALUES
  ('{user_id}'::uuid,
   $bio${bio}$bio$,
   'fat_loss', 2300, 170, 220, 75, 28, 2.8, 82.0, 76.0)
ON CONFLICT (user_id) DO NOTHING;
""")
print("Profile created.")

# ---------------------------------------------------------------------------
# Meal templates: (description, kcal, protein, carbs, fat, fiber, breakdown)
# breakdown = list of (item, kcal, protein, carbs, fat, fiber)
# ---------------------------------------------------------------------------
BREAKFASTS = [
    ("Oatmeal with banana and whey protein shake", 520, 38, 68, 11, 6, [
        ("Oatmeal 80g dry", 290, 10, 50,  5, 4),
        ("Banana medium",    90,  1, 23,  0, 1),
        ("Whey protein shake 30g", 140, 27,  5,  6, 1),
    ]),
    ("Greek yogurt with granola and mixed berries", 410, 28, 52, 9, 5, [
        ("Greek yogurt 200g", 190, 20, 8,  5, 0),
        ("Granola 50g",       190,  5, 30,  7, 2),
        ("Mixed berries 100g", 30,  3, 14, -2, 3),
    ]),
    ("3 scrambled eggs, whole wheat toast, orange juice", 490, 33, 44, 18, 4, [
        ("3 scrambled eggs",        210, 18,  2, 14, 0),
        ("Whole wheat toast 2 slices", 160,  8, 28,  2, 3),
        ("Orange juice 200ml",      120,  2, 26,  0, 1),
        ("Butter 5g",                36,  0,  0,  4, 0),
    ]),
    ("Protein pancakes with maple syrup and strawberries", 540, 36, 62, 14, 4, [
        ("Protein pancake mix 80g", 320, 28, 38,  8, 2),
        ("Maple syrup 20ml",        60,  0, 15,  0, 0),
        ("Strawberries 100g",        32,  1,  8,  0, 2),
        ("Egg 1",                   78,  6,  0,  5, 0),
        ("Milk 100ml",               50,  3,  5,  2, 0),
    ]),
    ("Avocado toast with poached eggs and cherry tomatoes", 480, 24, 38, 26, 7, [
        ("Whole grain bread 2 slices", 160, 8, 28,  2, 3),
        ("Avocado half",              160, 2,  4, 15, 3),
        ("2 poached eggs",            140, 12,  1, 10, 0),
        ("Cherry tomatoes 80g",        20, 1,  4,  0, 1),
        ("Olive oil drizzle 5ml",       44, 0,  0,  5, 0),
    ]),
    ("Overnight oats with chia seeds and blueberries", 420, 18, 58, 12, 8, [
        ("Rolled oats 70g",       260,  8, 44,  5, 4),
        ("Almond milk 200ml",      30,  1,  2,  2, 0),
        ("Chia seeds 15g",         72,  2,  5,  5, 4),
        ("Blueberries 100g",        57,  1, 14,  0, 2),
        ("Honey 10g",               30,  0,  8,  0, 0),
    ]),
    ("Smoothie bowl: banana, spinach, protein powder, almond butter", 490, 34, 52, 16, 6, [
        ("Banana large",            120,  1, 31,  0, 2),
        ("Spinach 50g",              11,  1,  2,  0, 1),
        ("Protein powder 30g",      120, 24,  4,  2, 0),
        ("Almond butter 20g",       120,  4,  4, 10, 1),
        ("Almond milk 150ml",        25,  1,  1,  2, 0),
        ("Granola topping 20g",      94,  3, 14,  3, 1),
    ]),
]

LUNCHES = [
    ("Grilled chicken breast 200g, cup of basmati rice, roasted broccoli", 580, 52, 58, 10, 6, [
        ("Grilled chicken breast 200g", 330, 46,  0,  7, 0),
        ("Basmati rice 1 cup cooked",   200,  4, 44,  0, 1),
        ("Roasted broccoli 150g",        50,  4, 10,  1, 4),
        ("Olive oil 5ml",                44,  0,  0,  5, 0),
    ]),
    ("Tuna salad sandwich on whole wheat, apple, handful of nuts", 510, 38, 48, 16, 7, [
        ("Whole wheat bread 2 slices", 160,  8, 28,  2, 3),
        ("Tuna in water 150g",         165, 36,  0,  1, 0),
        ("Light mayo 15g",              50,  0,  1,  5, 0),
        ("Apple medium",                80,  0, 21,  0, 3),
        ("Mixed nuts 20g",              55,  2,  4,  5, 1),
    ]),
    ("Turkey and veggie wrap with hummus", 530, 36, 52, 18, 6, [
        ("Whole wheat wrap",         180,  6, 32,  4, 2),
        ("Turkey slices 120g",       180, 26,  2,  4, 0),
        ("Hummus 40g",                94,  4,  8,  6, 2),
        ("Mixed vegetables 100g",     30,  2,  6,  0, 2),
        ("Feta cheese 20g",           56,  4,  0,  4, 0),
    ]),
    ("Salmon fillet 180g, sweet potato, steamed green beans", 560, 48, 44, 16, 7, [
        ("Salmon fillet 180g",       360, 40,  0, 22, 0),
        ("Sweet potato 150g",        130,  2, 30,  0, 4),
        ("Green beans 120g",          38,  2,  8,  0, 3),
        ("Olive oil 5ml",             44,  0,  0,  5, 0),
        ("Lemon + herbs",             10,  1,  2,  0, 0),
    ]),
    ("Chicken Caesar salad with whole grain croutons", 470, 42, 28, 22, 4, [
        ("Grilled chicken 150g",     248, 34,  0,  6, 0),
        ("Romaine lettuce 100g",      17,  1,  3,  0, 2),
        ("Caesar dressing 30ml",     100,  1,  3, 10, 0),
        ("Parmesan 15g",              60,  4,  0,  5, 0),
        ("Whole grain croutons 30g",  90,  3, 16,  2, 1),
    ]),
    ("Lentil soup with sourdough bread", 490, 24, 68, 10, 14, [
        ("Lentil soup 350ml",        290, 18, 46,  4, 12),
        ("Sourdough bread 2 slices", 160,  6, 32,  2,  2),
        ("Olive oil drizzle",         44,  0,  0,  5,  0),
    ]),
    ("Grilled beef burger 150g on brioche bun, salad, no fries", 680, 46, 48, 28, 3, [
        ("Beef patty 150g",          350, 32,  0, 24, 0),
        ("Brioche bun",              220,  8, 36,  4, 1),
        ("Lettuce, tomato, onion",    30,  1,  6,  0, 1),
        ("Ketchup 15g",               18,  0,  4,  0, 0),
        ("Mustard 10g",               10,  1,  1,  0, 0),
    ]),
]

DINNERS_HEALTHY = [
    ("Baked salmon 200g, quinoa, steamed asparagus", 570, 52, 42, 18, 7, [
        ("Baked salmon 200g",   400, 44,  0, 24, 0),
        ("Quinoa 80g dry",      120,  4, 22,  2, 3),
        ("Asparagus 150g",       35,  4,  6,  0, 3),
        ("Lemon + olive oil",    44,  0,  2,  5, 0),
    ]),
    ("Chicken stir fry with vegetables and brown rice", 600, 50, 58, 16, 8, [
        ("Chicken breast 180g",         248, 40,  0,  6, 0),
        ("Brown rice 80g dry",          280,  6, 58,  2, 3),
        ("Mixed stir fry vegetables",    50,  3,  8,  0, 4),
        ("Soy sauce + sesame oil",       44,  1,  4,  3, 0),
    ]),
    ("Turkey meatballs with whole wheat pasta and tomato sauce", 620, 48, 64, 14, 6, [
        ("Turkey meatballs 200g",       280, 34,  4, 12, 0),
        ("Whole wheat pasta 80g dry",   270,  9, 52,  2, 5),
        ("Tomato sauce 150ml",           60,  2, 12,  0, 2),
        ("Parmesan 15g",                 60,  4,  0,  5, 0),
    ]),
    ("Grilled lean beef steak 200g, roasted potatoes, side salad", 680, 56, 42, 24, 5, [
        ("Lean beef steak 200g",         440, 48,  0, 26, 0),
        ("Roasted potatoes 150g",        160,  4, 34,  2, 3),
        ("Side salad with dressing",      50,  1,  4,  3, 1),
        ("Olive oil 5ml",                 44,  0,  0,  5, 0),
    ]),
    ("Baked cod 200g, couscous, roasted Mediterranean vegetables", 520, 46, 50, 12, 8, [
        ("Baked cod 200g",                  200, 38,  0,  2, 0),
        ("Couscous 80g dry",                280,  8, 56,  0, 3),
        ("Roasted Mediterranean veg 200g",    50,  3, 10,  0, 4),
        ("Olive oil 8ml",                     71,  0,  0,  8, 0),
        ("Herbs and lemon",                    5,  0,  2,  0, 0),
    ]),
    ("Chicken tikka masala with brown rice (home cooked, lighter version)", 640, 52, 60, 16, 5, [
        ("Chicken breast 180g",       248, 40,  0,  6, 0),
        ("Tikka masala sauce 150ml",  160,  4, 12,  8, 2),
        ("Brown rice 80g dry",        230,  6, 48,  2, 3),
        ("Natural yogurt 50g",         34,  4,  4,  1, 0),
    ]),
    ("Shrimp and vegetable curry with basmati rice", 580, 44, 62, 14, 7, [
        ("Shrimp 200g",              200, 38,  0,  2, 0),
        ("Coconut curry sauce 100ml", 90,  2,  6,  8, 1),
        ("Mixed vegetables 150g",     50,  3, 10,  0, 4),
        ("Basmati rice 80g dry",     240,  4, 52,  0, 1),
    ]),
]

DINNERS_CHEAT = [
    ("Pepperoni pizza, 3 slices, can of Coke", 980, 36, 98, 42, 4, [
        ("Pepperoni pizza 3 slices", 830, 36, 78, 42, 3),
        ("Can of Coke 330ml",        139,  0, 35,  0, 0),
    ]),
    ("Chicken burger with fries and mayo dip (restaurant)", 920, 42, 88, 38, 5, [
        ("Chicken burger",         480, 28, 42, 22, 2),
        ("Fries large",            360, 10, 46, 16, 4),
        ("Mayo dip 30g",            80,  0,  2,  8, 0),
    ]),
    ("Pasta carbonara full portion with garlic bread", 880, 34, 96, 36, 3, [
        ("Pasta carbonara 400g",   680, 28, 82, 26, 2),
        ("Garlic bread 2 pieces",  200,  6, 28, 10, 1),
    ]),
    ("Beef kebab wrap with chips, garlic sauce", 950, 44, 82, 44, 4, [
        ("Beef kebab wrap",       580, 34, 48, 24, 2),
        ("Chips medium portion",  310,  4, 44, 14, 3),
        ("Garlic sauce 30g",       60,  0,  2,  6, 0),
    ]),
    ("Sushi takeout: 2 rolls + edamame + miso soup", 720, 32, 96, 18, 6, [
        ("Salmon avocado roll 8pc",  320, 16, 44,  8, 2),
        ("Spicy tuna roll 8pc",      280, 14, 42,  6, 2),
        ("Edamame 100g",              94,  9,  8,  4, 5),
        ("Miso soup",                 26,  2,  4,  0, 1),
    ]),
]

# ---------------------------------------------------------------------------
# Activity templates per day type
# ---------------------------------------------------------------------------
def activity_for(day: datetime.date) -> tuple:
    """Returns (entry, type, duration_min, intensity, steps, mood)"""
    dow = day.weekday()  # 0=Mon, 6=Sun
    steps = random.randint(9800, 12500)

    if dow == 1 or dow == 3:  # Tue, Thu: tennis
        moods = ["felt sharp", "played well", "good session", "legs felt heavy but pushed through"]
        mood = random.choice(moods)
        return (
            f"Tennis 1h, {mood}, {steps} steps",
            "sport", 60, "moderate", steps,
            "energized" if "well" in mood or "sharp" in mood else "good"
        )
    elif dow == 5:  # Sat: gym
        moods = ["great pump", "felt strong", "solid session", "a bit tired but pushed"]
        focus = random.choice(["chest and back", "legs and shoulders", "push day", "pull day"])
        return (
            f"Gym 2h {focus}, {random.choice(moods)}, {steps} steps",
            "gym", 120, "high", steps, "energized"
        )
    elif dow == 6:  # Sun: gym
        moods = ["good session", "felt strong", "bit sore from yesterday but solid"]
        focus = random.choice(["legs", "chest and triceps", "back and biceps", "full body"])
        return (
            f"Gym 2h {focus}, {random.choice(moods)}, {steps} steps",
            "gym", 120, "high", steps, "good"
        )
    else:  # Mon, Wed, Fri: rest + steps
        notes = [
            f"Rest day, {steps} steps walking to/from work",
            f"Active commute, {steps} steps, took stairs all day",
            f"Light walk at lunch, {steps} steps total",
        ]
        return (
            random.choice(notes),
            "walk", 30, "low", steps, "relaxed"
        )

# ---------------------------------------------------------------------------
# Build 14 days
# ---------------------------------------------------------------------------
start = datetime.date(2026, 3, 1)

for day_offset in range(14):
    day = start + datetime.timedelta(days=day_offset)
    date_str = day.isoformat()
    dow = day.weekday()

    # --- Meals ---
    breakfast = random.choice(BREAKFASTS)
    lunch     = random.choice(LUNCHES)

    # Cheat dinner: ~25% chance on weekdays, ~50% on weekends (Sat/Sun)
    cheat_prob = 0.50 if dow in (5, 6) else 0.20
    dinner = random.choice(DINNERS_CHEAT if random.random() < cheat_prob else DINNERS_HEALTHY)

    meals = [
        (breakfast, "07:30"),
        (lunch,     "12:45"),
        (dinner,    "19:30"),
    ]

    for (desc, kcal, prot, carbs, fat, fiber, items), time_str in meals:
        dt = f"{date_str}T{time_str}:00+00:00"
        breakdown = json.dumps([
            {"item": i, "kcal": k, "protein_g": p, "carbs_g": c, "fat_g": f, "fiber_g": fi}
            for i, k, p, c, f, fi in items
        ])
        psql(f"""
INSERT INTO meals (user_id, logged_at, description, has_photo, kcal, protein_g, carbs_g, fat_g, fiber_g, breakdown)
VALUES ('{user_id}'::uuid, '{dt}'::timestamptz, $d${desc}$d$, false, {kcal}, {prot}, {carbs}, {fat}, {fiber}, $b${breakdown}$b$::jsonb);
""")

    # --- Water ---
    water_entries = [
        (0.5, "08:00"), (0.5, "11:00"), (0.5, "14:00"),
        (0.5, "17:00"), (0.5, "20:00"), (0.3, "22:00"),
    ]
    # Extra water on workout days
    if dow in (1, 3, 5, 6):
        water_entries.append((0.6, "16:00"))

    for amount, time_str in water_entries:
        dt = f"{date_str}T{time_str}:00+00:00"
        psql(f"""
INSERT INTO water_logs (user_id, logged_at, amount_l)
VALUES ('{user_id}'::uuid, '{dt}'::timestamptz, {amount});
""")

    # --- Activity ---
    entry, act_type, duration, intensity, steps, mood = activity_for(day)
    parsed = json.dumps({
        "type": act_type,
        "duration_min": duration,
        "intensity": intensity,
        "steps": steps,
        "mood": mood,
        "notes": None
    })
    psql(f"""
INSERT INTO activity_logs (user_id, logged_at, entry, parsed)
VALUES ('{user_id}'::uuid, '{date_str}T20:00:00+00:00'::timestamptz,
        $e${entry}$e$, '{parsed}'::jsonb);
""")

    print(f"  {date_str} ({day.strftime('%a')}): meals + water + activity inserted")

# ---------------------------------------------------------------------------
# Daily insights (one per day — realistic coaching feedback)
# ---------------------------------------------------------------------------
daily_insights = [
    # Mar 1 Sun - gym day, cheat possible
    ("Great gym session today! Make sure to hit your protein target tonight to support muscle recovery after a 2-hour session.", "recovery"),
    # Mar 2 Mon
    ("Your calorie intake was on point today. Keep prioritizing protein at breakfast to maintain muscle during your fat loss phase.", "protein"),
    # Mar 3 Tue - tennis
    ("Tennis is excellent cardio — you burned roughly 400 extra kcal today. Consider a small carb-rich snack post-match to replenish glycogen.", "timing"),
    # Mar 4 Wed
    ("Fiber intake was a bit low today at around 18g. Adding a handful of legumes or an extra vegetable serving would close that gap.", "fiber"),
    # Mar 5 Thu - tennis
    ("Good consistency this week with two tennis sessions. Your 7-day protein average is tracking well — keep it up through the weekend.", "protein"),
    # Mar 6 Fri
    ("Hydration was solid today. With a big gym day tomorrow, aim to start the session already well-hydrated by drinking an extra 500ml tonight.", "hydration"),
    # Mar 7 Sat - gym
    ("Strong 2-hour gym session! Calories ran slightly high today — if dinner was a cheat meal, balance it with a leaner Sunday to stay on track for the week.", "weight"),
    # Mar 8 Sun - gym
    ("Back-to-back gym days are tough — your protein intake today will be critical for recovery. Aim for 50g+ at dinner tonight.", "recovery"),
    # Mar 9 Mon
    ("You're averaging 11k steps daily which adds meaningful NEAT to your deficit. Your 7-day kcal average is right on target.", "weight"),
    # Mar 10 Tue - tennis
    ("Tennis session logged! Your step count combined with tennis gives you a strong active day. Make sure dinner has at least 40g of protein.", "protein"),
    # Mar 11 Wed
    ("Mid-week check: fiber is consistently below target most days. Try swapping white rice for lentils or adding a side salad at lunch.", "fiber"),
    # Mar 12 Thu - tennis
    ("Two tennis sessions this week already — great work. Your weekly calorie balance looks solid so far with the deficit trending as planned.", "weight"),
    # Mar 13 Fri
    ("Heading into the weekend, your protein for the week is slightly under target. Prioritize a protein-rich breakfast both Saturday and Sunday.", "protein"),
    # Mar 14 Sat - gym
    ("Excellent 2-hour gym session to end the week! Total weekly activity is impressive. Recovery nutrition tonight will set you up well for next week.", "recovery"),
]

for day_offset, (insight_text, insight_type) in enumerate(daily_insights):
    day = start + datetime.timedelta(days=day_offset)
    psql(f"""
INSERT INTO daily_insights (user_id, date, insight, type)
VALUES ('{user_id}'::uuid, '{day.isoformat()}'::date, $i${insight_text}$i$, '{insight_type}')
ON CONFLICT (user_id, date) DO UPDATE SET insight = EXCLUDED.insight, type = EXCLUDED.type;
""")

print("Daily insights inserted.")

# ---------------------------------------------------------------------------
# Weekly insights (for Sunday Mar 1 and Sunday Mar 8)
# ---------------------------------------------------------------------------
weekly = {
    "2026-03-01": [
        ("Your weekly protein average hit 158g against a 170g target — you're close, but weekend cheat meals are pulling it down. Try adding a protein shake on Saturday evenings after your gym session.", "protein"),
        ("You trained 4 times this week and averaged 11,200 steps daily — excellent activity consistency. Your estimated weekly deficit is around 2,800 kcal, putting you on track to lose roughly 0.4kg this week.", "weight"),
        ("Fiber averaged only 17g per day this week, well below your 28g target. Swapping one refined carb serving per day for legumes, oats, or vegetables would significantly close this gap.", "fiber"),
    ],
    "2026-03-08": [
        ("Protein improved to 162g average this week — nearly at target. The post-workout shakes are clearly helping. Maintain this on weekends when cheat meals tend to lower your daily protein.", "protein"),
        ("Weight trend is moving in the right direction. Your average daily calorie intake across both weeks is 2,190 kcal against a 2,300 target, creating a consistent deficit without being too aggressive.", "weight"),
        ("Hydration has been strong on training days but drops on rest days. Aim for your 2.8L target every day, not just when you're active — consistent hydration supports fat metabolism and energy levels.", "hydration"),
    ],
}

for week_date, insights in weekly.items():
    for rank, (insight_text, insight_type) in enumerate(insights, start=1):
        psql(f"""
INSERT INTO weekly_insights (user_id, week_date, insight, type, rank)
VALUES ('{user_id}'::uuid, '{week_date}'::date, $i${insight_text}$i$, '{insight_type}', {rank})
ON CONFLICT (user_id, week_date, rank) DO UPDATE SET insight = EXCLUDED.insight, type = EXCLUDED.type;
""")

print("Weekly insights inserted.")
print()
print("=== DONE ===")
print(f"Login: {USER_EMAIL} / {USER_PASSWORD}")
