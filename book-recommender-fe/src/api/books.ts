import type { Book, BookApiItem, BookResponse } from '../types/book'

const API_HOST = import.meta.env.VITE_API_HOST?.trim()
const API_BASE_PATH = import.meta.env.VITE_API_BASE_PATH?.trim() || '/api'
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim()

const resolveApiBaseUrl = () => {
  if (API_BASE_URL) {
    return API_BASE_URL
  }

  if (API_HOST) {
    return new URL(API_BASE_PATH, API_HOST).toString().replace(/\/$/, '')
  }

  return API_BASE_PATH
}

const resolvedApiBaseUrl = resolveApiBaseUrl()

const toBook = (item: BookApiItem): Book => ({
  id: item.id,
  title: item.title,
  author: item.author,
  imageUrl: item.largeImageUrl || item.mediumImageUrl,
  publisherName: item.publisherName,
  itemCaption: item.itemCaption,
  itemUrl: item.itemUrl,
  reviewAverage: item.reviewAverage
})

const fetchBooks = async (path: string, query: string): Promise<Book[]> => {
  const url = new URL(`${resolvedApiBaseUrl}${path}`, window.location.origin)
  url.searchParams.set('query', query)

  const response = await fetch(url.toString())

  if (!response.ok) {
    throw new Error(`API request failed: ${response.status}`)
  }

  const data = (await response.json()) as BookResponse
  return data.items.map(toBook)
}

export const searchBooks = async (query: string): Promise<Book[]> => {
  return fetchBooks('/item', query)
}

export const fetchRecommendations = async (query: string): Promise<Book[]> => {
  return fetchBooks('/recommendations', query)
}
