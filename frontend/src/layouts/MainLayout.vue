<template>
  <q-layout view="lHh LpR lFf">
    <q-header elevated class="bg-primary">
      <q-toolbar>
        <q-toolbar-title class="text-weight-bold">LuxVita</q-toolbar-title>
      </q-toolbar>
    </q-header>

    <q-page-container>
      <router-view />
    </q-page-container>

    <q-footer>
      <q-tabs
        v-model="activeTab"
        dense
        class="bg-white text-grey-7"
        active-color="primary"
        indicator-color="primary"
      >
        <q-route-tab icon="today"         label="Today"   to="/today"   />
        <q-route-tab icon="add_circle"    label="Log"     to="/log"     />
        <q-route-tab icon="trending_up"   label="Trends"  to="/trends"  />
        <q-route-tab icon="person"        label="Profile" to="/profile" />
      </q-tabs>
    </q-footer>
  </q-layout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
const activeTab = ref('today')
</script>
