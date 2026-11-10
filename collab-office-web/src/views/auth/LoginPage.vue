<template><div class="auth-page"><div class="auth-brand"><span class="material-symbols-outlined">hub</span>Nexus Office</div><form @submit.prevent="submit"><h1>登录</h1><label>账号<el-input v-model="account" autocomplete="username" maxlength="50" /></label><label>密码<el-input v-model="password" type="password" show-password autocomplete="current-password" /></label><el-button type="primary" native-type="submit" :loading="busy">登录</el-button><router-link to="/register">创建账号</router-link></form></div></template>
<script setup lang="ts">
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import request from '@/utils/request'
import {setToken} from '@/utils/token'
import {loadPreferences,preferences} from '@/utils/preferences'
const account=ref(''),password=ref(''),busy=ref(false),router=useRouter()
async function submit(){if(!account.value.trim()||!password.value){ElMessage.warning('请填写账号和密码');return}busy.value=true;try{const r:any=await request.post('/auth/login',{account:account.value.trim(),password:password.value});setToken(r.data.accessToken,r.data.refreshToken);localStorage.setItem('user_info',JSON.stringify(r.data));try{await loadPreferences()}catch{}await router.replace(preferences.defaultView)}catch{}finally{busy.value=false}}
</script>
<style src="./local-auth.css"></style>
