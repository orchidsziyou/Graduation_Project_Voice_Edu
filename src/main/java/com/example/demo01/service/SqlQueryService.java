package com.example.demo01.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
public class SqlQueryService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    /**
     * 执行SQL查询
     */
    public Map<String, Object> executeQuery(String sql) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 确定查询类型
            String upperSql = sql.trim().toUpperCase();
            String queryType = "UNKNOWN";
            
            if (upperSql.startsWith("SELECT") || upperSql.startsWith("SHOW") || upperSql.startsWith("DESCRIBE")) {
                queryType = "SELECT";
            } else if (upperSql.startsWith("INSERT") || upperSql.startsWith("UPDATE") || 
                      upperSql.startsWith("DELETE") || upperSql.startsWith("CREATE") ||
                      upperSql.startsWith("DROP") || upperSql.startsWith("ALTER")) {
                queryType = "DML";
            }

            if ("SELECT".equals(queryType)) {
                // 执行查询语句
                SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql);
                List<Map<String, Object>> data = convertRowSetToList(rowSet);
                
                result.put("success", true);
                result.put("queryType", queryType);
                result.put("data", data);
                result.put("rowCount", data.size());
                result.put("columns", getColumnsFromRowSet(rowSet));
            } else {
                // 执行更新语句
                int rowsAffected = jdbcTemplate.update(sql);
                result.put("success", true);
                result.put("queryType", queryType);
                result.put("rowsAffected", rowsAffected);
                result.put("message", "操作成功，影响了 " + rowsAffected + " 行");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("queryType", "ERROR");
        }
        
        return result;
    }

    /**
     * 获取数据库中所有表名
     */
    public List<String> getAllTableNames() {
        List<String> tableNames = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                tableNames.add(tableName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // 按字母顺序排序
        Collections.sort(tableNames);
        return tableNames;
    }

    /**
     * 获取表结构信息
     */
    public List<Map<String, Object>> getTableStructure(String tableName) {
        List<Map<String, Object>> structure = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, tableName, "%");
            
            while (columns.next()) {
                Map<String, Object> columnInfo = new HashMap<>();
                columnInfo.put("COLUMN_NAME", columns.getString("COLUMN_NAME"));
                columnInfo.put("TYPE_NAME", columns.getString("TYPE_NAME"));
                columnInfo.put("COLUMN_SIZE", columns.getInt("COLUMN_SIZE"));
                columnInfo.put("IS_NULLABLE", columns.getString("IS_NULLABLE"));
                columnInfo.put("COLUMN_DEFAULT", columns.getString("COLUMN_DEF"));
                columnInfo.put("REMARKS", columns.getString("REMARKS"));
                structure.add(columnInfo);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return structure;
    }

    /**
     * 将SqlRowSet转换为List<Map<String, Object>>
     */
    private List<Map<String, Object>> convertRowSetToList(SqlRowSet rowSet) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        while (rowSet.next()) {
            Map<String, Object> row = new HashMap<>();
            SqlRowSetMetaData metaData = rowSet.getMetaData();
            
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rowSet.getObject(i);
                row.put(columnName, value);
            }
            
            result.add(row);
        }
        
        return result;
    }

    /**
     * 从RowSet获取列名列表
     */
    private List<String> getColumnsFromRowSet(SqlRowSet rowSet) {
        List<String> columns = new ArrayList<>();
        SqlRowSetMetaData metaData = rowSet.getMetaData();
        
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            columns.add(metaData.getColumnName(i));
        }
        
        return columns;
    }

    /**
     * 执行批量SQL语句
     */
    public Map<String, Object> executeBatch(List<String> sqlStatements) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Map<String, Object>> batchResults = new ArrayList<>();
            int totalRowsAffected = 0;
            
            for (String sql : sqlStatements) {
                if (sql != null && !sql.trim().isEmpty()) {
                    Map<String, Object> statementResult = executeQuery(sql);
                    batchResults.add(statementResult);
                    
                    if (statementResult.containsKey("rowsAffected")) {
                        totalRowsAffected += (Integer) statementResult.get("rowsAffected");
                    }
                }
            }
            
            result.put("success", true);
            result.put("batchResults", batchResults);
            result.put("totalRowsAffected", totalRowsAffected);
            result.put("message", "批量执行完成，总共影响了 " + totalRowsAffected + " 行");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取数据库统计信息
     */
    public Map<String, Object> getDatabaseStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 获取表数量
            List<String> tables = getAllTableNames();
            stats.put("tableCount", tables.size());
            stats.put("tableNames", tables);
            
            // 获取总记录数估计
            int totalRecords = 0;
            for (String tableName : tables) {
                try {
                    Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + tableName, Integer.class);
                    if (count != null) {
                        totalRecords += count;
                    }
                } catch (Exception e) {
                    // 忽略无法查询的表
                }
            }
            stats.put("estimatedTotalRecords", totalRecords);
            
        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
}