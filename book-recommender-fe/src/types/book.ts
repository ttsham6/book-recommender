export type Book = {
  id: string
  title: string
  author: string
  imageUrl?: string
  publisherName?: string
  itemCaption?: string
  itemUrl?: string
  reviewAverage?: number
}

export type BookApiItem = {
  id: string
  title: string
  author: string
  publisherName?: string
  itemCaption?: string
  itemUrl?: string
  mediumImageUrl?: string
  largeImageUrl?: string
  reviewAverage?: number
}

export type BookResponse = {
  count: number
  items: BookApiItem[]
}
