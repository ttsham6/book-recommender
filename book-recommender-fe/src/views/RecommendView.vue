<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchRecommendations } from '../api/books'
import BookCard from '../components/BookCard.vue'
import type { Book } from '../types/book'

const router = useRouter()
const route = useRoute()

const isLoading = ref(true)
const errorMessage = ref('')
const recommendations = ref<Book[]>([])
const selectedBook = ref<Book>({
  id: '',
  title: '',
  author: '',
  imageUrl: undefined
})

const recommendationQuery = computed(() => {
  const q = route.query.q
  return typeof q === 'string' ? q : ''
})

const loadRecommendations = async () => {
  if (!recommendationQuery.value) {
    errorMessage.value = '推薦クエリ不足。検索画面から選択。'
    isLoading.value = false
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    const items = await fetchRecommendations(recommendationQuery.value)
    recommendations.value = items.filter((item) => {
      return !(
        item.id === selectedBook.value.id ||
        (item.title === selectedBook.value.title &&
          item.author === selectedBook.value.author)
      )
    })
  } catch (error) {
    console.error(error)
    recommendations.value = []
    errorMessage.value = '推薦取得失敗。API接続確認。'
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  selectedBook.value = {
    id: typeof route.query.id === 'string' ? route.query.id : '',
    title: typeof route.query.title === 'string' ? route.query.title : '',
    author: typeof route.query.author === 'string' ? route.query.author : '',
    imageUrl:
      typeof route.query.imageUrl === 'string' ? route.query.imageUrl : undefined
  }

  void loadRecommendations()
})

const goBack = () => {
  router.push({ name: 'search' })
}
</script>

<template>
  <div class="recommend-view">
    <button class="back-button" @click="goBack">
      ← 本選択へ戻る
    </button>

    <section class="hero-panel">
      <p class="eyebrow">RECOMMEND RESULT</p>
      <h1>選んだ一冊から 類似本 取得。</h1>
      <p class="hero-copy">
        選択書籍ベースでおすすめ表示。API `/recommendations` 使用。
      </p>
    </section>

    <section class="selected-section">
      <div class="section-heading">
        <div>
          <p class="section-label">選択した本</p>
          <h2>この本に近い作品</h2>
        </div>
      </div>

      <BookCard
        :id="selectedBook.id"
        :title="selectedBook.title"
        :author="selectedBook.author"
        :image-url="selectedBook.imageUrl"
      />
    </section>

    <section class="recommendation-section">
      <div class="section-heading">
        <div>
          <p class="section-label">おすすめ結果</p>
          <h2>類似書籍 一覧</h2>
        </div>
      </div>

      <div v-if="isLoading" class="loading-panel">
        <div class="spinner"></div>
        <p>おすすめ取得中...</p>
      </div>

      <div v-else-if="errorMessage" class="empty-panel">
        <p class="empty-title">通信失敗</p>
        <p class="empty-copy">{{ errorMessage }}</p>
      </div>

      <div v-else-if="recommendations.length === 0" class="empty-panel">
        <p class="empty-title">おすすめなし</p>
        <p class="empty-copy">別の書籍で再検索</p>
      </div>

      <transition-group v-else name="fade" tag="div" class="recommendations-list" appear>
        <BookCard
          v-for="(book, index) in recommendations"
          :key="book.id"
          :id="book.id"
          :title="book.title"
          :author="book.author"
          :image-url="book.imageUrl"
          :style="{ transitionDelay: `${index * 120}ms` }"
        />
      </transition-group>
    </section>
  </div>
</template>

<style scoped>
.recommend-view {
  max-width: 760px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.back-button {
  margin-bottom: 14px;
  padding: 0;
  border: none;
  background: transparent;
  color: #466074;
  font-size: 0.95rem;
  cursor: pointer;
}

.hero-panel,
.selected-section,
.recommendation-section {
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.56);
  box-shadow: 0 16px 40px rgba(20, 32, 56, 0.08);
  backdrop-filter: blur(18px);
  border-radius: 28px;
}

.hero-panel {
  padding: 28px;
  background:
    radial-gradient(circle at top right, rgba(249, 115, 22, 0.16), transparent 28%),
    rgba(255, 255, 255, 0.84);
}

.eyebrow,
.section-label {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #9a3412;
  font-weight: 700;
}

.hero-panel h1,
.section-heading h2 {
  margin: 10px 0 0;
  color: #112033;
}

.hero-panel h1 {
  font-size: clamp(1.9rem, 4.6vw, 2.9rem);
  line-height: 1.08;
  max-width: 12em;
}

.hero-copy {
  margin: 14px 0 0;
  color: #516477;
  line-height: 1.7;
  max-width: 38rem;
}

.selected-section,
.recommendation-section {
  margin-top: 22px;
  padding: 24px;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
  margin-bottom: 18px;
}

.loading-panel,
.empty-panel {
  display: grid;
  place-items: center;
  min-height: 220px;
  text-align: center;
  color: #64748b;
}

.empty-title {
  margin: 0;
  color: #112033;
  font-size: 1.1rem;
  font-weight: 700;
}

.empty-copy {
  margin: 8px 0 0;
}

.spinner {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 4px solid rgba(249, 115, 22, 0.14);
  border-top-color: #ea580c;
  animation: spin 1s linear infinite;
}

.recommendations-list {
  display: grid;
  gap: 14px;
}

.fade-enter-active {
  transition: opacity 0.55s ease, transform 0.55s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(18px);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 560px) {
  .hero-panel,
  .selected-section,
  .recommendation-section {
    border-radius: 24px;
    padding: 22px;
  }

  .section-heading {
    flex-direction: column;
    align-items: start;
  }
}
</style>
