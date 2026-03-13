<template>
  <q-card>
    <q-card-section class="row q-gutter-sm justify-center">
      <MacroChip
        v-for="m in macros" :key="m.label"
        :label="m.label"
        :value="m.value"
        :target="m.target"
        :unit="m.unit"
        :color="m.color"
      />
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { computed, defineComponent, h } from 'vue'
import type { Macros, UserProfile } from 'src/api/client'

const props = defineProps<{
  totals:  Macros
  profile: UserProfile | null
}>()

const MacroChip = defineComponent({
  props: { label: String, value: Number, target: Number, unit: String, color: String },
  setup(p) {
    return () => {
      const pct  = Math.min(100, ((p.value ?? 0) / (p.target || 1)) * 100)
      const text = `${(p.value ?? 0).toFixed(p.unit === 'kcal' ? 0 : 0)}${p.unit} / ${p.target}${p.unit}`
      return h('div', { class: 'text-center', style: 'min-width: 70px' }, [
        h('div', { class: `text-caption text-weight-bold text-${p.color}-8` }, p.label),
        h('div', { class: 'text-caption text-grey-7' }, text),
        h('q-linear-progress', { value: pct / 100, color: p.color, trackColor: 'grey-3', rounded: true, style: 'height: 4px; margin-top: 2px' }),
      ])
    }
  },
})

const macros = computed(() => {
  const p = props.profile
  return [
    { label: 'Protein', value: props.totals.proteinG, target: p?.targetProteinG ?? 150, unit: 'g', color: 'blue'   },
    { label: 'Carbs',   value: props.totals.carbsG,   target: p?.targetCarbsG   ?? 200, unit: 'g', color: 'amber'  },
    { label: 'Fat',     value: props.totals.fatG,     target: p?.targetFatG     ?? 70,  unit: 'g', color: 'red'    },
    { label: 'Fiber',   value: props.totals.fiberG,   target: p?.targetFiberG   ?? 25,  unit: 'g', color: 'green'  },
  ]
})
</script>
