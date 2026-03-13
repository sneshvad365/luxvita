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

        <!-- Targets -->
        <div class="text-subtitle2 text-weight-bold">Daily targets</div>
        <div class="row q-col-gutter-sm">
          <div class="col-6"><q-input v-model.number="form.targetKcal"     label="Calories (kcal)" outlined dense type="number" /></div>
          <div class="col-6"><q-input v-model.number="form.targetProteinG" label="Protein (g)"     outlined dense type="number" /></div>
          <div class="col-6"><q-input v-model.number="form.targetCarbsG"   label="Carbs (g)"       outlined dense type="number" /></div>
          <div class="col-6"><q-input v-model.number="form.targetFatG"     label="Fat (g)"         outlined dense type="number" /></div>
          <div class="col-6"><q-input v-model.number="form.targetFiberG"   label="Fiber (g)"       outlined dense type="number" /></div>
          <div class="col-6"><q-input v-model.number="form.targetWaterL"   label="Water (L)"       outlined dense type="number" step="0.1" /></div>
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
import { ref, onMounted, watch } from 'vue'
import { useRouter }       from 'vue-router'
import { useProfileStore } from 'src/stores/profile'
import { useAuthStore }    from 'src/stores/auth'

const router       = useRouter()
const profileStore = useProfileStore()
const auth         = useAuthStore()
const saving       = ref(false)
const saved        = ref(false)

const goalOptions = [
  { label: 'Fat loss',      value: 'fat_loss'     },
  { label: 'Muscle gain',   value: 'muscle_gain'  },
  { label: 'Maintenance',   value: 'maintenance'  },
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
  targetFiberG:   25,
  targetWaterL:   2.5,
  baseWeightKg:   null as number | null,
  goalWeightKg:   null as number | null,
})

// Populate form when profile loads
watch(() => profileStore.profile, (p) => {
  if (!p) return
  form.value = {
    bio:            p.bio            ?? '',
    goal:           p.goal,
    targetKcal:     p.targetKcal,
    targetProteinG: p.targetProteinG,
    targetCarbsG:   p.targetCarbsG,
    targetFatG:     p.targetFatG,
    targetFiberG:   p.targetFiberG,
    targetWaterL:   p.targetWaterL,
    baseWeightKg:   p.baseWeightKg,
    goalWeightKg:   p.goalWeightKg,
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
