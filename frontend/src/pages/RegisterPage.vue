<template>
  <q-layout view="lHh lpr lFf">
  <q-page-container>
  <q-page class="flex flex-center bg-grey-1">
    <q-card style="width: 340px" class="q-pa-md">
      <q-card-section class="text-center q-pb-none">
        <div class="text-h5 text-weight-bold text-primary">Create account</div>
      </q-card-section>

      <q-card-section>
        <q-form @submit.prevent="submit" class="q-gutter-md">
          <q-input
            v-model="email"
            type="email"
            label="Email"
            outlined
            dense
            :rules="[v => !!v || 'Required']"
          />
          <q-input
            v-model="password"
            type="password"
            label="Password"
            hint="At least 8 characters"
            outlined
            dense
            :rules="[v => v.length >= 8 || 'Min 8 characters']"
          />

          <q-banner v-if="error" class="bg-red-1 text-red-8 rounded-borders" dense>
            {{ error }}
          </q-banner>

          <q-btn
            type="submit"
            label="Create account"
            color="primary"
            class="full-width"
            :loading="loading"
            unelevated
          />
        </q-form>
      </q-card-section>

      <q-card-section class="text-center q-pt-none">
        <span class="text-caption text-grey-6">Already have an account? </span>
        <router-link to="/login" class="text-primary text-caption">Sign in</router-link>
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
    await auth.register(email.value, password.value)
    await router.push('/profile')
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    error.value = msg ?? 'Registration failed'
  } finally {
    loading.value = false
  }
}
</script>
