import { createApp } from 'vue'
import { pinia } from '@/store/pinia'
import router from '@/router'
import App from './App.vue'
import './style.css'

createApp(App).use(pinia).use(router).mount('#app')
