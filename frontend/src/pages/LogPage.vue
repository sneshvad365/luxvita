<template>
  <q-page class="q-pa-md q-pb-xl">
    <div class="q-gutter-md">
      <div class="text-h6 text-weight-bold">Log Meal</div>

      <!-- Description input -->
      <q-input
        v-model="description"
        type="textarea"
        label="What did you eat?"
        hint="e.g. grilled chicken, cup of rice, roasted vegetables"
        outlined
        autogrow
      />

      <!-- Photo button -->
      <q-btn
        icon="photo_camera"
        :label="photo ? 'Photo selected ✓' : 'Add photo'"
        :color="photo ? 'positive' : 'grey-7'"
        outline
        @click="pickPhoto"
      />

      <!-- Estimate preview card -->
      <q-card v-if="log.pendingEstimate" class="bg-green-1">
        <q-card-section>
          <div class="text-subtitle2 text-weight-bold text-green-8 q-mb-sm">Estimated macros</div>
          <div class="text-caption text-grey-7 q-mb-sm">{{ log.pendingEstimate.description }}</div>
          <div class="row q-gutter-sm">
            <q-chip dense color="orange-2" text-color="orange-9">{{ log.pendingEstimate.kcal }} kcal</q-chip>
            <q-chip dense color="blue-2"   text-color="blue-9">P {{ log.pendingEstimate.proteinG.toFixed(1) }}g</q-chip>
            <q-chip dense color="amber-2"  text-color="amber-9">C {{ log.pendingEstimate.carbsG.toFixed(1) }}g</q-chip>
            <q-chip dense color="red-2"    text-color="red-9">F {{ log.pendingEstimate.fatG.toFixed(1) }}g</q-chip>
            <q-chip dense color="green-2"  text-color="green-9">Fi {{ log.pendingEstimate.fiberG.toFixed(1) }}g</q-chip>
          </div>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" @click="cancel" />
          <q-btn unelevated label="Confirm & save" color="primary" @click="confirmMeal" />
        </q-card-actions>
      </q-card>

      <q-banner v-if="log.error" class="bg-red-1 text-red-8 rounded-borders" dense>
        {{ log.error }}
      </q-banner>

      <q-btn
        v-if="!log.pendingEstimate"
        label="Estimate macros"
        color="primary"
        class="full-width"
        :loading="log.submitting"
        :disable="!description && !photo"
        unelevated
        @click="estimate"
      />

      <q-separator class="q-my-md" />

      <!-- Activity log -->
      <div class="text-h6 text-weight-bold">Log Activity</div>
      <q-input
        v-model="activityEntry"
        type="textarea"
        label="What did you do today?"
        hint="e.g. 1h gym chest and back, 9200 steps"
        outlined
        autogrow
      />
      <q-btn
        label="Log activity"
        color="secondary"
        class="full-width"
        :loading="activityLoading"
        :disable="!activityEntry"
        unelevated
        @click="submitActivity"
      />
      <q-banner v-if="activityDone" class="bg-green-1 text-green-8 rounded-borders" dense>
        Activity logged!
      </q-banner>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Camera, CameraResultType, CameraSource } from '@capacitor/camera'
import { useLogStore }   from 'src/stores/log'
import { useTodayStore } from 'src/stores/today'

const log   = useLogStore()
const today = useTodayStore()

const description     = ref('')
const photo           = ref<string | null>(null)
const activityEntry   = ref('')
const activityLoading = ref(false)
const activityDone    = ref(false)

async function pickPhoto() {
  try {
    const image = await Camera.getPhoto({
      quality:      90,
      allowEditing: false,
      resultType:   CameraResultType.Base64,
      source:       CameraSource.Prompt,
    })
    photo.value = image.base64String ?? null
  } catch {
    // user cancelled
  }
}

async function estimate() {
  await log.submitMeal(description.value || null, photo.value)
}

function cancel() {
  log.clearEstimate()
  description.value = ''
  photo.value       = null
}

// The meal was already saved when estimate was called — just clear the form
function confirmMeal() {
  today.fetchToday()
  cancel()
}

async function submitActivity() {
  if (!activityEntry.value) return
  activityLoading.value = true
  activityDone.value    = false
  try {
    await log.submitActivity(activityEntry.value)
    activityEntry.value = ''
    activityDone.value  = true
  } finally {
    activityLoading.value = false
  }
}
</script>
