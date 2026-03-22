<template>
  <q-layout view="lHh lpr lFf">
    <q-page-container>
      <q-page class="login-page flex flex-center">

        <!-- Card -->
        <q-card class="login-card">

          <!-- Logo -->
          <div class="text-center q-pt-xl q-pb-sm">
            <img src="/luxvita/favicon.svg" alt="LuxVita" class="login-logo" />
          </div>

          <q-card-section class="text-center q-pb-sm q-pt-sm">
            <div class="login-title">LuxVita</div>
            <div class="login-subtitle">Nutrition tracking, powered by AI</div>
          </q-card-section>

          <!-- Divider with red dots -->
          <div class="login-divider">
            <span class="login-divider__dot" />
            <span class="login-divider__line" />
            <span class="login-divider__dot" />
          </div>

          <q-card-section class="q-pt-md">
            <q-form @submit.prevent="submit" class="q-gutter-y-md">
              <q-input
                v-model="email"
                type="email"
                label="Email"
                outlined
                dense
                bg-color="white"
                :rules="[v => !!v || 'Required']"
              />
              <q-input
                v-model="password"
                type="password"
                label="Password"
                outlined
                dense
                bg-color="white"
                :rules="[v => !!v || 'Required']"
              />

              <q-banner v-if="error" class="bg-red-1 text-red-8 rounded-borders" dense>
                {{ error }}
              </q-banner>

              <q-btn
                type="submit"
                label="Sign In"
                color="primary"
                class="full-width"
                :loading="loading"
                unelevated
              />
            </q-form>
          </q-card-section>

          <q-card-section class="text-center q-pt-sm q-pb-xl">
            <span style="color:#8298AF;font-size:1rem">Don't have an account? </span>
            <router-link to="/register" class="login-link" style="font-size:1rem">Sign up</router-link>
          </q-card-section>

        </q-card>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from 'src/stores/auth'

const router   = useRouter()
const auth     = useAuthStore()
const email    = ref('')
const password = ref('')
const loading  = ref(false)
const error    = ref('')

async function submit() {
  error.value   = ''
  loading.value = true
  try {
    await auth.login(email.value, password.value)
    await router.push('/today')
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    error.value = msg ?? 'Login failed'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ── Full-page background — matches app gradient ──────────────── */
.login-page {
  min-height: 100vh;
  background:
    radial-gradient(ellipse at 5%  5%,  rgba(27, 79, 138, 0.18) 0%, transparent 55%),
    radial-gradient(ellipse at 95% 95%, rgba(212, 43, 43, 0.14) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 10%, rgba(212, 43, 43, 0.07) 0%, transparent 40%),
    radial-gradient(ellipse at 15% 90%, rgba(27, 79, 138, 0.07) 0%, transparent 40%),
    linear-gradient(150deg, #C8DCF5 0%, #E4EFF9 25%, #F4F6FA 55%, #F9E8E8 80%, #F5D5D5 100%);
  position: relative;
  overflow: hidden;
}

/* ── Card ─────────────────────────────────────────────────────── */
.login-card {
  position: relative;
  width: 360px;
  border-radius: 20px !important;
  background: #fff !important;
  border: 1px solid rgba(255, 255, 255, 0.9) !important;
  box-shadow:
    0 4px 24px rgba(18, 36, 58, 0.13),
    0 1px 4px  rgba(18, 36, 58, 0.07);
  overflow: hidden;
}

/* ── Logo ─────────────────────────────────────────────────────── */
.login-logo {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  box-shadow:
    0 8px 24px rgba(27, 79, 138, 0.35),
    0 0 0 3px rgba(212, 43, 43, 0.18);
  transition: transform 0.2s ease;
}
.login-logo:hover {
  transform: scale(1.04);
}

/* ── Typography ───────────────────────────────────────────────── */
.login-title {
  font-size: 1.7rem;
  font-weight: 800;
  letter-spacing: -0.5px;
  color: #12243A;
}
.login-subtitle {
  font-size: 0.78rem;
  color: #8298AF;
  margin-top: 2px;
  letter-spacing: 0.3px;
}

/* ── Divider ──────────────────────────────────────────────────── */
.login-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 4px 24px 0;
}
.login-divider__line {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, transparent, #E4EAF0, transparent);
}
.login-divider__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #D42B2B;
  opacity: 0.7;
}

/* ── Sign-up link ─────────────────────────────────────────────── */
.login-link {
  color: #D42B2B;
  font-size: 0.75rem;
  font-weight: 600;
  text-decoration: none;
}
.login-link:hover {
  text-decoration: underline;
}
</style>
