<template>
  <q-card>
    <q-card-section>
      <div class="text-subtitle2 text-weight-bold q-mb-sm">Weight trend</div>

      <div v-if="!data.entries.length" class="text-grey-5 text-center q-py-md">
        No weight data yet
      </div>

      <template v-else>
        <!-- Simple SVG sparkline -->
        <svg :viewBox="`0 0 ${W} ${H}`" class="full-width" style="height: 120px">
          <!-- Rolling average line -->
          <polyline
            :points="avgPoints"
            fill="none"
            stroke="#1976D2"
            stroke-width="2"
            stroke-linejoin="round"
          />
          <!-- Raw weight dots -->
          <circle
            v-for="(p, i) in dotPoints"
            :key="i"
            :cx="p.x"
            :cy="p.y"
            r="3"
            fill="#1976D2"
            opacity="0.5"
          />
        </svg>

        <!-- Stats row -->
        <div class="row q-gutter-sm q-mt-sm text-center">
          <div class="col">
            <div class="text-h6 text-weight-bold text-primary">
              {{ data.entries[0]?.weightKg.toFixed(1) ?? '—' }} kg
            </div>
            <div class="text-caption text-grey-6">Current</div>
          </div>
          <div class="col" v-if="data.sevenDayAvg">
            <div class="text-h6 text-weight-bold">{{ data.sevenDayAvg.toFixed(1) }} kg</div>
            <div class="text-caption text-grey-6">7-day avg</div>
          </div>
          <div class="col" v-if="data.paceKgPerWeek !== null">
            <div class="text-h6 text-weight-bold" :class="paceColor">
              {{ data.paceKgPerWeek > 0 ? '+' : '' }}{{ data.paceKgPerWeek?.toFixed(2) }} kg/wk
            </div>
            <div class="text-caption text-grey-6">Pace</div>
          </div>
          <div class="col" v-if="data.etaDays !== null && data.etaDays > 0">
            <div class="text-h6 text-weight-bold">{{ data.etaDays }}d</div>
            <div class="text-caption text-grey-6">ETA to goal</div>
          </div>
        </div>
      </template>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { WeightTrendResponse } from 'src/api/client'

const props = defineProps<{ data: WeightTrendResponse }>()

const W = 300, H = 100, PAD = 10

const sortedAsc = computed(() =>
  [...props.data.entries].reverse() // entries come DESC from API
)

const minW = computed(() => Math.min(...sortedAsc.value.map(e => e.weightKg)))
const maxW = computed(() => Math.max(...sortedAsc.value.map(e => e.weightKg)))
const range = computed(() => (maxW.value - minW.value) || 1)

function xOf(i: number, len: number) {
  return PAD + (i / Math.max(len - 1, 1)) * (W - PAD * 2)
}
function yOf(w: number) {
  return H - PAD - ((w - minW.value) / range.value) * (H - PAD * 2)
}

const dotPoints = computed(() =>
  sortedAsc.value.map((e, i) => ({ x: xOf(i, sortedAsc.value.length), y: yOf(e.weightKg) }))
)

const avgPoints = computed(() =>
  sortedAsc.value
    .map((e, i) => `${xOf(i, sortedAsc.value.length)},${yOf(e.rolling7d)}`)
    .join(' ')
)

const paceColor = computed(() => {
  const p = props.data.paceKgPerWeek
  if (p === null) return ''
  return p < 0 ? 'text-positive' : p > 0 ? 'text-negative' : ''
})
</script>
