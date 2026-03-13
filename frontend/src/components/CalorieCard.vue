<template>
  <q-card>
    <q-card-section class="text-center">
      <div class="text-caption text-grey-6 q-mb-xs">{{ dateLabel }}</div>

      <div class="relative-position inline-block">
        <q-circular-progress
          :value="pct"
          size="140px"
          :thickness="0.12"
          color="primary"
          track-color="grey-3"
          class="q-my-sm"
        />
        <div class="absolute-center text-center">
          <div class="text-h5 text-weight-bold text-primary">{{ totals.kcal }}</div>
          <div class="text-caption text-grey-6">/ {{ target }} kcal</div>
        </div>
      </div>

      <div class="text-caption text-grey-6 q-mt-xs">
        {{ remaining > 0 ? `${remaining} remaining` : `${Math.abs(remaining)} over` }}
      </div>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Macros, UserProfile } from 'src/api/client'

const props = defineProps<{
  totals:  Macros
  profile: UserProfile | null
}>()

const target = computed(() => props.profile?.targetKcal ?? 2000)
const pct    = computed(() => Math.min(100, (props.totals.kcal / target.value) * 100))
const remaining = computed(() => target.value - props.totals.kcal)

const dateLabel = computed(() =>
  new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' }),
)
</script>
