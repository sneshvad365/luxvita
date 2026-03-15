<template>
  <q-page class="q-pa-md q-pb-xl">
    <div class="q-gutter-md">
      <div class="text-h6 text-weight-bold">Health records</div>

      <div class="row q-gutter-sm">
        <q-btn unelevated color="secondary" icon="psychology" label="Insight"    class="col" @click="getInsight" :loading="insightLoading" />
        <q-btn unelevated color="primary"   icon="add"        label="Add record" class="col" @click="addOpen = true" />
      </div>

      <div v-if="records.length === 0" class="text-grey-5 text-center q-py-xl text-body2">
        No records yet — add blood tests, doctor notes, or any health data
      </div>

      <q-list separator v-else>
        <q-item
          v-for="rec in records"
          :key="rec.id"
          clickable
          dense
          class="q-px-none"
          @click="openRecord(rec.id)"
        >
          <q-item-section avatar>
            <q-icon :name="rec.sourceType === 'pdf' ? 'picture_as_pdf' : rec.sourceType === 'image' ? 'image' : 'notes'" color="primary" />
          </q-item-section>
          <q-item-section>
            <q-item-label class="text-body2 row items-center q-gutter-xs">
              <span>{{ rec.title }}</span>
              <q-chip
                v-if="rec.hasContent === false"
                dense
                color="warning"
                text-color="white"
                icon="warning"
                label="No medical data"
                size="xs"
              />
            </q-item-label>
            <q-item-label caption>{{ formatDate(rec.createdAt) }}</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-btn flat round dense icon="delete" size="sm" color="red-4" @click.stop="deleteRecord(rec.id)" />
          </q-item-section>
        </q-item>
      </q-list>

      <!-- AI Chat Section -->
      <div v-if="chatMessages.length > 0" class="q-mt-md">
        <div class="text-subtitle2 text-weight-bold q-mb-sm row items-center q-gutter-xs">
          <q-icon name="psychology" color="secondary" />
          <span>Health Advisor</span>
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
              <q-spinner-dots color="secondary" size="20px" />
            </div>
          </div>
        </div>

        <div class="row q-mt-sm q-gutter-sm">
          <q-input
            v-model="chatInput"
            outlined
            dense
            placeholder="Ask a follow-up question..."
            class="col"
            @keyup.enter="sendChat"
          />
          <q-btn unelevated color="secondary" icon="send" :loading="chatLoading" :disable="!chatInput.trim()" @click="sendChat" />
        </div>
      </div>
    </div>

    <!-- Add record dialog -->
    <q-dialog v-model="addOpen" @hide="resetAddForm">
      <q-card style="min-width:300px;max-width:480px;width:90vw">
        <q-card-section class="q-pb-none">
          <div class="text-subtitle2 text-weight-bold">Add health record</div>
        </q-card-section>

        <q-card-section class="q-gutter-sm">
          <q-input v-model="addForm.title" label="Title" outlined dense placeholder="e.g. Blood test March 2026" />

          <q-btn-toggle
            v-model="addForm.type"
            :options="[{ label: 'Plain text', value: 'text' }, { label: 'PDF or image', value: 'file' }]"
            unelevated dense spread
            color="primary"
            text-color="grey-7"
            toggle-color="primary"
            class="q-mt-sm"
          />

          <q-input
            v-if="addForm.type === 'text'"
            v-model="addForm.content"
            type="textarea"
            label="Content"
            outlined
            autogrow
            placeholder="Paste doctor notes, test results, diagnoses..."
          />

          <div v-else class="q-mt-sm">
            <q-file
              v-model="addForm.pdfFile"
              label="Choose PDF or image"
              outlined
              dense
              accept=".pdf,.jpg,.jpeg"
              hint="Claude will extract the medical data automatically"
            >
              <template #prepend><q-icon name="attach_file" /></template>
            </q-file>
          </div>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-6" v-close-popup />
          <q-btn
            unelevated label="Save"
            color="primary"
            :loading="addLoading"
            :disable="!addForm.title || (addForm.type === 'text' ? !addForm.content : !addForm.pdfFile)"
            @click="submitRecord"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- View / Edit record dialog -->
    <q-dialog v-model="viewOpen" @hide="editMode = false">
      <q-card style="min-width:300px;max-width:600px;width:90vw;max-height:85vh" class="column">

        <q-card-section class="q-pb-none row items-start justify-between no-wrap">
          <div class="col">
            <q-input
              v-if="editMode"
              v-model="editForm.title"
              dense outlined label="Title"
              class="q-mb-xs"
            />
            <div v-else class="text-subtitle2 text-weight-bold">{{ viewRecord?.title }}</div>

            <q-input
              v-if="editMode"
              v-model="editForm.date"
              dense outlined label="Date"
              type="date"
              style="max-width:180px"
            />
            <div v-else class="text-caption text-grey-6">{{ viewRecord ? formatDate(viewRecord.createdAt) : '' }}</div>
          </div>
          <div class="row q-gutter-xs q-ml-sm">
            <q-btn flat round dense :icon="editMode ? 'close' : 'edit'" size="sm" color="grey-6" @click="toggleEdit" />
            <q-btn flat round dense icon="close" size="sm" color="grey-6" v-close-popup />
          </div>
        </q-card-section>

        <q-card-section class="col scroll">
          <!-- Original file preview (only in view mode) -->
          <template v-if="!editMode && viewRecord?.fileData">
            <div class="q-mb-sm">
              <img
                v-if="viewRecord.sourceType === 'image'"
                :src="`data:${viewRecord.fileMimeType};base64,${viewRecord.fileData}`"
                style="max-width:100%;border-radius:8px"
              />
              <q-btn
                v-else-if="viewRecord.sourceType === 'pdf'"
                unelevated
                color="primary"
                icon="open_in_new"
                label="View PDF"
                size="sm"
                @click="openOriginalFile"
              />
            </div>
            <q-separator class="q-mb-sm" />
            <div class="text-caption text-grey-6 q-mb-xs">Extracted text</div>
          </template>

          <q-input
            v-if="editMode"
            v-model="editForm.content"
            type="textarea"
            outlined
            autogrow
            label="Content"
          />
          <pre v-else class="text-body2" style="white-space:pre-wrap;word-break:break-word;font-family:inherit">{{ viewRecord?.content }}</pre>
        </q-card-section>

        <q-card-actions align="right">
          <template v-if="editMode">
            <q-btn flat label="Cancel" color="grey-6" @click="toggleEdit" />
            <q-btn unelevated label="Save" color="primary" :loading="editLoading" @click="saveEdit" />
          </template>
          <q-btn v-else flat label="Close" color="grey-6" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from 'src/api/client'
