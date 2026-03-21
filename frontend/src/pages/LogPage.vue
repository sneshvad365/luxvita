<template>
  <q-page class="q-pa-md q-pb-xl">
    <div class="q-gutter-md">
      <div class="text-h6 text-weight-bold">Log Meal</div>

      <!-- Description input -->
      <q-input
        v-model="description"
        type="textarea"
        placeholder="e.g. 200g grilled chicken, cup of basmati rice, roasted vegetables"
        outlined
        autogrow
      />

      <!-- Photo button + tip -->
      <div class="row items-start q-gutter-sm">
        <q-btn
          icon="photo_camera"
          :label="photo ? 'Photo selected ✓' : 'Add photo'"
          :color="photo ? 'positive' : 'grey-7'"
          outline
          @click="pickPhoto"
        />
        <div class="text-caption text-grey-6 col" style="line-height:1.4">
          Include a fork, coin, or ruler for accurate portion size. You can also photograph a
          <strong>barcode</strong> or <strong>nutrition label</strong>.
        </div>
      </div>
      <input ref="fileInput"        type="file" accept="image/*"                    style="display:none" @change="onFileChange" />
      <input ref="fileInputCamera" type="file" accept="image/*" capture="environment" style="display:none" @change="onFileChange" />

      <!-- Photo source dialog -->
      <q-dialog v-model="photoDialog">
        <q-card style="min-width:240px">
          <q-card-section class="q-gutter-sm">
            <q-btn unelevated color="primary"  icon="photo_camera"  label="Take photo"       class="full-width" @click="openCamera"  />
            <q-btn unelevated color="grey-7"   icon="photo_library" label="Choose from gallery" class="full-width" @click="openGallery" />
          </q-card-section>
        </q-card>
      </q-dialog>

      <!-- Water quick-add -->
      <div class="row items-center q-gutter-sm">
        <q-icon name="water_drop" color="cyan-7" size="sm" />
        <span class="text-caption text-grey-7">Add water:</span>
        <q-btn
          v-for="amt in quickAmounts" :key="amt.label"
          dense unelevated outline color="cyan-7" size="sm"
          :label="amt.label"
          :loading="quickAdding === amt.l"
          @click="quickAdd(amt.l)"
        />
      </div>
      <q-banner v-if="drinkDone" class="bg-cyan-1 text-cyan-9 rounded-borders" dense>
        Logged {{ (lastDrinkL * 1000).toFixed(0) }}ml
      </q-banner>

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
            <q-chip v-if="log.pendingEstimate.saturatedFatG != null" dense color="pink-2" text-color="pink-9">SF {{ log.pendingEstimate.saturatedFatG!.toFixed(1) }}g</q-chip>
            <q-chip dense color="green-2"  text-color="green-9">Fi {{ log.pendingEstimate.fiberG.toFixed(1) }}g</q-chip>
          </div>
        </q-card-section>

        <!-- Hydration prompt -->
        <q-card-section v-if="log.pendingEstimate.waterMl" class="q-pt-none">
          <q-separator class="q-mb-sm" />
          <div class="row items-center q-gutter-sm">
            <q-icon name="water_drop" color="cyan-7" />
            <div class="col text-caption text-grey-8">
              Add <strong>{{ log.pendingEstimate.waterMl }}ml</strong> to hydration?
            </div>
            <q-input
              v-model.number="waterMlOverride"
              dense outlined type="number" suffix="ml"
              style="width: 90px"
            />
            <q-toggle v-model="addWater" color="cyan-7" />
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

      <div v-if="!log.pendingEstimate">
        <q-btn
          label="Estimate macros"
          color="primary"
          class="full-width"
          :loading="log.submitting"
          :disable="!description && !photo"
          unelevated
          @click="estimate"
        />
      </div>

      <q-separator class="q-my-md" />

      <!-- Activity log -->
      <div class="text-h6 text-weight-bold">Log Activity</div>
      <q-input
        v-model="activityEntry"
        type="textarea"
        placeholder="e.g. 1h gym chest and back, 9200 steps"
        outlined
        autogrow
      />
      <div>
        <q-btn
          label="Log activity"
          color="secondary"
          class="full-width"
          :loading="activityLoading"
          :disable="!activityEntry"
          unelevated
          @click="submitActivity"
        />
      </div>
      <q-banner v-if="activityDone" class="bg-green-1 text-green-8 rounded-borders" dense>
        Activity logged!
      </q-banner>

    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Capacitor } from '@capacitor/core'
