import {reactive} from 'vue'
import request from './request'
export const preferences=reactive({theme:'system',editorFont:'Arial',editorSize:15,editorLineHeight:1.8,editorWidth:'comfortable',defaultView:'/dashboard',searchShortcut:'k',notifications:{system:true,collab:true,task:true,meeting:true}})
export const accountSettings=reactive({language:'zh-CN',timezone:'Asia/Shanghai',emailNotify:true,pushNotify:true,privacyLevel:1})
const media=window.matchMedia('(prefers-color-scheme: dark)')
export function applyPreferences(){const dark=preferences.theme==='dark'||preferences.theme==='system'&&media.matches;document.documentElement.dataset.theme=dark?'dark':'light';document.documentElement.style.setProperty('--editor-font',preferences.editorFont);document.documentElement.style.setProperty('--editor-size',preferences.editorSize+'px');document.documentElement.style.setProperty('--editor-line-height',String(preferences.editorLineHeight));document.documentElement.style.setProperty('--editor-width',preferences.editorWidth==='wide'?'100%':'860px')}
media.addEventListener('change',applyPreferences)
export async function loadPreferences(){const r:any=await request.get('/user/settings');Object.assign(accountSettings,r.data);Object.assign(preferences,r.data.preferences||{});applyPreferences()}
export async function savePreferences(){await request.put('/user/settings',{...accountSettings,preferences:{...preferences}});applyPreferences()}
