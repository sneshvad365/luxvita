<template>
  <q-layout view="lHh LpR lFf">
    <q-header elevated class="bg-primary">
      <q-toolbar>
        <img src="/luxvita/favicon.svg" alt="" style="width:28px;height:28px;margin-right:10px;flex-shrink:0" />
        <q-toolbar-title>LuxVita</q-toolbar-title>
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
import { ref, onMounted } from 'vue'
import { useProfileStore } from 'src/stores/profile'

const activeTab    = ref('today')
const profileStore = useProfileStore()
onMounted(() => profileStore.fetch())
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
