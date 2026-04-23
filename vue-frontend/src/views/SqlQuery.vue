<template>
  <div class="sql-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>💾 SQL 查询</span>
          <el-button type="success" size="small" @click="loadTableNames">
            <el-icon><refresh /></el-icon>
            刷新表
          </el-button>
        </div>
      </template>
      
      <el-alert
        title="提示"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      >
        此功能用于执行 SQL 查询，请谨慎操作。禁止执行 DROP、TRUNCATE 等危险操作！
      </el-alert>
      
      <!-- 数据库统计信息 -->
      <el-row :gutter="20" style="margin-bottom: 20px">
        <el-col :span="8">
          <el-statistic title="表数量" :value="dbStats.tableCount" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="预估总记录数" :value="dbStats.totalRecords" />
        </el-col>
        <el-col :span="8">
          <el-statistic title="数据库类型" value="H2" />
        </el-col>
      </el-row>
      
      <!-- 表名列表 -->
      <el-collapse v-if="tableNames.length > 0" style="margin-bottom: 20px">
        <el-collapse-item title="📊 数据库表列表" name="1">
          <el-tag
            v-for="tableName in tableNames"
            :key="tableName"
            style="margin: 5px; cursor: pointer"
            @click="viewTableStructure(tableName)"
          >
            {{ tableName }}
          </el-tag>
        </el-collapse-item>
      </el-collapse>
      
      <!-- SQL 输入框 -->
      <el-input
        v-model="sqlQuery"
        type="textarea"
        :rows="8"
        placeholder="请输入 SQL 查询语句，例如：SELECT * FROM users"
        style="margin-bottom: 20px"
      />
      
      <div style="margin-bottom: 20px">
        <el-button
          type="primary"
          @click="handleExecuteQuery"
          :loading="loading"
        >
          执行查询
        </el-button>
        
        <el-button @click="handleClear">
          清空
        </el-button>
        
        <el-button @click="handleLoadStats">
          刷新统计
        </el-button>

        <el-button @click="returndashboard">
          回到主页
        </el-button>
      </div>
      
      <!-- 查询结果 -->
      <div v-if="queryResult" style="margin-top: 20px">
        <el-divider>{{ resultTitle }}</el-divider>
        
        <!-- 表格结果（SELECT 查询） -->
        <el-table
          v-if="queryResult.data && queryResult.data.length > 0"
          :data="queryResult.data"
          stripe
          border
          style="width: 100%; margin-bottom: 20px"
          max-height="400"
        >
          <el-table-column
            v-for="column in queryResult.columns"
            :key="column"
            :prop="column"
            :label="column"
          />
        </el-table>
        
        <!-- 文本结果（其他类型查询） -->
        <pre v-else-if="typeof queryResult === 'string'" class="result-box">{{ queryResult }}</pre>
        
        <!-- 影响行数 -->
        <el-alert
          v-if="queryResult.rowsAffected !== undefined"
          :title="`✅ 操作成功，影响了 ${queryResult.rowsAffected} 行数据`"
          type="success"
          :closable="false"
          show-icon
          style="margin-top: 10px"
        />
        
        <!-- 错误信息 -->
        <el-alert
          v-if="queryResult.error"
          :title="`❌ 错误：${queryResult.error}`"
          type="error"
          :closable="false"
          show-icon
          style="margin-top: 10px"
        />
      </div>
      
      <!-- 表结构对话框 -->
      <el-dialog
        v-model="structureDialogVisible"
        :title="`表结构：${currentTableName}`"
        width="80%"
      >
        <el-table :data="tableStructure" stripe border style="width: 100%">
          <el-table-column prop="COLUMN_NAME" label="列名" />
          <el-table-column prop="TYPE_NAME" label="数据类型" />
          <el-table-column prop="COLUMN_SIZE" label="长度" width="80" />
          <el-table-column prop="IS_NULLABLE" label="允许 NULL" width="100">
            <template #default="{ row }">
              <el-tag :type="row.IS_NULLABLE === 'YES' ? 'warning' : 'success'">
                {{ row.IS_NULLABLE }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="COLUMN_DEFAULT" label="默认值" />
          <el-table-column prop="REMARKS" label="备注" />
        </el-table>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// SQL 查询相关变量
const sqlQuery = ref('')
const loading = ref(false)
const queryResult = ref(null)
const resultTitle = ref('查询结果')

// 数据库统计信息
const dbStats = reactive({
  tableCount: 0,
  totalRecords: 0
})

const returndashboard = () => {
  router.push('/')
}

// 表名列表
const tableNames = ref([])

// 表结构对话框
const structureDialogVisible = ref(false)
const currentTableName = ref('')
const tableStructure = ref([])

// 创建 axios 实例
const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 添加 token
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    console.log('=== SQL 请求拦截器 ===')
    console.log('当前 URL:', config.url)
    console.log('从 localStorage 获取的 Token:', token ? token.substring(0, 20) + '...' : 'null')
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      console.log('已添加 Authorization Header:', config.headers.Authorization)
    } else {
      console.warn('⚠️ 未找到 Token，用户可能未登录！')
    }
    console.log('====================\n')
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 加载数据库统计信息
const handleLoadStats = async () => {
  try {
    const response = await api.get('/sql/stats')
    if (response.data.success) {
      dbStats.tableCount = response.data.tableCount || 0
      dbStats.totalRecords = response.data.estimatedTotalRecords || 0
    }
  } catch (error) {
    console.error('加载统计信息失败:', error)
  }
}

