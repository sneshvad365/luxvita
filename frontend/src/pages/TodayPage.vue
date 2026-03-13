<template>
  <q-page class="q-pa-md q-pb-xl">
    <div class="q-gutter-md">
      <!-- Calorie ring card -->
      <CalorieCard :totals="today.totals" :profile="profile.profile" />

      <!-- Macro chips -->
      <MacroChips :totals="today.totals" :profile="profile.profile" />

      <!-- AI Insight -->
      <InsightCard v-if="today.insight" :insight="today.insight" />

      <!-- Meal list -->
      <MealList :meals="today.meals" :loading="today.loading" />
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useTodayStore }   from 'src/stores/today'
import { useProfileStore } from 'src/stores/profile'
import CalorieCard from 'src/components/CalorieCard.vue'
import MacroChips  from 'src/components/MacroChips.vue'
import InsightCard from 'src/components/InsightCard.vue'
import MealList    from 'src/components/MealList.vue'

const today   = useTodayStore()
const profile = useProfileStore()

onMounted(async () => {
  await Promise.all([
    today.fetchToday(),
    today.fetchInsight(),
    profile.fetch(),
  ])
})
</script>
