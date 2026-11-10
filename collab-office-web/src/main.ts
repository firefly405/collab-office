import { createApp } from 'vue'
import ElementPlus, { ElMessage } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import '@fontsource-variable/inter'
import '@fontsource-variable/jetbrains-mono'
import '@fontsource-variable/material-symbols-outlined'
import './style.css'
import './assets/styles/design-system.css'
import './assets/styles/typography.css'
import './assets/styles/element-override.scss'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import pinia from './stores'
import { isLocalDemoUrl } from './utils/local-demo'

document.addEventListener('click', event => {
  const link = event.target instanceof Element ? event.target.closest('a[href]') : null
  if (link && !isLocalDemoUrl(link.getAttribute('href') || '', location.href)) {
    event.preventDefault()
    event.stopImmediatePropagation()
    ElMessage.info('本机演示仅支持本地链接')
  }
}, true)

const app = createApp(App)

app.use(ElementPlus, { size: 'default', locale: zhCn })
app.use(router)
app.use(pinia)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.mount('#app')
