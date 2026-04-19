<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import BookCard from '../components/BookCard.vue'

const router = useRouter()
const searchQuery = ref('')
const isSearching = ref(false)

// Mock data for initial UI
const searchResults = ref<{ id: string; title: string; author: string }[]>([])

const handleSearch = async () => {
  if (!searchQuery.value.trim()) return
  
  isSearching.value = true
  
  // Simulate API call
  setTimeout(() => {
    searchResults.value = [
      { id: '1', title: 'モモ', author: 'ミヒャエル・エンデ' },
      { id: '2', title: '星の王子さま', author: 'サン＝テグジュペリ' },
      { id: '3', title: '銀河鉄道の夜', author: '宮沢賢治' }
    ]
    isSearching.value = false
  }, 1000)
}

const selectBook = (id: string) => {
  router.push({ name: 'recommend', params: { id } })
}
</script>

<template>
  <div class="search-view">
    <header class="header">
      <h1>どんな本が好きですか？</h1>
      <p>お気に入りの一冊から、次に出会う本を探しましょう。</p>
    </header>

    <div class="search-container">
      <div class="search-box">
        <input 
          v-model="searchQuery" 
          type="text" 
          placeholder="本のタイトルや著者名を入力..." 
          @keyup.enter="handleSearch"
        />
        <button @click="handleSearch" :disabled="isSearching || !searchQuery.trim()">
          検索
        </button>
      </div>
    </div>

    <div class="results-container" v-if="isSearching || searchResults.length > 0">
      <div v-if="isSearching" class="loading">
        <div class="spinner"></div>
        <p>探しています...</p>
      </div>
      
      <transition-group v-else name="list" tag="div" class="book-list">
        <BookCard 
          v-for="book in searchResults" 
          :key="book.id"
          :id="book.id"
          :title="book.title"
          :author="book.author"
          @click="selectBook"
        />
      </transition-group>
    </div>
  </div>
</template>

<style scoped>
.search-view {
  padding: 24px 16px;
  max-width: 600px;
  margin: 0 auto;
}

.header {
  text-align: center;
  margin-bottom: 32px;
}

.header h1 {
  font-size: 1.5rem;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.header p {
  font-size: 0.9rem;
  color: #666;
}

.search-box {
  display: flex;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  padding: 6px 6px 6px 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
}

.search-box input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 1rem;
  outline: none;
  color: #333;
}

.search-box button {
  background: linear-gradient(135deg, #00c300, #009900);
  color: white;
  border: none;
  padding: 10px 24px;
  border-radius: 20px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, opacity 0.2s;
}

.search-box button:active {
  transform: scale(0.95);
}

.search-box button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.results-container {
  margin-top: 32px;
}

.loading {
  text-align: center;
  color: #666;
  padding: 40px 0;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(0, 195, 0, 0.2);
  border-top-color: #00c300;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Transitions */
.list-enter-active,
.list-leave-active {
  transition: all 0.4s ease;
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(20px);
}
</style>
