import {defineStore} from 'pinia'
import {computed,ref} from 'vue'
import request from '@/utils/request'

export const useIdentityStore=defineStore('identity',()=>{
  const user=ref<any>(null)
  const role=computed(()=>user.value?.platformRole||'MEMBER')
  const isAdmin=computed(()=>role.value==='SYSTEM_ADMIN')
  const isManager=computed(()=>role.value==='PROJECT_ADMIN')
  const roleName=computed(()=>isAdmin.value?'系统管理员':isManager.value?'项目管理员':'普通用户')
  async function load(){const result:any=await request.get('/user/info');user.value=result.data}
  function reset(){user.value=null}
  return {user,role,isAdmin,isManager,roleName,load,reset}
})
