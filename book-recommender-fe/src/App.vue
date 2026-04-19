<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <main class="app-container">
    <RouterView />
  </main>
</template>

<style>
/* Base Global Styles */
:root {
  --bg-color-start: #f6f8fb;
  --bg-color-end: #e5ebf4;
  --text-main: #1a1a1a;
}

body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Arial, 'Hiragino Kaku Gothic ProN', 'Hiragino Sans', Meiryo, sans-serif;
  background: linear-gradient(135deg, var(--bg-color-start) 0%, var(--bg-color-end) 100%);
  min-height: 100vh;
  color: var(--text-main);
  -webkit-font-smoothing: antialiased;
}

.app-container {
  min-height: 100vh;
  position: relative;
  overflow-x: hidden;
}

/* Background decoration */
.app-container::before {
  content: '';
  position: absolute;
  top: -100px;
  right: -100px;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(0, 195, 0, 0.1) 0%, rgba(255,255,255,0) 70%);
  border-radius: 50%;
  z-index: -1;
}

.app-container::after {
  content: '';
  position: absolute;
  bottom: -50px;
  left: -50px;
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(0, 100, 255, 0.08) 0%, rgba(255,255,255,0) 70%);
  border-radius: 50%;
  z-index: -1;
}

* {
  box-sizing: border-box;
}
</style>
