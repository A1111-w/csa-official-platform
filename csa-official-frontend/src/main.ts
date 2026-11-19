import { createApp } from 'vue'
import { pinia } from '@/store/pinia'
import router from '@/router'
import App from './App.vue'
import './style.css'
import './workspace.css'

let theme = 'dark'
try {
  if (localStorage.getItem('csa-theme') === 'light') theme = 'light'
} catch { /* Use the default palette when browser storage is unavailable. */ }
document.documentElement.dataset.theme = theme

createApp(App).use(pinia).use(router).mount('#app')
