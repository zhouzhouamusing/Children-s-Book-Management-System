import request from '@/utils/request'

// Token验证
export function validateToken() {
  return request.get('/auth/validate')
}

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

export function getReaderReadingProgress(id, params) {
  return request.get(`/readers/${id}/reading-progress`, { params })
}

export function getReaderReadingStatistics(id) {
  return request.get(`/readers/${id}/reading-statistics`)
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

export function getReaderCategories() {
  return request.get('/reader-center/categories')
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

export function getRecommendByProgress(params) {
  return request.get('/reader-center/recommend/by-progress', { params })
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

// 文件上传 API
export function uploadFile(data) {
  return request.post('/files/upload', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadFileBatch(data) {
  return request.post('/files/upload-batch', data, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getFileList(params) {
  return request.get('/files/list', { params })
}

export function getBookFiles(bookId) {
  return request.get(`/files/book/${bookId}`)
}

export function deleteFile(id) {
  return request.delete(`/files/${id}`)
}

// 图书评价 API (读者端)
export function createReview(data) {
  return request.post('/reader-center/reviews', data)
}

export function getMyReviews(params) {
  return request.get('/reader-center/reviews/my', { params })
}

export function updateReview(id, data) {
  return request.put(`/reader-center/reviews/${id}`, data)
}

export function deleteMyReview(id) {
  return request.delete(`/reader-center/reviews/${id}`)
}

export function getBookReviews(bookId, params) {
  return request.get(`/reader-center/reviews/book/${bookId}`, { params })
}

export function checkCanReview(bookId) {
  return request.get(`/reader-center/reviews/check/${bookId}`)
}

// 图书评价 API (管理端)
export function getAdminReviews(params) {
  return request.get('/reviews', { params })
}

export function approveReview(id) {
  return request.put(`/reviews/${id}/approve`)
}

export function rejectReview(id) {
  return request.put(`/reviews/${id}/reject`)
}

export function replyReview(id, data) {
  return request.put(`/reviews/${id}/reply`, data)
}

export function adminDeleteReview(id) {
  return request.delete(`/reviews/${id}`)
}

// 好评推荐
export function getTopRatedBooks(params) {
  return request.get('/reader-center/recommend/top-rated', { params })
}

// 数据面板 API
export function getBorrowTrends(params) {
  return request.get('/dashboard/borrow-trends', { params })
}

export function getOverdueAnalytics(params) {
  return request.get('/dashboard/overdue-analytics', { params })
}

export function getTopReaders(params) {
  return request.get('/dashboard/top-readers', { params })
}

export function getAuditLogs(params) {
  return request.get('/dashboard/audit-logs', { params })
}

export function getReaderGrowth(params) {
  return request.get('/dashboard/reader-growth', { params })
}

export function getBookUtilization(params) {
  return request.get('/dashboard/book-utilization', { params })
}

export function getDashboardReadingStats() {
  return request.get('/dashboard/reading-stats')
}

// 读者密码重置
export function readerSendResetCode(data) {
  return request.post('/reader/send-code', data)
}

export function readerResetPassword(data) {
  return request.post('/reader/reset-password', data)
}

// 预约管理 (管理端)
export function getAdminReservations(params) {
  return request.get('/borrows/reservations', { params })
}

export function fulfillReservation(id) {
  return request.put(`/borrows/reservations/${id}/fulfill`)
}

// 暂停申诉
export function appealSuspension(data) {
  return request.post('/reader-center/appeal-suspension', data)
}

export function getSuspensionAppeals(params) {
  return request.get('/readers/suspension-appeals', { params })
}

export function resolveAppeal(readerId, data) {
  return request.put(`/readers/${readerId}/resolve-appeal`, data)
}

// ===== RBAC 权限管理 API =====

// 角色管理
export function getRoles(params) {
  return request.get('/sys/roles', { params })
}

export function getAllRoles() {
  return request.get('/sys/roles/all')
}

export function createRole(data) {
  return request.post('/sys/roles', data)
}

export function updateRole(id, data) {
  return request.put(`/sys/roles/${id}`, data)
}

export function deleteRole(id) {
  return request.delete(`/sys/roles/${id}`)
}

export function getRolePermissions(roleId) {
  return request.get(`/sys/roles/${roleId}/permissions`)
}

export function getEffectivePermissions(roleId) {
  return request.get(`/sys/roles/${roleId}/effective-permissions`)
}

export function assignRolePermissions(roleId, data) {
  return request.put(`/sys/roles/${roleId}/permissions`, data)
}

// 权限管理
export function getPermissions(params) {
  return request.get('/sys/permissions', { params })
}

export function getPermissionsGrouped() {
  return request.get('/sys/permissions/grouped')
}

export function getPermissionModules() {
  return request.get('/sys/permissions/modules')
}

export function getPermissionsWithRoles() {
  return request.get('/sys/permissions/with-roles')
}

// 用户角色分配
export function getUserRoles(userType, userId) {
  return request.get(`/sys/user-roles/users/${userType}/${userId}/roles`)
}

export function assignUserRoles(userType, userId, data) {
  return request.put(`/sys/user-roles/users/${userType}/${userId}/roles`, data)
}

export function getAllUsersWithRoles(params) {
  return request.get('/sys/user-roles/users', { params })
}

export function getRoleUsers(roleId) {
  return request.get(`/sys/user-roles/roles/${roleId}/users`)
}

// 用户菜单
export function getMyMenus() {
  return request.get('/sys/menus/my')
}

// 权限CRUD
export function createPermission(data) {
  return request.post('/sys/permissions', data)
}

export function updatePermission(id, data) {
  return request.put(`/sys/permissions/${id}`, data)
}

export function deletePermission(id) {
  return request.delete(`/sys/permissions/${id}`)
}

// 读者申诉
export function submitAppeal(data) {
  return request.post('/reader-center/appeals', data)
}

export function getMyAppeals(params) {
  return request.get('/reader-center/appeals', { params })
}

export function getAppealDetail(id) {
  return request.get(`/reader-center/appeals/${id}`)
}

// 管理员申诉管理
export function getAdminAppeals(params) {
  return request.get('/admin/appeals', { params })
}

export function reviewAppeal(id, data) {
  return request.put(`/admin/appeals/${id}/review`, data)
}

