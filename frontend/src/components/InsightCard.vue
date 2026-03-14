<template>
  <q-card :class="`bg-${color}-1`">
    <q-card-section class="row items-start q-gutter-sm">
      <q-icon :name="icon" :color="`${color}-8`" size="sm" class="q-mt-xs" />
      <div class="col">
        <div v-if="showTitle" class="text-caption text-weight-bold q-mb-xs" :class="`text-${color}-8`" style="text-transform:uppercase;letter-spacing:0.05em">{{ title }}</div>
        <div class="text-body2" :class="`text-${color}-9`">{{ insight.insight }}</div>
      </div>
      <q-btn v-if="!noRefresh" flat round dense icon="refresh" :color="`${color}-8`" size="sm" @click="emit('refresh')" />
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Insight } from 'src/api/client'

const props = defineProps<{ insight: Insight; showTitle?: boolean; noRefresh?: boolean }>()
const emit  = defineEmits<{ refresh: [] }>()

const typeMap: Record<string, { color: string; icon: string; label: string }> = {
  protein:   { color: 'blue',   icon: 'fitness_center',  label: 'Protein'   },
  timing:    { color: 'purple', icon: 'schedule',         label: 'Timing'    },
  fiber:     { color: 'green',  icon: 'eco',              label: 'Fiber'     },
  hydration: { color: 'cyan',   icon: 'water_drop',       label: 'Hydration' },
  recovery:  { color: 'orange', icon: 'self_improvement', label: 'Recovery'  },
  weight:    { color: 'pink',   icon: 'monitor_weight',   label: 'Weight'    },
}

const color = computed(() => typeMap[props.insight.type]?.color ?? 'grey')
const icon  = computed(() => typeMap[props.insight.type]?.icon  ?? 'lightbulb')
const title = computed(() => typeMap[props.insight.type]?.label ?? props.insight.type)
</script>
