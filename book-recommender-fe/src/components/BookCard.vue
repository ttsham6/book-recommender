<script setup lang="ts">
defineProps<{
  id: string
  title: string
  author: string
  imageUrl?: string
  description?: string
}>()

defineEmits<{
  (e: 'click', id: string): void
}>()
</script>

<template>
  <div class="book-card" @click="$emit('click', id)">
    <div class="book-image-placeholder">
      <img v-if="imageUrl" :src="imageUrl" :alt="title" />
      <span v-else>No Image</span>
    </div>
    <div class="book-info">
      <h3 class="book-title">{{ title }}</h3>
      <p class="book-author">{{ author }}</p>
      <p v-if="description" class="book-description">{{ description }}</p>
    </div>
  </div>
</template>

<style scoped>
.book-card {
  display: flex;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 12px;
  margin-bottom: 16px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.book-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.1);
}

.book-image-placeholder {
  width: 80px;
  height: 120px;
  background: linear-gradient(135deg, #e0eafc, #cfdef3);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  color: #666;
  font-size: 0.8rem;
}

.book-image-placeholder img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.book-info {
  margin-left: 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.book-title {
  margin: 0 0 4px 0;
  font-size: 1.1rem;
  color: #333;
  font-weight: 600;
  line-height: 1.3;
}

.book-author {
  margin: 0;
  font-size: 0.9rem;
  color: #666;
}

.book-description {
  margin: 8px 0 0 0;
  font-size: 0.85rem;
  color: #777;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
