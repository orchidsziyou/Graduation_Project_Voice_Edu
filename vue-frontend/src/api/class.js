import axios from 'axios'

// 创建班级管理 API 实例
const classApi = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 自动添加 token
classApi.interceptors.request.use(
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
 * 创建新班级
 */
export function createClass(classname, classcode, classnum, creatorUserId = null) {
  const data = { classname, classcode, classnum }
  if (creatorUserId != null) {
    data.creatorUserId = creatorUserId
  }
  return classApi.post('/class/create', data)
}

/**
 * 获取所有班级列表
 */
export function getAllClasses() {
  return classApi.get('/class/list')
}

/**
 * 根据 ID 获取班级详情
 */
export function getClassById(classid) {
  return classApi.get(`/class/${classid}`)
}

/**
 * 更新班级信息
 */
export function updateClass(classid, classname, classnum) {
  return classApi.put(`/class/${classid}`, { classname, classnum })
}

/**
 * 删除班级
 */
export function deleteClass(classid) {
  return classApi.delete(`/class/${classid}`)
}

/**
 * 搜索班级
 */
export function searchClasses(keyword) {
  return classApi.get('/class/search', { params: { keyword } })
}

/**
 * 获取用户加入的所有班级
 */
export function getMyClasses(userid) {
  return classApi.get(`/class-members/my-classes/${userid}`)
}

/**
 * 获取班级所有成员（包含用户信息）
 */
export function getClassMembersWithUsers(classid) {
  return classApi.get(`/class-members/list-with-users/${classid}`)
}

/**
 * 移除班级成员
 */
export function removeClassMember(classid, userid) {
  return classApi.delete('/class-members/remove', {
    data: { classid, userid }
  })
}

/**
 * 加入班级
 */
export function joinClass(userid, classid, userrole = 1) {
  return classApi.post('/class-members/add', {
    userid,
    classid,
    userrole
  })
}

export default classApi
