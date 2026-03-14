<template>
  <q-card>
    <q-card-section>
      <div class="text-subtitle2 text-weight-bold q-mb-sm">Today's activity</div>

      <div v-if="activities.length === 0" class="text-grey-5 text-center q-py-md">
        No activity logged yet
      </div>

      <q-list separator>
        <q-item v-for="act in activities" :key="act.id" dense>
          <q-item-section avatar>
            <q-icon :name="activityIcon(act.parsed?.type)" color="orange-7" />
          </q-item-section>

          <!-- Display mode -->
          <q-item-section v-if="editingId !== act.id">
            <q-item-label class="text-body2">{{ act.entry }}</q-item-label>
            <q-item-label caption>{{ formatTime(act.loggedAt) }}</q-item-label>
          </q-item-section>
          <q-item-section v-if="editingId !== act.id && act.parsed" side>
            <div v-if="act.parsed.durationMin" class="text-caption text-grey-7">
              {{ act.parsed.durationMin }}min
            </div>
            <div v-if="act.parsed.steps" class="text-caption text-grey-7">
              {{ act.parsed.steps.toLocaleString() }} steps
            </div>
          </q-item-section>
          <q-item-section v-if="editingId !== act.id && !readonly" side>
            <div class="row">
              <q-btn flat round dense icon="edit" size="sm" color="grey-6" @click="startEdit(act)" />
              <q-btn flat round dense icon="delete" size="sm" color="red-4" @click="remove(act.id)" />
            </div>
          </q-item-section>

          <!-- Edit mode -->
          <q-item-section v-if="editingId === act.id">
            <q-input
              v-model="editText"
              dense
              outlined
              autofocus
              @keyup.enter="saveEdit(act.id)"
              @keyup.escape="editingId = null"
            />
          </q-item-section>
          <q-item-section v-if="editingId === act.id" side>
            <div class="row">
              <q-btn flat round dense icon="check" size="sm" color="positive" :loading="saving" @click="saveEdit(act.id)" />
              <q-btn flat round dense icon="close" size="sm" color="grey-6" @click="editingId = null" />
            </div>
          </q-item-section>
        </q-item>
      </q-list>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import api from 'src/api/client'
import type { ActivityLogResponse } from 'src/api/client'

defineProps<{ activities: ActivityLogResponse[]; readonly?: boolean }>()
const emit = defineEmits<{ refresh: [] }>()

const editingId = ref<string | null>(null)
const editText  = ref('')
const saving    = ref(false)

function startEdit(act: ActivityLogResponse) {
  editingId.value = act.id
  editText.value  = act.entry
}

async function saveEdit(id: string) {
  if (!editText.value.trim()) return
  saving.value = true
  try {
    await api.put(`/api/activity/${id}`, { entry: editText.value.trim() })
    editingId.value = null
    emit('refresh')
  } finally {
    saving.value = false
  }
}

async function remove(id: string) {
  await api.delete(`/api/activity/${id}`)
  emit('refresh')
}

function formatTime(ts: string) {
  return new Date(ts).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}

function activityIcon(type: string | undefined) {
  const map: Record<string, string> = {
    gym:   'fitness_center',
    run:   'directions_run',
    walk:  'directions_walk',
    sport: 'sports_tennis',
    rest:  'self_improvement',
    swim:  'pool',
    bike:  'directions_bike',
  }
  return map[type ?? ''] ?? 'directions_run'
}
</script>
