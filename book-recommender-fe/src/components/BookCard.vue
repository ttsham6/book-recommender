<script setup lang="ts">
defineProps<{
  id: string
  title: string
  author: string
  imageUrl?: string
}>()

defineEmits<{
  (e: 'click', id: string): void
}>()
</script>

<template>
  <div class="book-card" @click="$emit('click', id)">
    <div class="book-image-placeholder">
      <img v-if="imageUrl" :src="imageUrl" :alt="title" />
      <div v-else class="book-spine">
        <span>BOOK</span>
      </div>
    </div>

    <div class="book-info">
      <h3 class="book-title">{{ title }}</h3>
      <p class="book-author">{{ author }}</p>
    </div>

    <div class="book-arrow" aria-hidden="true">→</div>
  </div>
</template>

<style scoped>
.book-card {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(17, 32, 51, 0.06);
  border-radius: 22px;
  padding: 14px;
  box-shadow: 0 10px 22px rgba(17, 32, 51, 0.05);
  cursor: pointer;
}

.book-image-placeholder {
  width: 84px;
  height: 118px;
  border-radius: 14px;
  overflow: hidden;
  background: linear-gradient(180deg, #dbeafe, #eff6ff);
}

.book-image-placeholder img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.book-spine {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  background:
    linear-gradient(180deg, rgba(30, 64, 175, 0.15), rgba(30, 64, 175, 0.02)),
    linear-gradient(135deg, #d9e7ff, #eef4ff);
  color: #31517a;
  font-size: 0.74rem;
  font-weight: 800;
  letter-spacing: 0.18em;
}

.book-info {
  min-width: 0;
}

.book-title {
  margin: 0;
  color: #112033;
  font-size: 1.08rem;
  line-height: 1.35;
}

.book-author {
  margin: 6px 0 0;
  color: #5a6d80;
  font-size: 0.92rem;
}

.book-arrow {
  color: #7b8da1;
  font-size: 1.2rem;
  font-weight: 700;
}

@media (max-width: 560px) {
  .book-card {
    grid-template-columns: 72px minmax(0, 1fr);
  }

  .book-image-placeholder {
    width: 72px;
    height: 104px;
  }

  .book-arrow {
    display: none;
  }
}
</style>
