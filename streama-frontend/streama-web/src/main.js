import './assets/base.scss'
import './assets/iconfont/iconfont.js'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import VueCookies from 'vue-cookies'

const app = createApp(App)

app.use(createPinia())
app.use(router)

app.use(ElementPlus)

app.config.globalProperties.VueCookies = VueCookies
app.mount('#app')
