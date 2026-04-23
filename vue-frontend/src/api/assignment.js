import axios from 'axios'

// 创建题目推送 API 实例
const assignmentApi = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 自动添加 token
assignmentApi.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

/**
 * 创建题目推送
 */
export function createAssignment(data) {
  return assignmentApi.post('/assignment/create', data)
}

/**
 * 查询教师的推送记录（分页）
 */
export function getTeacherAssignments(teacherId, page = 0, size = 10) {
  return assignmentApi.get(`/assignment/teacher/${teacherId}`, {
    params: { page, size }
  })
}

/**
 * 查询班级的推送记录（分页）
 */
export function getClassAssignments(classId, page = 0, size = 10) {
  return assignmentApi.get(`/assignment/class/${classId}`, {
    params: { page, size }
  })
}

/**
 * 获取推送详情
 */
export function getAssignmentDetail(assignmentId) {
  return assignmentApi.get(`/assignment/${assignmentId}`)
}

/**
 * 更新推送状态
 */
export function updateAssignmentStatus(assignmentId, status) {
  return assignmentApi.put(`/assignment/${assignmentId}/status`, { status })
}

/**
 * 获取推送统计
 */
export function getAssignmentStatistics(assignmentId) {
  return assignmentApi.get(`/assignment/${assignmentId}/statistics`)
}

/**
 * 获取学生在某次推送中的答题记录
 */
export function getStudentAnswerRecord(assignmentId, studentId) {
  return assignmentApi.get(`/assignment/${assignmentId}/student/${studentId}`)
}

/**
 * 学生提交答案
 */
export function submitAnswer(recordId, answer) {
  return assignmentApi.post('/assignment/submit-answer', { recordId, answer })
}

/**
 * 获取某次推送的所有答题记录
 */
export function getAssignmentRecords(assignmentId) {
  return assignmentApi.get(`/assignment/${assignmentId}/records`)
}

/**
 * 获取学生的待完成任务
 */
export function getPendingAssignments(studentId) {
  return assignmentApi.get(`/assignment/pending/${studentId}`)
}

/**
 * 获取学生的所有推送题目（包括已完成和未完成）
 */
export function getAllAssignments(studentId) {
  return assignmentApi.get(`/assignment/all/${studentId}`)
}

/**
 * 获取班级的所有推送记录（带统计信息）
 */
export function getClassAssignmentsWithStats(classId) {
  return assignmentApi.get(`/assignment/class/${classId}/with-stats`)
}

/**
 * 获取某次推送的详细作答记录（包含学生信息）
 */
export function getAssignmentAnswerDetails(assignmentId) {
  return assignmentApi.get(`/assignment/${assignmentId}/answer-details`)
}

export default assignmentApi
