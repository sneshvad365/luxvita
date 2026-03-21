<template>
  <q-card>
    <q-card-section>
      <div class="text-subtitle2 text-weight-bold q-mb-sm">Today's meals</div>

      <q-inner-loading :showing="loading" />

      <div v-if="!loading && meals.length === 0" class="text-grey-5 text-center q-py-md">
        No meals logged yet
      </div>

      <q-list separator>
        <div v-for="meal in meals" :key="meal.id">
          <!-- Display mode -->
          <q-item v-if="editingId !== meal.id" dense clickable @click="breakdownMeal = meal">
            <q-item-section>
              <q-item-label class="text-body2">
                {{ meal.description ?? (meal.hasPhoto ? 'Photo meal' : 'Unnamed meal') }}
              </q-item-label>
              <q-item-label caption>{{ formatTime(meal.loggedAt) }}</q-item-label>
            </q-item-section>
            <q-item-section side>
              <div class="text-weight-medium text-primary">{{ meal.kcal ?? '—' }} kcal</div>
              <div class="text-caption text-grey-6">
                P{{ (meal.proteinG ?? 0).toFixed(0) }}
                C{{ (meal.carbsG   ?? 0).toFixed(0) }}
                F{{ (meal.fatG     ?? 0).toFixed(0) }}
              </div>
            </q-item-section>
            <q-item-section side>
              <div class="column">
                <q-btn flat round dense icon="content_copy" size="sm" color="grey-6" @click.stop="emit('copy', meal.id)" />
                <template v-if="!readonly">
                  <q-btn flat round dense icon="edit"   size="sm" color="grey-6" @click.stop="startEdit(meal)" />
                  <q-btn flat round dense icon="delete" size="sm" color="red-4"  @click.stop="remove(meal.id)" />
                </template>
              </div>
            </q-item-section>
          </q-item>

          <!-- Edit mode -->
          <q-item v-else dense class="q-py-sm">
            <q-item-section>
              <q-input
                v-model="form.description"
                dense outlined label="Description"
                class="q-mb-sm"
              />
              <div class="row q-gutter-sm">
                <q-input v-model.number="form.kcal"     dense outlined label="kcal"  type="number" style="width:72px" />
                <q-input v-model.number="form.proteinG" dense outlined label="P (g)" type="number" style="width:64px" />
                <q-input v-model.number="form.carbsG"   dense outlined label="C (g)" type="number" style="width:64px" />
                <q-input v-model.number="form.fatG"     dense outlined label="F (g)" type="number" style="width:64px" />
                <q-input v-model.number="form.fiberG"   dense outlined label="Fi(g)" type="number" style="width:64px" />
              </div>
            </q-item-section>
            <q-item-section side top>
              <div class="column q-gutter-xs q-mt-xs">
                <q-btn flat round dense icon="check" size="sm" color="positive" :loading="saving" @click="saveEdit(meal.id)" />
                <q-btn flat round dense icon="close" size="sm" color="grey-6"   @click="editingId = null" />
              </div>
            </q-item-section>
          </q-item>
        </div>
      </q-list>
    </q-card-section>
  </q-card>

  <!-- Breakdown dialog -->
  <q-dialog :model-value="!!breakdownMeal" @update:model-value="v => { if (!v) breakdownMeal = null }">
    <q-card style="min-width:300px;max-width:420px">
      <q-card-section class="q-pb-none">
        <div class="text-subtitle2 text-weight-bold">
          {{ breakdownMeal?.description ?? (breakdownMeal?.hasPhoto ? 'Photo meal' : 'Meal') }}
        </div>
        <div class="text-caption text-grey-6">{{ breakdownMeal ? formatTime(breakdownMeal.loggedAt) : '' }}</div>
      </q-card-section>

      <q-card-section v-if="breakdownMeal?.breakdown?.length">
        <q-list dense separator>
          <q-item v-for="item in breakdownMeal.breakdown" :key="item.item" class="q-px-none">
            <q-item-section>
              <q-item-label class="text-body2">{{ item.item }}</q-item-label>
              <q-item-label caption class="text-grey-6">
                P{{ item.proteinG.toFixed(0) }}
                C{{ item.carbsG.toFixed(0) }}
                F{{ item.fatG.toFixed(0) }}
                Fi{{ item.fiberG.toFixed(0) }}
              </q-item-label>
            </q-item-section>
            <q-item-section side>
              <span class="text-weight-medium text-primary">{{ item.kcal }} kcal</span>
            </q-item-section>
          </q-item>
        </q-list>

        <q-separator class="q-my-sm" />
        <div class="row justify-between text-body2 text-weight-bold">
          <span>Total</span>
          <span class="text-primary">{{ breakdownMeal?.kcal ?? '—' }} kcal</span>
        </div>
        <div v-if="breakdownMeal?.hasPhoto" class="q-mt-sm">
          <q-btn flat no-caps dense icon="photo_camera" label="View photo" color="primary" size="sm" @click="openPhoto(breakdownMeal!)" />
        </div>
      </q-card-section>

      <q-card-section v-else class="text-grey-5 text-center">
        No breakdown available for this meal
        <div v-if="breakdownMeal?.hasPhoto" class="q-mt-sm">
          <q-btn flat no-caps dense icon="photo_camera" label="View photo" color="primary" size="sm" @click="openPhoto(breakdownMeal!)" />
        </div>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn flat label="Close" color="grey-6" v-close-popup />
      </q-card-actions>
    </q-card>
  </q-dialog>

  <!-- Photo viewer dialog -->
  <q-dialog :model-value="!!photoMeal" @update:model-value="v => { if (!v) closePhotoDialog() }">
    <q-card style="min-width:300px;max-width:500px">
      <q-card-section class="q-pb-none">
        <div class="text-subtitle2 text-weight-bold">
          {{ photoMeal?.description ?? 'Meal photo' }}
        </div>
        <div class="text-caption text-grey-6">{{ photoMeal ? formatTime(photoMeal.loggedAt) : '' }}</div>
      </q-card-section>

      <q-card-section>
        <div v-if="photoLoading" class="flex flex-center q-py-lg">
          <q-spinner size="40px" color="primary" />
        </div>
        <div v-else-if="photoError" class="text-grey-5 text-center q-py-md">
          Photo not available
        </div>
        <img
          v-else-if="photoData"
          :src="'data:image/jpeg;base64,' + photoData"
          style="width:100%;border-radius:8px"
        />
      </q-card-section>

      <q-card-actions align="right">
        <q-btn flat label="Close" color="grey-6" @click="closePhotoDialog" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import api from 'src/api/client'
