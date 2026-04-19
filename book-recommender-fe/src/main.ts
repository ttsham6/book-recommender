import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import liff from '@line/liff'

const app = createApp(App)

app.use(router)

// LIFF Initialization mock/setup
// Replace 'YOUR_LIFF_ID' with actual LIFF ID when ready
const LIFF_ID = import.meta.env.VITE_LIFF_ID || ''

const initializeLiff = async () => {
  if (!LIFF_ID) {
    console.warn('LIFF ID is not set. Running in browser mode.')
    app.mount('#app')
    return
  }

  try {
    await liff.init({ liffId: LIFF_ID })
    console.log('LIFF initialized')
    app.mount('#app')
  } catch (err) {
    console.error('LIFF initialization failed', err)
    // Even if LIFF fails, we might want to mount the app for testing
    app.mount('#app')
  }
}

initializeLiff()
