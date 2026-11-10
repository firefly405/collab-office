<template><SystemAdminPage v-if="identity.isAdmin"/><RoleDashboard v-else/></template>
<script setup lang="ts">
import {useIdentityStore} from '@/stores/identity'
import SystemAdminPage from './SystemAdminPage.vue'
import RoleDashboard from './RoleDashboard.vue'
const identity=useIdentityStore()
</script>