import type { Meal } from 'src/api/client'
import { fetchMealPhoto } from 'src/api/client'

defineProps<{ meals: Meal[]; loading: boolean; readonly?: boolean }>()
const emit = defineEmits<{ refresh: []; copy: [mealId: string] }>()

const editingId       = ref<string | null>(null)
const saving          = ref(false)
const form            = reactive({ description: '', kcal: 0, proteinG: 0, carbsG: 0, fatG: 0, fiberG: 0 })
const breakdownMeal   = ref<Meal | null>(null)
const photoMeal    = ref<Meal | null>(null)
const photoData    = ref<string | null>(null)
const photoLoading = ref(false)
const photoError   = ref(false)

function closePhotoDialog() {
  photoMeal.value  = null
  photoData.value  = null
  photoError.value = false
}

async function openPhoto(meal: Meal) {
  photoMeal.value    = meal
  photoData.value    = null
  photoError.value   = false
  photoLoading.value = true
  try {
    const res = await fetchMealPhoto(meal.id)
    photoData.value = res.photoData
  } catch {
    photoError.value = true
  } finally {
    photoLoading.value = false
  }
}

function startEdit(meal: Meal) {
  editingId.value    = meal.id
  form.description   = meal.description ?? ''
  form.kcal          = meal.kcal     ?? 0
  form.proteinG      = meal.proteinG ?? 0
  form.carbsG        = meal.carbsG   ?? 0
  form.fatG          = meal.fatG     ?? 0
  form.fiberG        = meal.fiberG   ?? 0
}

async function saveEdit(id: string) {
  saving.value = true
  try {
    await api.put(`/api/meals/${id}`, {
      description: form.description || null,
      kcal:        form.kcal,
      proteinG:    form.proteinG,
      carbsG:      form.carbsG,
      fatG:        form.fatG,
      fiberG:      form.fiberG,
    })
    editingId.value = null
    emit('refresh')
  } finally {
    saving.value = false
  }
}

async function remove(id: string) {
  await api.delete(`/api/meals/${id}`)
  emit('refresh')
}

function formatTime(ts: string) {
  return new Date(ts).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}
</script>
