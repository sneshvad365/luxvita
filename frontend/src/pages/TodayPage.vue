<template>
  <q-page class="q-pa-md q-pb-xl">
    <div class="q-gutter-md">

      <!-- Date navigation -->
      <div class="row items-center justify-between">
        <q-btn flat round dense icon="chevron_left" @click="prevDay" />
        <div class="text-subtitle2 text-weight-medium">{{ dateLabel }}</div>
        <q-btn flat round dense icon="chevron_right" :disable="isToday" @click="nextDay" />
      </div>

      <!-- AI Insight button (top, today only) -->
      <div v-if="isToday">
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

      <!-- Chat section (shown after insight is fetched) -->
      <div v-if="chatMessages.length > 0">
        <div class="text-subtitle2 text-weight-bold q-mb-sm row items-center q-gutter-xs">
          <q-icon name="lightbulb" color="primary" />
          <span>Today's Insight</span>
          <q-btn flat round dense icon="close" size="xs" color="grey-5" class="q-ml-auto" @click="clearChat" />
        </div>

        <div class="q-gutter-sm">
          <div
            v-for="(msg, i) in chatMessages"
            :key="i"
            :class="msg.role === 'user' ? 'row justify-end' : 'row justify-start'"
          >
            <div
              :class="msg.role === 'user'
                ? 'bg-primary text-white rounded-borders q-pa-sm text-body2'
                : 'bg-grey-2 text-grey-9 rounded-borders q-pa-sm text-body2'"
              style="max-width: 85%; white-space: pre-wrap; word-break: break-word"
            >
              {{ msg.content }}
            </div>
          </div>

          <div v-if="chatLoading" class="row justify-start">
            <div class="bg-grey-2 rounded-borders q-pa-sm">
              <q-spinner-dots color="primary" size="20px" />
            </div>
          </div>
        </div>

        <div v-if="isToday" class="row q-mt-sm q-gutter-sm">
          <q-input
            v-model="chatInput"
            outlined
            dense
            placeholder="Ask a follow-up question..."
            class="col"
            @keyup.enter="sendChat"
          />
          <q-btn unelevated color="primary" icon="send" :loading="chatLoading" :disable="!chatInput.trim()" @click="sendChat" />
        </div>
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
              @click="fetchInsightAndChat"
            />
          </q-card-actions>
        </q-card>
      </q-dialog>

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

      <!-- Meal list -->
      <MealList :meals="today.meals" :loading="today.loading" :readonly="!isToday" @refresh="today.fetchToday()" @copy="copyMeal" />

      <!-- Activity list -->
      <ActivityList :activities="today.activities" :readonly="!isToday" @refresh="today.fetchToday()" />
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { onMounted, computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import api from 'src/api/client'
import { useTodayStore }   from 'src/stores/today'
import { useProfileStore } from 'src/stores/profile'
import { useLogStore }     from 'src/stores/log'
import CalorieCard  from 'src/components/CalorieCard.vue'
import MacroChips   from 'src/components/MacroChips.vue'
import MealList     from 'src/components/MealList.vue'
import ActivityList from 'src/components/ActivityList.vue'

const insightConfirmOpen = ref(false)
const chatMessages = ref<{ role: 'user' | 'assistant'; content: string }[]>([])
const chatInput    = ref('')
const chatLoading  = ref(false)

const router  = useRouter()
const today   = useTodayStore()
const profile = useProfileStore()
const log     = useLogStore()

const todayStr  = new Date().toISOString().slice(0, 10)
const isToday   = computed(() => today.selectedDate === todayStr)
const dateLabel = computed(() => {
  const d = new Date(today.selectedDate + 'T12:00:00')
  if (today.selectedDate === todayStr) return 'Today'
  const yesterday = new Date(); yesterday.setDate(yesterday.getDate() - 1)
  if (today.selectedDate === yesterday.toISOString().slice(0, 10)) return 'Yesterday'
  return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })
})

async function fetchInsightAndChat() {
  chatMessages.value = []
  chatInput.value    = ''
  await today.fetchInsight()
  if (today.insight) {
    chatMessages.value = [{ role: 'assistant', content: today.insight.insight }]
  }
}

async function sendChat() {
  const text = chatInput.value.trim()
  if (!text) return
  chatInput.value = ''
  chatMessages.value.push({ role: 'user', content: text })
  chatLoading.value = true
  try {
    const { data } = await api.post<{ reply: string }>('/api/insights/chat', {
      messages: chatMessages.value,
      date: today.selectedDate,
    })
    chatMessages.value.push({ role: 'assistant', content: data.reply })
  } finally {
    chatLoading.value = false
  }
}

function clearChat() {
  chatMessages.value = []
  chatInput.value    = ''
}

function prevDay() {
  const d = new Date(today.selectedDate + 'T12:00:00')
  d.setDate(d.getDate() - 1)
  today.goToDate(d.toISOString().slice(0, 10))
  clearChat()
}

function nextDay() {
  if (isToday.value) return
  const d = new Date(today.selectedDate + 'T12:00:00')
  d.setDate(d.getDate() + 1)
  today.goToDate(d.toISOString().slice(0, 10))
  clearChat()
}

onMounted(async () => {
  await Promise.all([
    today.fetchToday(),
    profile.fetch(),
  ])
  // Seed chat if insight was already loaded (e.g. navigating back to Today)
  if (today.insight && chatMessages.value.length === 0) {
    chatMessages.value = [{ role: 'assistant', content: today.insight.insight }]
  }
})

async function copyMeal(mealId: string) {
  await log.copyMeal(mealId)
  await router.push('/log')
}

async function deleteWater(id: string) {
  await api.delete(`/api/water/${id}`)
  await today.fetchToday()
}
</script>