// 加载表名列表
const loadTableNames = async () => {
  try {
    const response = await api.get('/sql/tables')
    if (response.data.success) {
      tableNames.value = response.data.tableNames || []
      ElMessage.success(`已加载 ${tableNames.value.length} 个表`)
    }
  } catch (error) {
    console.error('加载表名失败:', error)
    ElMessage.error('加载表名失败')
  }
}

// 查看表结构
const viewTableStructure = async (tableName) => {
  try {
    currentTableName.value = tableName
    const response = await api.get(`/sql/table/${tableName}/structure`)
    
    if (response.data.success) {
      tableStructure.value = response.data.structure || []
      structureDialogVisible.value = true
    } else {
      ElMessage.error(response.data.error || '获取表结构失败')
    }
  } catch (error) {
    console.error('获取表结构失败:', error)
    ElMessage.error('获取表结构失败')
  }
}

// 执行 SQL 查询
const handleExecuteQuery = async () => {
  if (!sqlQuery.value.trim()) {
    ElMessage.warning('请输入 SQL 查询语句')
    return
  }
  
  // 检查是否是危险操作
  const upperSql = sqlQuery.value.toUpperCase().trim()
  if (upperSql.startsWith('DROP') || upperSql.startsWith('TRUNCATE')) {
    ElMessage.warning('禁止执行 DROP、TRUNCATE 等危险操作')
    return
  }
  
  loading.value = true
  queryResult.value = null
  
  try {
    const response = await api.post('/sql/execute', {
      sql: sqlQuery.value
    })
    
    console.log('SQL 响应:', response.data)
    
    if (response.data.success) {
      // 根据查询类型设置标题
      if (upperSql.startsWith('SELECT') || upperSql.startsWith('SHOW')) {
        resultTitle.value = `✅ 查询成功（${response.data.rowCount || 0} 条记录）`
      } else {
        resultTitle.value = '✅ 执行成功'
      }
      
      queryResult.value = response.data
      
      // 显示成功消息
      if (response.data.rowsAffected !== undefined) {
        ElMessage.success(`操作成功，影响了 ${response.data.rowsAffected} 行数据`)
      } else if (response.data.rowCount !== undefined) {
        ElMessage.success(`查询成功，返回 ${response.data.rowCount} 条记录`)
      }
    } else {
      queryResult.value = {
        error: response.data.error || '查询失败'
      }
      ElMessage.error(response.data.error || '查询失败')
    }
  } catch (error) {
    console.error('查询失败:', error)
    queryResult.value = {
      error: error.response?.data?.error || error.message || '查询失败，请检查网络连接'
    }
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

// 清空
const handleClear = () => {
  sqlQuery.value = ''
  queryResult.value = null
  resultTitle.value = '查询结果'
}

// 初始化
onMounted(() => {
  handleLoadStats()
  loadTableNames()
})
</script>

<style scoped>
.sql-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-box {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  font-family: 'Courier New', Courier, monospace;
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 400px;
  overflow-y: auto;
}
</style>
