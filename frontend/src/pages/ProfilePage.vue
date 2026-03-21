<template>
  <q-page class="q-pa-md q-pb-xl">
    <div class="q-gutter-md">
      <div class="row items-center justify-between">
        <div class="text-h6 text-weight-bold">Profile & targets</div>
        <q-btn flat icon="logout" label="Sign out" size="sm" color="grey-7" @click="logout" />
      </div>

      <q-inner-loading :showing="profileStore.loading" />

      <q-form @submit.prevent="save" class="q-gutter-md">
        <!-- Bio -->
        <q-input
          v-model="form.bio"
          type="textarea"
          label="About you (for AI context)"
          :placeholder="bioPlaceholder"
          outlined
          autogrow
          hint="Helps AI give personalised insights"
        />

        <!-- Goal -->
        <q-select
          v-model="form.goal"
          :options="goalOptions"
          label="Goal"
          outlined
          dense
          emit-value
          map-options
        />

        <!-- Body stats for Mifflin-St Jeor -->
        <div class="text-subtitle2 text-weight-bold">Body stats</div>
        <div class="row q-col-gutter-sm">
          <div class="col-6">
            <q-select
              v-model="form.sex"
              :options="sexOptions"
              label="Sex"
              outlined dense emit-value map-options clearable
            />
          </div>
          <div class="col-6">
            <q-input v-model.number="form.heightCm" label="Height (cm)" outlined dense type="number" />
          </div>
          <div class="col-6">
            <q-input v-model="form.birthDate" label="Date of birth" outlined dense type="date" />
          </div>
          <div class="col-6">
            <q-select
              v-model="form.activityLevel"
              :options="activityOptions"
              label="Activity level"
              outlined dense emit-value map-options clearable
            />
          </div>
        </div>

        <!-- Targets -->
        <div class="text-subtitle2 text-weight-bold">Daily targets</div>
        <div v-if="suggestedMacros" class="text-caption text-grey-7 q-mb-xs">
          Suggested for your goal:
          <strong>{{ suggestedMacros.kcal }} kcal</strong> ·
          <strong>{{ suggestedMacros.proteinG }}g</strong> protein ·
          <strong>{{ suggestedMacros.carbsG }}g</strong> carbs ·
          <strong>{{ suggestedMacros.fatG }}g</strong> fat ·
          <strong>{{ suggestedMacros.fiberG }}g</strong> fiber
          <q-btn flat dense size="xs" label="apply all" color="primary" class="q-ml-xs" @click="applyMacros" />
        </div>
        <div class="row q-col-gutter-sm">
          <div class="col-6"><q-input v-model.number="form.targetKcal"     label="Calories (kcal)" outlined dense type="number" /></div>
          <div class="col-6"><q-input v-model.number="form.targetProteinG" label="Protein (g)"     outlined dense type="number" /></div>
          <div class="col-6"><q-input v-model.number="form.targetCarbsG"   label="Carbs (g)"       outlined dense type="number" /></div>
          <div class="col-6"><q-input v-model.number="form.targetFatG"     label="Fat (g)"         outlined dense type="number" /></div>
          <div class="col-6"><q-input v-model.number="form.targetFiberG"        label="Fiber (g)"       outlined dense type="number" /></div>
          <div class="col-6"><q-input v-model.number="form.targetSaturatedFatG" label="Sat. fat (g)"    outlined dense type="number" /></div>
          <div class="col-6"><q-input v-model.number="form.targetWaterL"        label="Water (L)"       outlined dense type="number" step="0.1" /></div>
        </div>

        <!-- Weight goals -->
        <div class="text-subtitle2 text-weight-bold">Weight</div>
        <div class="row q-col-gutter-sm">
          <div class="col-6"><q-input v-model.number="form.baseWeightKg" label="Starting weight (kg)" outlined dense type="number" step="0.1" /></div>
          <div class="col-6"><q-input v-model.number="form.goalWeightKg" label="Goal weight (kg)"     outlined dense type="number" step="0.1" /></div>
        </div>

        <q-banner v-if="saved" class="bg-green-1 text-green-8 rounded-borders" dense>
          Profile saved!
        </q-banner>

        <q-btn
          type="submit"
          label="Save profile"
          color="primary"
          class="full-width"
          :loading="saving"
          unelevated
        />
      </q-form>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter }       from 'vue-router'
import { useProfileStore } from 'src/stores/profile'
import { useAuthStore }    from 'src/stores/auth'

const router       = useRouter()
const profileStore = useProfileStore()
const auth         = useAuthStore()
const saving       = ref(false)
const saved        = ref(false)

const goalOptions = [
  { label: 'Fat loss',    value: 'fat_loss'    },
  { label: 'Muscle gain', value: 'muscle_gain' },
  { label: 'Maintenance', value: 'maintenance' },
]

const sexOptions = [
  { label: 'Male',   value: 'male'   },
  { label: 'Female', value: 'female' },
]

const activityOptions = [
  { label: 'Sedentary (desk job, little exercise)',       value: 'sedentary'          },
  { label: 'Lightly active (1–3 days/week)',              value: 'lightly_active'     },
  { label: 'Moderately active (3–5 days/week)',           value: 'moderately_active'  },
  { label: 'Very active (6–7 days/week)',                 value: 'very_active'        },
  { label: 'Extra active (physical job or twice/day)',    value: 'extra_active'       },
]

