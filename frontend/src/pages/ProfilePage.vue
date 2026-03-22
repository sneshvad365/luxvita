<template>
  <div v-if="navBlocking" class="nav-block-flash nav-block-top" />
  <div v-if="navBlocking" class="nav-block-flash nav-block-bottom" />
  <q-page class="q-pa-md q-pb-xl">
    <div class="q-gutter-md">
      <div class="row items-center justify-between">
        <div class="text-h6 text-weight-bold">Profile & targets</div>
        <q-btn flat icon="logout" label="Sign out" size="sm" color="grey-7" @click="logout" />
      </div>

      <q-inner-loading :showing="profileStore.loading" />

      <q-form @submit.prevent="save" class="q-gutter-md">

        <!-- Bio + Goal card -->
        <q-card>
          <q-card-section class="q-gutter-md">
            <q-input
              v-model="form.bio"
              type="textarea"
              label="About you (for AI context)"
              :placeholder="bioPlaceholder"
              outlined
              autogrow
              hint="Helps AI give personalised insights"
            />
            <q-select
              v-model="form.goal"
              :options="goalOptions"
              label="Goal"
              outlined
              dense
              emit-value
              map-options
            />
          </q-card-section>
        </q-card>

        <!-- Body stats card -->
        <q-card>
          <q-card-section>
            <div class="text-subtitle2 text-weight-bold q-mb-sm">Body stats</div>
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
                <q-input v-model.number="form.age" label="Age" outlined dense type="number" :min="10" :max="120" />
              </div>
              <div class="col-6">
                <q-select
                  v-model="form.activityLevel"
                  :options="activityOptions"
                  label="Activity level"
                  outlined dense emit-value map-options clearable
                />
              </div>
              <div class="col-6">
                <q-input v-model.number="form.baseWeightKg" label="Current weight (kg)" outlined dense type="number" step="0.1" />
              </div>
            </div>
          </q-card-section>
        </q-card>

        <!-- Daily targets card -->
        <q-card :class="{ 'targets-flash': targetsFlashing }">
          <q-card-section>
            <div class="text-subtitle2 text-weight-bold q-mb-sm">Daily targets</div>
            <div class="row q-col-gutter-sm">
              <div class="col-6"><q-input v-model.number="form.targetKcal"          label="Calories (kcal)" outlined dense type="number" /></div>
              <div class="col-6"><q-input v-model.number="form.targetProteinG"       label="Protein (g)"     outlined dense type="number" /></div>
              <div class="col-6"><q-input v-model.number="form.targetCarbsG"         label="Carbs (g)"       outlined dense type="number" /></div>
              <div class="col-6"><q-input v-model.number="form.targetFatG"           label="Fat (g)"         outlined dense type="number" /></div>
              <div class="col-6"><q-input v-model.number="form.targetFiberG"         label="Fiber (g)"       outlined dense type="number" /></div>
              <div class="col-6"><q-input v-model.number="form.targetSaturatedFatG"  label="Sat. fat (g)"    outlined dense type="number" /></div>
              <div class="col-6"><q-input v-model.number="form.targetWaterL"         label="Water (L)"       outlined dense type="number" step="0.1" /></div>
            </div>
          </q-card-section>
        </q-card>

<q-banner v-if="saved" class="bg-green-1 text-green-8 rounded-borders" dense>
          Profile saved!
        </q-banner>

        <q-banner v-if="!profileComplete" class="bg-blue-1 text-blue-9 rounded-borders" dense>
          Fill in all body stats above to unlock the app.
        </q-banner>

        <q-btn
          type="submit"
          label="Save profile"
          color="primary"
          class="full-width"
          :loading="saving"
          :disable="!profileComplete"
          unelevated
        />
      </q-form>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter }       from 'vue-router'
import { useProfileStore } from 'src/stores/profile'
import { useAuthStore }    from 'src/stores/auth'

const router       = useRouter()
const profileStore = useProfileStore()
const auth         = useAuthStore()
const saving          = ref(false)
const targetsFlashing = ref(false)
const saved        = ref(false)
const navBlocking     = ref(false)

watch(() => profileStore.navigationBlocked, (val) => {
  if (!val) return
  profileStore.navigationBlocked = false
  navBlocking.value = true
  setTimeout(() => { navBlocking.value = false }, 800)
})

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

