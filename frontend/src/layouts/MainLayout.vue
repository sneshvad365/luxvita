<template>
  <q-layout view="lHh LpR lFf">
    <q-header elevated class="bg-primary">
      <q-toolbar>
        <!-- Fork + leaf logo mark -->
        <svg width="28" height="28" viewBox="0 0 512 512" fill="none" xmlns="http://www.w3.org/2000/svg" style="margin-right:10px;flex-shrink:0">
          <rect width="512" height="512" rx="112" fill="rgba(255,255,255,0.15)"/>
          <line x1="152" y1="100" x2="152" y2="220" stroke="white" stroke-width="32" stroke-linecap="round"/>
          <line x1="196" y1="100" x2="196" y2="220" stroke="white" stroke-width="32" stroke-linecap="round"/>
          <line x1="240" y1="100" x2="240" y2="220" stroke="white" stroke-width="32" stroke-linecap="round"/>
          <path d="M152 220 Q152 272 196 272 Q240 272 240 220" stroke="white" stroke-width="32" fill="none"/>
          <line x1="196" y1="272" x2="196" y2="412" stroke="white" stroke-width="32" stroke-linecap="round"/>
          <path d="M320 88 C320 88 448 140 444 256 C441 340 374 400 340 412 C340 412 268 360 288 280 C304 216 320 88 320 88Z" fill="white"/>
        </svg>

        <q-toolbar-title>LuxVita</q-toolbar-title>

        <!-- Decorative nutrition icons — right side -->
        <div class="header-deco" aria-hidden="true">
          <!-- Apple -->
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M12 6C9 6 6.5 8.5 6.5 12C6.5 15.5 8.5 18.5 12 18.5C15.5 18.5 17.5 15.5 17.5 12C17.5 8.5 15 6 12 6Z" fill="white" opacity="0.55"/>
            <path d="M12 6C12 6 12 4 14 3" stroke="white" stroke-width="1.4" stroke-linecap="round" opacity="0.55"/>
            <path d="M11.5 6C11.5 6 9.5 4.5 8 4" stroke="white" stroke-width="1.4" stroke-linecap="round" opacity="0.55"/>
          </svg>
          <!-- Leaf -->
          <svg width="18" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M12 3C12 3 20 7 19 14C18.5 18 15 21 12 21C12 21 7 17 9 12C10.5 8 12 3 12 3Z" fill="white" opacity="0.55"/>
            <path d="M12 5L13 19" stroke="rgba(255,255,255,0.4)" stroke-width="1.2" stroke-linecap="round"/>
          </svg>
          <!-- Water drop -->
          <svg width="16" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M12 3L18 12C18 16 15.5 20 12 20C8.5 20 6 16 6 12Z" fill="white" opacity="0.55"/>
          </svg>
        </div>
      </q-toolbar>
    </q-header>

    <q-page-container>
      <router-view />
    </q-page-container>

    <q-footer>
      <q-tabs
        v-model="activeTab"
        dense
        class="bg-white text-grey-7 nav-tabs"
        active-color="primary"
        indicator-color="primary"
      >
        <q-route-tab icon="today"         label="Today"   to="/today"   />
        <q-route-tab icon="add_circle"    label="Log"     to="/log"     />
        <q-route-tab icon="trending_up"   label="Trends"  to="/trends"  />
        <q-route-tab icon="favorite"      label="Health"  to="/health"  />
        <q-route-tab icon="person"        label="Profile" to="/profile" />
      </q-tabs>
    </q-footer>
  </q-layout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
const activeTab = ref('today')
</script>

<style>
.nav-tabs .q-tab__label {
  font-size: 9px !important;
  line-height: 1.2;
}
.nav-tabs .q-tab {
  min-width: 0 !important;
  padding: 0 4px !important;
}
/* Keep footer fixed regardless of any app.scss z-index stacking tricks */
.q-footer {
  position: fixed !important;
}
</style>