import { Camera, CameraResultType, CameraSource } from '@capacitor/camera'
import api from 'src/api/client'
import { useLogStore }   from 'src/stores/log'
import { useTodayStore } from 'src/stores/today'

const log   = useLogStore()
const today = useTodayStore()

const description      = ref('')
const photo            = ref<string | null>(null)
const activityEntry    = ref('')
const activityLoading  = ref(false)
const activityDone     = ref(false)
const fileInput        = ref<HTMLInputElement | null>(null)
const fileInputCamera  = ref<HTMLInputElement | null>(null)
const photoDialog      = ref(false)
const addWater         = ref(true)
const waterMlOverride  = ref<number>(0)

function pickPhoto() {
  if (Capacitor.isNativePlatform()) {
    // Capacitor shows its own camera/gallery prompt natively
    Camera.getPhoto({
      quality:      90,
      allowEditing: false,
      resultType:   CameraResultType.Base64,
      source:       CameraSource.Prompt,
    }).then(image => {
      photo.value = image.base64String ?? null
    }).catch(() => {})
  } else {
    photoDialog.value = true
  }
}

function openCamera() {
  photoDialog.value = false
  fileInputCamera.value?.click()
}

function openGallery() {
  photoDialog.value = false
  fileInput.value?.click()
}

function onFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    const img = new Image()
    img.onload = () => {
      const MAX = 1024
      const scale = Math.min(1, MAX / Math.max(img.width, img.height))
      const canvas = document.createElement('canvas')
      canvas.width  = Math.round(img.width  * scale)
      canvas.height = Math.round(img.height * scale)
      canvas.getContext('2d')!.drawImage(img, 0, 0, canvas.width, canvas.height)
      photo.value = canvas.toDataURL('image/jpeg', 0.85).split(',')[1] ?? null
    }
    img.src = reader.result as string
  }
  reader.readAsDataURL(file)
}

watch(() => log.pendingEstimate?.waterMl, (v) => {
  waterMlOverride.value = v ?? 0
  addWater.value = !!v
})

async function estimate() {
  await log.submitMeal(description.value || null, photo.value)
}

function cancel() {
  log.clearEstimate()
  description.value = ''
  photo.value       = null
  addWater.value    = true
}

async function confirmMeal() {
  if (addWater.value && waterMlOverride.value > 0) {
    await api.post('/api/water', { amountL: waterMlOverride.value / 1000 })
  }
  await today.fetchToday()
  cancel()
}

// ── Water quick-add ──────────────────────────────────────────────────────────

const quickAmounts = [
  { label: '150ml', l: 0.15 },
  { label: '250ml', l: 0.25 },
  { label: '330ml', l: 0.33 },
  { label: '500ml', l: 0.50 },
  { label: '1L',    l: 1.00 },
]
const quickAdding = ref<number | null>(null)
const drinkDone   = ref(false)
const lastDrinkL  = ref(0)

async function quickAdd(amountL: number) {
  quickAdding.value = amountL
  drinkDone.value   = false
  try {
    await api.post('/api/water', { amountL })
    lastDrinkL.value = amountL
    drinkDone.value  = true
    await today.fetchToday()
  } finally {
    quickAdding.value = null
  }
}

// ── Activity ─────────────────────────────────────────────────────────────────

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
