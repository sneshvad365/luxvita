<template>
  <q-card>
    <q-card-section>
      <div class="text-subtitle2 text-weight-bold q-mb-sm">Today's meals</div>

      <q-inner-loading :showing="loading" />

      <div v-if="!loading && meals.length === 0" class="text-grey-5 text-center q-py-md">
        No meals logged yet
      </div>

      <q-list separator>
        <q-item v-for="meal in meals" :key="meal.id" dense>
          <q-item-section>
            <q-item-label class="text-body2">
              {{ meal.description ?? (meal.hasPhoto ? 'Photo meal' : 'Unnamed meal') }}
            </q-item-label>
            <q-item-label caption>
              {{ formatTime(meal.loggedAt) }}
            </q-item-label>
          </q-item-section>
          <q-item-section side>
            <div class="text-weight-medium text-primary">{{ meal.kcal ?? '—' }} kcal</div>
            <div class="text-caption text-grey-6">
              P{{ (meal.proteinG ?? 0).toFixed(0) }}
              C{{ (meal.carbsG   ?? 0).toFixed(0) }}
              F{{ (meal.fatG     ?? 0).toFixed(0) }}
            </div>
          </q-item-section>
        </q-item>
      </q-list>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import type { Meal } from 'src/api/client'

defineProps<{ meals: Meal[]; loading: boolean }>()

function formatTime(ts: string) {
  return new Date(ts).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}
</script>