import type { MedicalRecord } from 'src/api/client'

const records      = ref<MedicalRecord[]>([])
const addOpen      = ref(false)
const addLoading   = ref(false)
const viewOpen     = ref(false)
const viewRecord   = ref<MedicalRecord | null>(null)
const editMode     = ref(false)
const editLoading  = ref(false)
const editForm     = ref({ title: '', content: '', date: '' })

const insightLoading = ref(false)
const chatMessages   = ref<{ role: 'user' | 'assistant'; content: string }[]>([])
const chatInput      = ref('')
const chatLoading    = ref(false)

const addForm = ref({
  title:   '',
  type:    'text' as 'text' | 'file',
  content: '',
  pdfFile: null as File | null,
})

onMounted(loadRecords)

function resetAddForm() {
  addForm.value = { title: '', type: 'text' as 'text' | 'file', content: '', pdfFile: null }
}

async function loadRecords() {
  const { data } = await api.get<{ records: MedicalRecord[] }>('/api/medical')
  records.value = data.records
}

async function submitRecord() {
  addLoading.value = true
  try {
    let body: Record<string, unknown>
    if (addForm.value.type === 'file' && addForm.value.pdfFile) {
      const b64 = await fileToBase64(addForm.value.pdfFile)
      if (addForm.value.pdfFile.type === 'application/pdf') {
        body = { title: addForm.value.title, pdf: b64 }
      } else {
        body = { title: addForm.value.title, image: b64 }
      }
    } else {
      body = { title: addForm.value.title, content: addForm.value.content }
    }
    await api.post('/api/medical', body)
    addOpen.value = false
    await loadRecords()
  } finally {
    addLoading.value = false
  }
}

async function openRecord(id: string) {
  const { data } = await api.get<MedicalRecord>(`/api/medical/${id}`)
  viewRecord.value = data
  editMode.value   = false
  viewOpen.value   = true
}

function toggleEdit() {
  if (!editMode.value && viewRecord.value) {
    editForm.value = {
      title:   viewRecord.value.title,
      content: viewRecord.value.content ?? '',
      date:    viewRecord.value.createdAt.slice(0, 10),
    }
  }
  editMode.value = !editMode.value
}

async function saveEdit() {
  if (!viewRecord.value) return
  editLoading.value = true
  try {
    const { data } = await api.put<MedicalRecord>(`/api/medical/${viewRecord.value.id}`, {
      title:   editForm.value.title,
      content: editForm.value.content,
      date:    editForm.value.date || null,
    })
    viewRecord.value = { ...viewRecord.value, ...data, content: editForm.value.content }
    editMode.value = false
    await loadRecords()
  } finally {
    editLoading.value = false
  }
}

async function deleteRecord(id: string) {
  await api.delete(`/api/medical/${id}`)
  await loadRecords()
}

async function getInsight() {
  insightLoading.value = true
  try {
    const { data } = await api.post<{ insight: string }>('/api/medical/insight', {})
    chatMessages.value = [{ role: 'assistant', content: data.insight }]
  } finally {
    insightLoading.value = false
  }
}

async function sendChat() {
  const text = chatInput.value.trim()
  if (!text) return
  chatInput.value = ''
  chatMessages.value.push({ role: 'user', content: text })
  chatLoading.value = true
  try {
    const { data } = await api.post<{ reply: string }>('/api/medical/chat', {
      messages: chatMessages.value,
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

function openOriginalFile() {
  if (!viewRecord.value?.fileData || !viewRecord.value?.fileMimeType) return
  const byteChars = atob(viewRecord.value.fileData)
  const bytes = new Uint8Array(byteChars.length)
  for (let i = 0; i < byteChars.length; i++) bytes[i] = byteChars.charCodeAt(i)
  const blob = new Blob([bytes], { type: viewRecord.value.fileMimeType })
  window.open(URL.createObjectURL(blob), '_blank')
}

function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload  = () => resolve((reader.result as string).split(',')[1] ?? '')
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

function formatDate(ts: string) {
  return new Date(ts).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}
</script>
