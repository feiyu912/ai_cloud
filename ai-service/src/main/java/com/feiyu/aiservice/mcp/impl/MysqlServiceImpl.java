package com.feiyu.aiservice.mcp.impl;

import com.feiyu.aiservice.mcp.MysqlService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TODO
 *
 * @Author: ws
 * @Date: 2025/4/27 19:48
 */
@Service
@RequiredArgsConstructor
public class MysqlServiceImpl implements MysqlService {

    private final JdbcTemplate jdbcTemplate;

    @Tool(description = "返回所有数据库名，逗号分隔")
    @Override
    public List<String> listAllDatabaseNames() {
        List<Map<String, Object>> dbNames = jdbcTemplate.queryForList("SHOW DATABASES");
        return dbNames.stream().map(e -> e.values().iterator().next().toString()).collect(Collectors.toList());
    }

    @Tool(description = "返回指定数据库下所有表名，逗号分隔")
    @Override
    public List<String> listTablesByDatabase(String database) {
        List<Map<String, Object>> tableNames = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = ?", database);
        return tableNames.stream().map(e -> e.values().iterator().next().toString()).collect(Collectors.toList());
    }

    @Tool(description = "返回指定表的结构信息，包括字段名、类型、是否可空、默认值")
    @Override
    public String getTableSchema(String database, String tableName) {
        String sql = "SELECT column_name, data_type, is_nullable, column_default FROM information_schema.columns WHERE table_schema = ? AND table_name = ?";
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(sql, database, tableName);
        StringBuilder sb = new StringBuilder("column_name | data_type | is_nullable | column_default\n");
        for (Map<String, Object> col : columns) {
            sb.append(col.get("column_name")).append(" | ")
              .append(col.get("data_type")).append(" | ")
              .append(col.get("is_nullable")).append(" | ")
              .append(col.get("column_default")).append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "查询指定数据库下的表内容，最多返回limit行")
    @Override
    public List<Map<String, Object>> queryTable(String database, String table, int limit) {
        String sql = String.format("SELECT * FROM `%s`.`%s` LIMIT %d", database, table, limit);
        return jdbcTemplate.queryForList(sql);
    }


}
