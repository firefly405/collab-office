<template><div class="auth-page"><div class="auth-brand"><span class="material-symbols-outlined">hub</span>Nexus Office</div><form @submit.prevent="submit"><h1>创建账号</h1><label>用户名<el-input v-model="username" autocomplete="username" maxlength="50" /></label><label>密码<el-input v-model="password" type="password" show-password autocomplete="new-password" maxlength="20" placeholder="8至20位" /></label><label>确认密码<el-input v-model="confirm" type="password" show-password autocomplete="new-password" /></label><el-button type="primary" native-type="submit" :loading="busy">注册</el-button><router-link to="/login">返回登录</router-link></form></div></template>
<script setup lang="ts">
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import request from '@/utils/request'
import {setToken} from '@/utils/token'
const username=ref(''),password=ref(''),confirm=ref(''),busy=ref(false),router=useRouter()
async function submit(){if(!username.value.trim()||password.value.length<8||password.value.length>20){ElMessage.warning('请填写用户名和8至20位密码');return}if(password.value!==confirm.value){ElMessage.warning('两次密码不一致');return}busy.value=true;try{const r:any=await request.post('/auth/register',{username:username.value.trim(),password:password.value});setToken(r.data.accessToken,r.data.refreshToken);localStorage.removeItem('active_space_id');await router.replace('/dashboard')}catch{}finally{busy.value=false}}
</script>
<style src="./local-auth.css"></style>
