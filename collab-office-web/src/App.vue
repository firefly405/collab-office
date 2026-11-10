<template>
  <el-config-provider :locale="accountSettings.language==='en-US'?en:zh"><router-view /></el-config-provider>
</template>
<script setup lang="ts">
import en from 'element-plus/es/locale/lang/en'
import zh from 'element-plus/es/locale/lang/zh-cn'
import {accountSettings} from '@/utils/preferences'
</script>
