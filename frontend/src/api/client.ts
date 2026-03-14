import axios, { type AxiosInstance } from 'axios'
import { LocalStorage } from 'quasar'

// In dev, use relative URLs so Vite's proxy handles routing to the backend.
// In production builds, set VITE_API_BASE_URL to the real backend origin.
const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.PROD ? (import.meta.env.VITE_API_BASE_URL ?? '') : '',
  timeout: 30_000,
})

// Inject JWT on every request
api.interceptors.request.use((config) => {
  const token = LocalStorage.getItem<string>('jwt')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Redirect to login on 401
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      LocalStorage.remove('jwt')
      LocalStorage.remove('userId')
      window.location.replace(window.location.pathname + '#/login')
    }
    return Promise.reject(err)
  },
)

export default api

// ── Typed request helpers ────────────────────────────────────────────────────

export interface AuthResponse {
  token:  string
  userId: string
}

export interface MacroEstimate {
  kcal:        number
  proteinG:    number
  carbsG:      number
  fatG:        number
  fiberG:      number
  description: string
  waterMl:     number | null
}

export interface BreakdownItem {
  item:     string
  kcal:     number
  proteinG: number
  carbsG:   number
  fatG:     number
  fiberG:   number
}

export interface Meal {
  id:          string
  userId:      string
  loggedAt:    string
  description: string | null
  hasPhoto:    boolean
  kcal:        number | null
  proteinG:    number | null
  carbsG:      number | null
  fatG:        number | null
  fiberG:      number | null
  breakdown:   BreakdownItem[]
}

export interface Macros {
  kcal:     number
  proteinG: number
  carbsG:   number
  fatG:     number
  fiberG:   number
}

export interface TodayResponse {
  meals:  Meal[]
  totals: Macros
}

export interface ParsedActivity {
  type:        string
  durationMin: number | null
  intensity:   string | null
  steps:       number | null
  mood:        string | null
  notes:       string | null
}

export interface ActivityLogResponse {
  id:       string
  entry:    string
  parsed:   ParsedActivity | null
  loggedAt: string
}

export interface WeightEntry {
  loggedAt:  string
  weightKg:  number
  rolling7d: number
}

export interface WeightTrendResponse {
  entries:       WeightEntry[]
  sevenDayAvg:   number | null
  paceKgPerWeek: number | null
  etaDays:       number | null
}

export interface UserProfile {
  id:             string
  userId:         string
  bio:            string | null
  goal:           'fat_loss' | 'muscle_gain' | 'maintenance'
  targetKcal:     number
  targetProteinG: number
  targetCarbsG:   number
  targetFatG:     number
  targetFiberG:   number
  targetWaterL:   number
  baseWeightKg:   number | null
  goalWeightKg:   number | null
  updatedAt:      string
}

export interface Insight {
  insight: string
  type:    'protein' | 'timing' | 'fiber' | 'hydration' | 'recovery' | 'weight'
}

export interface MedicalRecord {
  id:         string
  title:      string
  sourceType: 'text' | 'pdf'
  createdAt:  string
  content?:   string
}

export interface TrendsSummary {
  sevenDayAvg:  Macros
  thirtyDayAvg: Macros
  consistency: {
    kcal:    number
    protein: number
    fiber:   number
  }
}
