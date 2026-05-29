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

// 借阅管理 API
export function getBorrows(params) {
  return request.get('/borrows', { params })
}

export function createBorrow(data) {
  return request.post('/borrows', data)
}

export function returnBorrow(id) {
  return request.put(`/borrows/${id}/return`)
}

export function renewBorrow(id, days = 14) {
  return request.put(`/borrows/${id}/renew`, null, { params: { days } })
}

export function getBorrowStatistics() {
  return request.get('/borrows/statistics')
}

// 邮箱验证码
export function sendResetCode(data) {
  return request.post('/admin/send-code', data)
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

export function getMyStatistics() {
  return request.get('/reader-center/statistics')
}

export function getMyPoints() {
  return request.get('/reader-center/points')
}

// 管理员申请 API
export function applyForAdmin(data) {
  return request.post('/admin-application/apply', data)
}

export function getMyApplicationStatus() {
  return request.get('/admin-application/my-status')
}

export function getAdminApplications(params) {
  return request.get('/admin-application/list', { params })
}

export function approveApplication(id) {
  return request.put(`/admin-application/${id}/approve`)
}

export function rejectApplication(id, data) {
  return request.put(`/admin-application/${id}/reject`, data)
}

// 图书推荐 API
export function getRecommendByHistory(params) {
  return request.get('/reader-center/recommend/by-history', { params })
}

export function getRecommendByAge(params) {
  return request.get('/reader-center/recommend/by-age', { params })
}

export function getRecommendTop10() {
  return request.get('/reader-center/recommend/top10')
}

export function getAllRecommendations() {
  return request.get('/reader-center/recommend/all')
}

// 阅读进度 API
export function getReadingProgressList(params) {
  return request.get('/reader-center/reading-progress', { params })
}

export function createOrUpdateProgress(data) {
  return request.post('/reader-center/reading-progress', data)
}

export function updateProgressStatus(id, data) {
  return request.put(`/reader-center/reading-progress/${id}/status`, data)
}

export function deleteProgress(id) {
  return request.delete(`/reader-center/reading-progress/${id}`)
}

export function getReadingStatistics() {
  return request.get('/reader-center/reading-progress/statistics')
}

// 阅读笔记 API
export function getReadingNotes(params) {
  return request.get('/reader-center/reading-progress/notes', { params })
}

export function addReadingNote(data) {
  return request.post('/reader-center/reading-progress/notes', data)
}

export function updateReadingNote(id, data) {
  return request.put(`/reader-center/reading-progress/notes/${id}`, data)
}

export function deleteReadingNote(id) {
  return request.delete(`/reader-center/reading-progress/notes/${id}`)
}

