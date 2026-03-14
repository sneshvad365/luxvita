<template>
  <q-page class="q-pa-md q-pb-xl">
    <div class="q-gutter-md">
      <div class="text-h6 text-weight-bold">Trends</div>

      <q-inner-loading :showing="trends.loading" />

      <!-- Log weight -->
      <q-card>
        <q-card-section>
          <div class="text-subtitle2 text-weight-bold q-mb-sm">Log weight</div>
          <div class="row q-gutter-sm items-center">
            <q-input
              v-model.number="weightInput"
              type="number"
              label="kg"
              outlined
              dense
              class="col"
            />
            <q-btn label="Save" color="primary" unelevated :loading="weightLoading" @click="logWeight" />
          </div>
        </q-card-section>
      </q-card>

      <!-- Weight chart -->
      <WeightChart v-if="trends.weightTrend" :data="trends.weightTrend" />

      <!-- Consistency bars -->
      <ConsistencyBars v-if="trends.summary" :summary="trends.summary" />

      <!-- 7-day averages -->
      <q-card v-if="trends.summary">
        <q-card-section>
          <div class="text-subtitle2 text-weight-bold q-mb-sm">7-day averages</div>
          <MacroRow label="Calories"  :value="trends.summary.sevenDayAvg.kcal"     unit="kcal" />
          <MacroRow label="Protein"   :value="trends.summary.sevenDayAvg.proteinG" unit="g"    />
          <MacroRow label="Carbs"     :value="trends.summary.sevenDayAvg.carbsG"   unit="g"    />
          <MacroRow label="Fat"       :value="trends.summary.sevenDayAvg.fatG"     unit="g"    />
          <MacroRow label="Fiber"     :value="trends.summary.sevenDayAvg.fiberG"   unit="g"    />
        </q-card-section>
      </q-card>

      <!-- Weekly insights -->
      <template v-if="trends.insights.length">
        <div class="text-subtitle2 text-weight-bold">Weekly insights</div>
        <InsightCard v-for="(ins, i) in trends.insights" :key="i" :insight="ins" />
      </template>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted, defineComponent, h } from 'vue'
import { useTrendsStore } from 'src/stores/trends'
import WeightChart      from 'src/components/WeightChart.vue'
import ConsistencyBars  from 'src/components/ConsistencyBars.vue'
import InsightCard      from 'src/components/InsightCard.vue'

const trends        = useTrendsStore()
const weightInput   = ref<number | null>(null)
const weightLoading = ref(false)

// Simple inline helper component to avoid a full file for one row
const MacroRow = defineComponent({
  props: { label: String, value: Number, unit: String },
  setup(p) {
    return () => h('div', { class: 'row justify-between text-body2 q-py-xs' }, [
      h('span', { class: 'text-grey-7' }, p.label),
      h('span', { class: 'text-weight-medium' }, `${p.value?.toFixed(p.unit === 'kcal' ? 0 : 1)} ${p.unit}`),
    ])
  },
})

onMounted(async () => {
  await Promise.all([trends.fetchAll(), trends.fetchWeeklyInsights()])
})

async function logWeight() {
  if (!weightInput.value) return
  weightLoading.value = true
  try {
    await trends.logWeight(weightInput.value)
    weightInput.value = null
  } finally {
    weightLoading.value = false
  }
}
</script>