const bioPlaceholder = `Lactose intolerant. Try to eat mostly whole foods but snack a lot in the evenings.
Struggle to hit protein on weekends. Don't cook much — mostly meal prep or
takeaway. Trying to cut down on processed food.`

const form = ref({
  bio:            '',
  goal:           'maintenance' as 'fat_loss' | 'muscle_gain' | 'maintenance',
  targetKcal:          null as number | null,
  targetProteinG:      null as number | null,
  targetCarbsG:        null as number | null,
  targetFatG:          null as number | null,
  targetFiberG:        null as number | null,
  targetSaturatedFatG: null as number | null,
  targetWaterL:        null as number | null,
  baseWeightKg:   null as number | null,
  goalWeightKg:   null as number | null,
  sex:            null as 'male' | 'female' | null,
  heightCm:       null as number | null,
  age:            null as number | null,
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
  kcal: number; proteinG: number; carbsG: number; fatG: number; fiberG: number; saturatedFatG: number
}

const suggestedMacros = computed<MacroSuggestion | null>(() => {
  const { sex, heightCm, age, activityLevel, baseWeightKg, goal } = form.value
  if (!sex || !heightCm || !age || !activityLevel || !baseWeightKg) return null
  if (age <= 0) return null
  const bmr = sex === 'male'
    ? 10 * baseWeightKg + 6.25 * heightCm - 5 * age + 5
    : 10 * baseWeightKg + 6.25 * heightCm - 5 * age - 161
  const kcal     = Math.round(bmr * (activityMultipliers[activityLevel] ?? 1.2) + (goalAdjustments[goal] ?? 0))
  const proteinG = Math.round(baseWeightKg * (proteinPerKg[goal] ?? 1.8))
  const fatG     = Math.round((kcal * (fatFraction[goal] ?? 0.28)) / 9)
  const carbsG   = Math.max(0, Math.round((kcal - proteinG * 4 - fatG * 9) / 4))
  const fiberG        = suggestedFiber[goal] ?? 25
  const saturatedFatG = Math.round((kcal * 0.10) / 9)
  return { kcal, proteinG, carbsG, fatG, fiberG, saturatedFatG }
})

const profileComplete = computed(() => {
  const f = form.value
  return !!f.sex && !!f.heightCm && !!f.age && !!f.activityLevel && !!f.baseWeightKg
})

watch(
  () => [form.value.sex, form.value.heightCm, form.value.age, form.value.activityLevel, form.value.baseWeightKg, form.value.goal],
  () => {
    const s = suggestedMacros.value
    if (!s) return
    form.value.targetKcal          = s.kcal
    form.value.targetProteinG      = s.proteinG
    form.value.targetCarbsG        = s.carbsG
    form.value.targetFatG          = s.fatG
    form.value.targetFiberG        = s.fiberG
    form.value.targetSaturatedFatG = s.saturatedFatG
    // flash the card
    targetsFlashing.value = false
    nextTick(() => { targetsFlashing.value = true })
    setTimeout(() => { targetsFlashing.value = false }, 700)
  }
)

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
    age:            p.birthDate
                    ? Math.floor((Date.now() - new Date(p.birthDate).getTime()) / (365.25 * 24 * 3600 * 1000))
                    : null,
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
      birthDate:    form.value.age
                      ? `${new Date().getFullYear() - form.value.age}-07-01`
                      : null,
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

<style scoped>
@keyframes targets-flash {
  0%   { background: #fff; }
  30%  { background: #EEF4FB; box-shadow: 0 0 0 3px rgba(27,79,138,0.18); }
  100% { background: #fff; box-shadow: none; }
}
.targets-flash {
  animation: targets-flash 0.7s ease;
}

@keyframes nav-block-flash {
  0%   { opacity: 0; }
  20%  { opacity: 1; }
  100% { opacity: 0; }
}
.nav-block-flash {
  position: fixed;
  left: 0;
  right: 0;
  height: 6px;
  background: #D42B2B;
  z-index: 9999;
  animation: nav-block-flash 0.8s ease forwards;
  pointer-events: none;
}
.nav-block-top    { top: 0; }
.nav-block-bottom { bottom: 0; }
</style>
