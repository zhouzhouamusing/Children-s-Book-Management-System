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

// 读者管理 API
export function getReaders(params) {
  return request.get('/readers', { params })
}

export function getReader(id) {
  return request.get(`/readers/${id}`)
}

export function addReader(data) {
  return request.post('/readers', data)
}

export function updateReader(id, data) {
  return request.put(`/readers/${id}`, data)
}

export function deleteReader(id) {
  return request.delete(`/readers/${id}`)
}

export function getReaderBorrowRecords(id, params) {
  return request.get(`/readers/${id}/borrow-records`, { params })
}

export function updateReaderStatus(id, status) {
  return request.put(`/readers/${id}/status`, { status })
}

// 读者认证 API
export function readerLogin(data) {
  return request.post('/reader/login', data)
}

export function readerRegister(data) {
  return request.post('/reader/register', data)
}

// 读者个人中心 API
export function getMyProfile() {
  return request.get('/reader-center/profile')
}

export function updateMyProfile(data) {
  return request.put('/reader-center/profile', data)
}

export function getMyBorrowRecords(params) {
  return request.get('/reader-center/borrow-records', { params })
}

export function getMyReservations(params) {
  return request.get('/reader-center/reservations', { params })
}

export function createReservation(data) {
  return request.post('/reader-center/reservations', data)
}

export function cancelReservation(id) {
  return request.put(`/reader-center/reservations/${id}/cancel`)
}

export function browseBooks(params) {
  return request.get('/reader-center/books', { params })
}
