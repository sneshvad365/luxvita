<template>
  <q-card>
    <q-card-section class="text-center">
      <div class="text-subtitle2 text-weight-bold q-mb-sm">Hydration</div>

      <!-- Bottle SVG -->
      <div class="flex flex-center q-mb-sm">
        <svg width="60" height="110" viewBox="0 0 60 110">
          <path
            d="M22,0 L22,12 Q10,18 8,30 L8,95 Q8,104 18,104 L42,104 Q52,104 52,95 L52,30 Q50,18 38,12 L38,0 Z"
            fill="none" stroke="#90CAF9" stroke-width="2"
          />
          <clipPath :id="`fill-${uid}`">
            <rect x="0" :y="fillY" width="60" height="110" />
          </clipPath>
          <path
            d="M22,0 L22,12 Q10,18 8,30 L8,95 Q8,104 18,104 L42,104 Q52,104 52,95 L52,30 Q50,18 38,12 L38,0 Z"
            :fill="fillColor"
            :clip-path="`url(#fill-${uid})`"
            style="transition: all 0.5s ease"
          />
          <line x1="6" :y1="goalY" x2="54" :y2="goalY" stroke="#42A5F5" stroke-width="1" stroke-dasharray="3,2" />
        </svg>
      </div>

      <div class="text-h6 text-weight-bold text-cyan-8">{{ totalL.toFixed(2) }}L</div>
      <div class="text-caption text-grey-6">goal {{ target }}L</div>
      <div v-if="totalL >= target" class="text-caption text-positive q-mt-xs">Goal reached!</div>
    </q-card-section>

    <!-- Entries -->
    <q-card-section v-if="entries.length" class="q-pt-none">
      <q-separator class="q-mb-sm" />
      <q-list dense>
        <q-item v-for="entry in entries" :key="entry.id" dense class="q-px-none">
          <q-item-section>
            <q-item-label class="text-caption text-grey-7">
              {{ formatTime(entry.loggedAt) }} — {{ (entry.amountL * 1000).toFixed(0) }}ml
            </q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-btn flat round dense icon="close" size="xs" color="grey-5" @click="remove(entry.id)" />
          </q-item-section>
        </q-item>
      </q-list>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import api from 'src/api/client'
import type { UserProfile } from 'src/api/client'

interface WaterEntry { id: string; loggedAt: string; amountL: number }

const props = defineProps<{
  totalL:  number
  entries: WaterEntry[]
  profile: UserProfile | null
}>()
const emit = defineEmits<{ refresh: [] }>()

const uid    = Math.random().toString(36).slice(2)
const target = computed(() => props.profile?.targetWaterL ?? 2.5)
const pct    = computed(() => Math.min(1, props.totalL / target.value))

const BOTTLE_TOP    = 14
const BOTTLE_BOTTOM = 102
const BOTTLE_H      = BOTTLE_BOTTOM - BOTTLE_TOP

const fillY     = computed(() => BOTTLE_BOTTOM - pct.value * BOTTLE_H)
const goalY     = computed(() => BOTTLE_TOP)
const fillColor = computed(() => pct.value >= 1 ? '#29B6F6' : '#81D4FA')

async function remove(id: string) {
  await api.delete(`/api/water/${id}`)
  emit('refresh')
}

function formatTime(ts: string) {
  return new Date(ts).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}
</script>
