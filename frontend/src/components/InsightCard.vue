<template>
  <q-card :class="`bg-${color}-1`">
    <q-card-section class="row items-start q-gutter-sm">
      <q-icon :name="icon" :color="`${color}-8`" size="sm" class="q-mt-xs" />
      <div class="col text-body2" :class="`text-${color}-9`">{{ insight.insight }}</div>
      <q-btn flat round dense icon="refresh" :color="`${color}-8`" size="sm" @click="emit('refresh')" />
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Insight } from 'src/api/client'

const props = defineProps<{ insight: Insight }>()
const emit  = defineEmits<{ refresh: [] }>()

const typeMap: Record<string, { color: string; icon: string }> = {
  protein:   { color: 'blue',   icon: 'fitness_center'  },
  timing:    { color: 'purple', icon: 'schedule'         },
  fiber:     { color: 'green',  icon: 'eco'              },
  hydration: { color: 'cyan',   icon: 'water_drop'       },
  recovery:  { color: 'orange', icon: 'self_improvement' },
  weight:    { color: 'pink',   icon: 'monitor_weight'   },
}

const color = computed(() => typeMap[props.insight.type]?.color ?? 'grey')
const icon  = computed(() => typeMap[props.insight.type]?.icon  ?? 'lightbulb')
</script>
