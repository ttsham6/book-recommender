<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import BookCard from '../components/BookCard.vue'

const props = defineProps<{
  id: string
}>()

const router = useRouter()
const isLoading = ref(true)
const recommendations = ref<{ id: string; title: string; author: string; description: string }[]>([])
const baseBook = ref<{ title: string; author: string } | null>(null)

onMounted(() => {
  // Simulate fetching base book info and recommendations
  setTimeout(() => {
    baseBook.value = { title: 'モモ', author: 'ミヒャエル・エンデ' }
    
    recommendations.value = [
      { 
        id: '101', 
        title: 'はてしない物語', 
        author: 'ミヒャエル・エンデ',
        description: '「モモ」が好きなあなたへ。同じ著者によるファンタジーの傑作です。'
      },
      { 
        id: '102', 
        title: 'アルケミスト', 
        author: 'パウロ・コエーリョ',
        description: '自分の人生の宝物を探す旅。ファンタジーでありながら哲学的な一冊です。'
      },
      { 
        id: '103', 
        title: 'クラバート', 
        author: 'オトフリート・プロイスラー',
        description: 'ドイツの児童文学。魔法と友情、そして自己犠牲を描く名作。'
      }
    ]
    isLoading.value = false
  }, 1500)
})

const goBack = () => {
  router.push({ name: 'search' })
}
</script>

<template>
  <div class="recommend-view">
    <button class="back-button" @click="goBack">
      ← 戻る
    </button>

    <div v-if="isLoading" class="loading-state">
      <div class="spinner"></div>
      <p>あなたにぴったりな本を厳選しています...</p>
    </div>

    <div v-else class="content">
      <header class="header">
        <p class="subtitle">「{{ baseBook?.title }}」が好きなあなたへ</p>
        <h1>おすすめの3冊</h1>
      </header>

      <div class="recommendations-list">
        <transition-group name="fade" appear>
          <BookCard 
            v-for="(book, index) in recommendations" 
            :key="book.id"
            :id="book.id"
            :title="book.title"
            :author="book.author"
            :description="book.description"
            :style="{ transitionDelay: `${index * 150}ms` }"
          />
        </transition-group>
      </div>

      <div class="action-area">
        <button class="share-button">
          友だちにシェアする
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.recommend-view {
  padding: 24px 16px;
  max-width: 600px;
  margin: 0 auto;
}

.back-button {
  background: transparent;
  border: none;
  color: #666;
  font-size: 0.9rem;
  cursor: pointer;
  padding: 8px 0;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}

.header {
  text-align: center;
  margin-bottom: 32px;
}

.subtitle {
  font-size: 0.9rem;
  color: #00c300;
  font-weight: 600;
  margin-bottom: 4px;
}

.header h1 {
  font-size: 1.6rem;
  color: #1a1a1a;
}

.loading-state {
  text-align: center;
  padding: 60px 0;
  color: #666;
}

.spinner {
  width: 50px;
  height: 50px;
  border: 4px solid rgba(0, 195, 0, 0.2);
  border-top-color: #00c300;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.action-area {
  margin-top: 40px;
  text-align: center;
}

.share-button {
  background: #00c300;
  color: white;
  border: none;
  padding: 14px 32px;
  border-radius: 24px;
  font-size: 1rem;
  font-weight: bold;
  width: 100%;
  max-width: 300px;
  box-shadow: 0 4px 12px rgba(0, 195, 0, 0.3);
  cursor: pointer;
  transition: transform 0.2s;
}

.share-button:active {
  transform: scale(0.95);
}

/* Animations */
.fade-enter-active {
  transition: opacity 0.6s ease, transform 0.6s ease;
}
.fade-enter-from {
  opacity: 0;
  transform: translateY(20px);
}
</style>