const bioPlaceholder = `Male, 33, 180cm, 83kg. Goal is to lose fat while keeping muscle.
Train 4x a week — gym (chest/back, legs) and tennis on weekends.
Desk job, mostly sedentary outside workouts. Tend to snack a lot
in the evening. Lactose intolerant. Trying to eat more protein
but struggle on weekends.`

const form = ref({
  bio:            '',
  goal:           'maintenance' as 'fat_loss' | 'muscle_gain' | 'maintenance',
  targetKcal:     2000,
  targetProteinG: 150,
  targetCarbsG:   200,
  targetFatG:     70,
  targetFiberG:        25,
  targetSaturatedFatG: 20,
  targetWaterL:        2.5,
  baseWeightKg:   null as number | null,
  goalWeightKg:   null as number | null,
  sex:            null as 'male' | 'female' | null,
  heightCm:       null as number | null,
  birthDate:      null as string | null,
  activityLevel:  null as string | null,
})

const activityMultipliers: Record<string, number> = {
  sedentary:         1.2,
  lightly_active:    1.375,
  moderately_active: 1.55,
  very_active:       1.725,
  extra_active:      1.9,
}

const goalAdjustments: Record<string, number> = {
  fat_loss:    -400,
  maintenance:    0,
  muscle_gain: +250,
}

// g of protein per kg of bodyweight by goal
const proteinPerKg: Record<string, number> = {
  fat_loss:    2.2,
  maintenance: 1.8,
  muscle_gain: 2.0,
}

// fat as fraction of total calories by goal
const fatFraction: Record<string, number> = {
  fat_loss:    0.25,
  maintenance: 0.30,
  muscle_gain: 0.25,
}

const suggestedFiber: Record<string, number> = {
  fat_loss:    30,
  maintenance: 25,
  muscle_gain: 28,
}

interface MacroSuggestion {
  kcal: number; proteinG: number; carbsG: number; fatG: number; fiberG: number
}

const suggestedMacros = computed<MacroSuggestion | null>(() => {
  const { sex, heightCm, birthDate, activityLevel, baseWeightKg, goal } = form.value
  if (!sex || !heightCm || !birthDate || !activityLevel || !baseWeightKg) return null
  const age = Math.floor((Date.now() - new Date(birthDate).getTime()) / (365.25 * 24 * 3600 * 1000))
  if (age <= 0) return null
  const bmr = sex === 'male'
    ? 10 * baseWeightKg + 6.25 * heightCm - 5 * age + 5
    : 10 * baseWeightKg + 6.25 * heightCm - 5 * age - 161
  const kcal     = Math.round(bmr * (activityMultipliers[activityLevel] ?? 1.2) + (goalAdjustments[goal] ?? 0))
  const proteinG = Math.round(baseWeightKg * (proteinPerKg[goal] ?? 1.8))
  const fatG     = Math.round((kcal * (fatFraction[goal] ?? 0.28)) / 9)
  const carbsG   = Math.max(0, Math.round((kcal - proteinG * 4 - fatG * 9) / 4))
  const fiberG   = suggestedFiber[goal] ?? 25
  return { kcal, proteinG, carbsG, fatG, fiberG }
})

function applyMacros() {
  if (!suggestedMacros.value) return
  const s = suggestedMacros.value
  form.value.targetKcal     = s.kcal
  form.value.targetProteinG = s.proteinG
  form.value.targetCarbsG   = s.carbsG
  form.value.targetFatG     = s.fatG
  form.value.targetFiberG   = s.fiberG
}

watch(() => profileStore.profile, (p) => {
  if (!p) return
  form.value = {
    bio:            p.bio ?? '',
    goal:           p.goal as 'fat_loss' | 'muscle_gain' | 'maintenance',
    targetKcal:     p.targetKcal,
    targetProteinG: p.targetProteinG,
    targetCarbsG:   p.targetCarbsG,
    targetFatG:     p.targetFatG,
    targetFiberG:        p.targetFiberG,
    targetSaturatedFatG: p.targetSaturatedFatG,
    targetWaterL:        p.targetWaterL,
    baseWeightKg:   p.baseWeightKg,
    goalWeightKg:   p.goalWeightKg,
    sex:            p.sex,
    heightCm:       p.heightCm,
    birthDate:      p.birthDate,
    activityLevel:  p.activityLevel,
  }
}, { immediate: true })

onMounted(() => profileStore.fetch())

async function save() {
  saving.value = true
  saved.value  = false
  try {
    await profileStore.save({
      ...form.value,
      bio:          form.value.bio || null,
      baseWeightKg: form.value.baseWeightKg ?? null,
      goalWeightKg: form.value.goalWeightKg ?? null,
      sex:          form.value.sex ?? null,
      heightCm:     form.value.heightCm ?? null,
      birthDate:    form.value.birthDate ?? null,
      activityLevel: form.value.activityLevel ?? null,
    })
    saved.value = true
  } finally {
    saving.value = false
  }
}

function logout() {
  auth.logout()
  void router.push('/login')
}
</script>
