<template>
  <q-card>
    <q-card-section>
      <div class="text-caption text-grey-6 text-center q-mb-sm">{{ dateLabel }}</div>

      <div class="row items-center justify-around">

        <!-- Calorie ring -->
        <div class="column items-center">
          <div class="relative-position">
            <q-circular-progress
              :value="calPct"
              size="100px"
              :thickness="0.12"
              color="primary"
              track-color="grey-3"
            />
            <div class="absolute-center text-center">
              <div class="text-subtitle1 text-weight-bold text-primary">{{ totals.kcal }}</div>
              <div class="text-caption text-grey-6" style="font-size:10px">/ {{ calTarget }} kcal</div>
            </div>
          </div>
          <div class="text-caption text-grey-6 q-mt-xs">
            {{ calRemaining > 0 ? `${calRemaining} left` : `${Math.abs(calRemaining)} over` }}
          </div>
        </div>

        <!-- Divider -->
        <q-separator vertical />

        <!-- Water bottle -->
        <div class="column items-center">
          <svg width="38" height="72" viewBox="0 0 60 110">
            <path
              d="M22,0 L22,12 Q10,18 8,30 L8,95 Q8,104 18,104 L42,104 Q52,104 52,95 L52,30 Q50,18 38,12 L38,0 Z"
              fill="none" stroke="#90CAF9" stroke-width="2"
            />
            <clipPath :id="`wfill-${uid}`">
              <rect x="0" :y="fillY" width="60" height="110" />
            </clipPath>
            <path
              d="M22,0 L22,12 Q10,18 8,30 L8,95 Q8,104 18,104 L42,104 Q52,104 52,95 L52,30 Q50,18 38,12 L38,0 Z"
              :fill="fillColor"
              :clip-path="`url(#wfill-${uid})`"
              style="transition: all 0.5s ease"
            />
            <line x1="6" y1="14" x2="54" y2="14" stroke="#42A5F5" stroke-width="1" stroke-dasharray="3,2" />
          </svg>
          <div class="text-weight-bold text-cyan-8 q-mt-xs">{{ totalL.toFixed(2) }}L</div>
          <div class="text-caption text-grey-6">/ {{ waterTarget }}L</div>
          <div v-if="totalL >= waterTarget" class="text-caption text-positive">Done!</div>
        </div>

      </div>
    </q-card-section>

    <!-- Water entries -->
    <q-card-section v-if="waterEntries.length" class="q-pt-none">
      <q-separator class="q-mb-xs" />
      <q-list dense>
        <q-item v-for="entry in waterEntries" :key="entry.id" dense class="q-px-none">
          <q-item-section>
            <q-item-label class="text-caption text-grey-6">
              {{ formatTime(entry.loggedAt) }} — {{ (entry.amountL * 1000).toFixed(0) }}ml
            </q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-btn flat round dense icon="close" size="xs" color="grey-4" @click="emit('deleteWater', entry.id)" />
          </q-item-section>
        </q-item>
      </q-list>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Macros, UserProfile } from 'src/api/client'

interface WaterEntry { id: string; loggedAt: string; amountL: number }

const props = defineProps<{
  totals:             Macros
  profile:            UserProfile | null
  adjustedCalTarget:  number | null
  totalL:             number
  waterEntries:       WaterEntry[]
}>()

const emit = defineEmits<{ deleteWater: [id: string] }>()

const uid = Math.random().toString(36).slice(2)

// Calories — use activity-adjusted target when available
const calTarget    = computed(() => props.adjustedCalTarget ?? props.profile?.targetKcal ?? 2000)
const calPct       = computed(() => Math.min(100, (props.totals.kcal / calTarget.value) * 100))
const calRemaining = computed(() => calTarget.value - props.totals.kcal)

// Water
const waterTarget = computed(() => props.profile?.targetWaterL ?? 2.5)
const waterPct    = computed(() => Math.min(1, props.totalL / waterTarget.value))

const BOTTLE_TOP    = 14
const BOTTLE_BOTTOM = 102
const BOTTLE_H      = BOTTLE_BOTTOM - BOTTLE_TOP
const fillY         = computed(() => BOTTLE_BOTTOM - waterPct.value * BOTTLE_H)
const fillColor     = computed(() => waterPct.value >= 1 ? '#29B6F6' : '#81D4FA')

const dateLabel = computed(() =>
  new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' }),
)

function formatTime(ts: string) {
  return new Date(ts).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}
</script>
