<template>
  <q-card>
    <q-card-section>
      <div class="text-subtitle2 text-weight-bold q-mb-sm">30-day consistency</div>
      <div v-for="bar in bars" :key="bar.label" class="q-mb-sm">
        <div class="row justify-between text-caption q-mb-xs">
          <span class="text-grey-7">{{ bar.label }}</span>
          <span class="text-weight-medium">{{ (bar.value * 100).toFixed(0) }}%</span>
        </div>
        <q-linear-progress
          :value="bar.value"
          :color="bar.color"
          track-color="grey-3"
          rounded
          style="height: 8px"
        />
      </div>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TrendsSummary } from 'src/api/client'

const props = defineProps<{ summary: TrendsSummary }>()

const bars = computed(() => [
  { label: 'Calories within target', value: props.summary.consistency.kcal,    color: 'orange' },
  { label: 'Protein target met',     value: props.summary.consistency.protein,  color: 'blue'   },
  { label: 'Fiber target met',       value: props.summary.consistency.fiber,    color: 'green'  },
])
</script>
