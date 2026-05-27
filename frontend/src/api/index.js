import request from '@/utils/request'

export function login(data) {
  return request.post('/admin/login', data)
}

export function register(data) {
  return request.post('/admin/register', data)
}

export function resetPassword(data) {
  return request.post('/admin/reset-password', data)
}

export function getBooks(params) {
  return request.get('/books', { params })
}

export function getBook(id) {
  return request.get(`/books/${id}`)
}

export function addBook(data) {
  return request.post('/books', data)
}

export function updateBook(id, data) {
  return request.put(`/books/${id}`, data)
}

export function deleteBook(id) {
  return request.delete(`/books/${id}`)
}

export function getStatistics() {
  return request.get('/books/statistics')
}

export function getCategories() {
  return request.get('/books/categories')
}

export function getCategoryList(params) {
  return request.get('/categories', { params })
}

export function getAllCategories() {
  return request.get('/categories/all')
}

export function getCategory(id) {
  return request.get(`/categories/${id}`)
}

export function addCategory(data) {
  return request.post('/categories', data)
}

export function updateCategory(id, data) {
  return request.put(`/categories/${id}`, data)
}

export function deleteCategory(id) {
  return request.delete(`/categories/${id}`)
}

export function getCategoryBookCount(id) {
  return request.get(`/categories/${id}/book-count`)
}
