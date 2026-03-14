<template>
  <q-page class="q-pa-md q-pb-xl">
    <div class="q-gutter-md">

      <!-- Date navigation -->
      <div class="row items-center justify-between">
        <q-btn flat round dense icon="chevron_left" @click="prevDay" />
        <div class="text-subtitle2 text-weight-medium">{{ dateLabel }}</div>
        <q-btn flat round dense icon="chevron_right" :disable="isToday" @click="nextDay" />
      </div>

      <!-- Calorie ring + water bottle -->
      <CalorieCard
        :totals="today.totals"
        :profile="profile.profile"
        :total-l="today.waterTotalL"
        :water-entries="today.waterEntries"
        @delete-water="deleteWater"
      />

      <!-- Macro chips -->
      <MacroChips :totals="today.totals" :profile="profile.profile" />

      <!-- AI Insight -->
      <InsightCard v-if="today.insight" :insight="today.insight" @refresh="today.fetchInsight()" />
      <div v-else-if="isToday">
        <q-btn
          unelevated
          color="primary"
          icon="lightbulb"
          label="Get today's insight"
          :loading="today.insightLoading"
          class="full-width"
          @click="insightConfirmOpen = true"
        />
      </div>

      <!-- Insight confirmation dialog -->
      <q-dialog v-model="insightConfirmOpen">
        <q-card style="min-width: 280px">
          <q-card-section class="row items-center q-pb-none">
            <q-icon name="lightbulb" color="primary" size="sm" class="q-mr-sm" />
            <span class="text-subtitle2 text-weight-bold">Ready for your insight?</span>
          </q-card-section>
          <q-card-section class="text-body2 text-grey-8">
            For the most accurate insight, make sure you've logged all your meals,
            activities, and water intake for today.
          </q-card-section>
          <q-card-actions align="right">
            <q-btn flat label="Not yet" color="grey-6" v-close-popup />
            <q-btn
              unelevated
              label="Yes, get insight"
              color="primary"
              v-close-popup
              @click="today.fetchInsight()"
            />
          </q-card-actions>
        </q-card>
      </q-dialog>

      <!-- Meal list -->
      <MealList :meals="today.meals" :loading="today.loading" :readonly="!isToday" @refresh="today.fetchToday()" />

      <!-- Activity list -->
      <ActivityList :activities="today.activities" :readonly="!isToday" @refresh="today.fetchToday()" />
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { onMounted, computed, ref } from 'vue'
import api from 'src/api/client'
import { useTodayStore }   from 'src/stores/today'
import { useProfileStore } from 'src/stores/profile'
import CalorieCard  from 'src/components/CalorieCard.vue'
import MacroChips   from 'src/components/MacroChips.vue'
import InsightCard  from 'src/components/InsightCard.vue'
import MealList     from 'src/components/MealList.vue'
import ActivityList from 'src/components/ActivityList.vue'

const insightConfirmOpen = ref(false)

const today   = useTodayStore()
const profile = useProfileStore()

const todayStr  = new Date().toISOString().slice(0, 10)
const isToday   = computed(() => today.selectedDate === todayStr)
const dateLabel = computed(() => {
  const d = new Date(today.selectedDate + 'T12:00:00')
  if (today.selectedDate === todayStr) return 'Today'
  const yesterday = new Date(); yesterday.setDate(yesterday.getDate() - 1)
  if (today.selectedDate === yesterday.toISOString().slice(0, 10)) return 'Yesterday'
  return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })
})

function prevDay() {
  const d = new Date(today.selectedDate + 'T12:00:00')
  d.setDate(d.getDate() - 1)
  today.goToDate(d.toISOString().slice(0, 10))
}

function nextDay() {
  if (isToday.value) return
  const d = new Date(today.selectedDate + 'T12:00:00')
  d.setDate(d.getDate() + 1)
  today.goToDate(d.toISOString().slice(0, 10))
}

onMounted(async () => {
  await Promise.all([
    today.fetchToday(),
    profile.fetch(),
  ])
})

async function deleteWater(id: string) {
  await api.delete(`/api/water/${id}`)
  await today.fetchToday()
}
</script>
