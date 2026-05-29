<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { searchBooks } from '../api/books'
import BookCard from '../components/BookCard.vue'
import type { Book } from '../types/book'

const router = useRouter()
const searchQuery = ref('')
const isSearching = ref(false)
const hasSearched = ref(false)
const errorMessage = ref('')
const searchResults = ref<Book[]>([])

const handleSearch = async () => {
  const query = searchQuery.value.trim()

  if (!query) return

  isSearching.value = true
  hasSearched.value = true
  errorMessage.value = ''

  try {
    searchResults.value = await searchBooks(query)
  } catch (error) {
    console.error(error)
    searchResults.value = []
    errorMessage.value = '検索失敗。API接続確認。'
  } finally {
    isSearching.value = false
  }
}

const selectBook = (bookId: string) => {
  const book = searchResults.value.find((item) => item.id === bookId)

  if (!book) return

  router.push({
    name: 'recommend',
    query: {
      id: book.id,
      title: book.title,
      author: book.author,
      imageUrl: book.imageUrl,
      q: `${book.title} ${book.author}`.trim()
    }
  })
}
</script>

<template>
  <div class="search-view">
    <section class="hero-panel">
      <p class="eyebrow">BOOK RECOMMENDER</p>
      <h1>好きな本 検索。次の一冊 発見。</h1>
      <p class="hero-copy">
        タイトルか著者名で検索。気に入った本を選ぶと類似本を取得。
      </p>

      <div class="search-box">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="タイトル 著者 で検索"
          @keyup.enter="handleSearch"
        />
        <button @click="handleSearch" :disabled="isSearching || !searchQuery.trim()">
          {{ isSearching ? '検索中' : '検索' }}
        </button>
      </div>
    </section>

    <section class="results-section">
      <div class="section-heading">
        <div>
          <p class="section-label">検索結果</p>
          <h2>好きな本 選択</h2>
        </div>
        <p class="section-note">カード押下で推薦画面へ</p>
      </div>

      <div v-if="isSearching" class="loading-panel">
        <div class="spinner"></div>
        <p>候補抽出中...</p>
      </div>

      <div v-else-if="errorMessage" class="empty-panel">
        <p class="empty-title">通信失敗</p>
        <p class="empty-copy">{{ errorMessage }}</p>
      </div>

      <div v-else-if="hasSearched && searchResults.length === 0" class="empty-panel">
        <p class="empty-title">候補なし</p>
        <p class="empty-copy">キーワード変更して再検索</p>
      </div>

      <div v-else-if="!hasSearched" class="empty-panel">
        <p class="empty-title">検索待機中</p>
        <p class="empty-copy">好きな本を入力して検索</p>
      </div>

      <transition-group v-else name="list" tag="div" class="book-list">
        <BookCard
          v-for="book in searchResults"
          :key="book.id"
          :id="book.id"
          :title="book.title"
          :author="book.author"
          :image-url="book.imageUrl"
          @click="selectBook"
        />
      </transition-group>
    </section>
  </div>
</template>

<style scoped>
.search-view {
  max-width: 760px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.hero-panel,
.results-section {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.55);
  backdrop-filter: blur(18px);
  box-shadow: 0 16px 40px rgba(20, 32, 56, 0.08);
  border-radius: 28px;
}

.hero-panel {
  padding: 28px;
  background:
    radial-gradient(circle at top right, rgba(22, 163, 74, 0.14), transparent 30%),
    rgba(255, 255, 255, 0.82);
}

.eyebrow,
.section-label {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #18794e;
  font-weight: 700;
}

.hero-panel h1,
.section-heading h2 {
  margin: 10px 0 0;
  color: #112033;
}

.hero-panel h1 {
  font-size: clamp(2rem, 5vw, 3rem);
  line-height: 1.05;
  max-width: 10em;
}

.hero-copy {
  margin: 16px 0 0;
  color: #4b5c71;
  line-height: 1.7;
  max-width: 34rem;
}

.search-box {
  display: flex;
  gap: 10px;
  margin-top: 24px;
  padding: 8px;
  border-radius: 22px;
  background: rgba(241, 245, 249, 0.9);
}

.search-box input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  padding: 12px 14px;
  color: #112033;
  font-size: 1rem;
}

.search-box button,
.book-list :deep(.book-card) {
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    background-color 0.2s ease,
    border-color 0.2s ease;
}

.search-box button {
  border: none;
  border-radius: 18px;
  padding: 0 20px;
  background: linear-gradient(135deg, #1d9d62, #166534);
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.search-box button:disabled {
  opacity: 0.6;
  cursor: wait;
}

.results-section {
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

.section-note {
  margin: 0;
  color: #66788a;
  font-size: 0.92rem;
}

.search-box button:hover,
.book-list :deep(.book-card:hover) {
  transform: translateY(-2px);
}

.loading-panel,
.empty-panel {
  display: grid;
  place-items: center;
  min-height: 180px;
  text-align: center;
  color: #607282;
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

.book-list {
  display: grid;
  gap: 14px;
}

.spinner {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  border: 4px solid rgba(29, 155, 98, 0.18);
  border-top-color: #1d9d62;
  animation: spin 1s linear infinite;
}

.list-enter-active,
.list-leave-active {
  transition: all 0.35s ease;
}

.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(16px);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 720px) {
  .section-heading {
    flex-direction: column;
    align-items: start;
  }
}

@media (max-width: 560px) {
  .hero-panel,
  .results-section {
    border-radius: 24px;
  }

  .hero-panel {
    padding: 22px;
  }

  .search-box {
    flex-direction: column;
  }

  .search-box button {
    height: 48px;
  }
}
</style>
