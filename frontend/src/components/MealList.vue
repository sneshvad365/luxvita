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
          <q-item v-if="editingId !== meal.id" dense>
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
            <q-item-section v-if="!readonly" side>
              <div class="column">
                <q-btn flat round dense icon="edit"   size="sm" color="grey-6" @click="startEdit(meal)" />
                <q-btn flat round dense icon="delete" size="sm" color="red-4"  @click="remove(meal.id)" />
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
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import api from 'src/api/client'
import type { Meal } from 'src/api/client'

defineProps<{ meals: Meal[]; loading: boolean; readonly?: boolean }>()
const emit = defineEmits<{ refresh: [] }>()

const editingId = ref<string | null>(null)
const saving    = ref(false)
const form      = reactive({ description: '', kcal: 0, proteinG: 0, carbsG: 0, fatG: 0, fiberG: 0 })

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
