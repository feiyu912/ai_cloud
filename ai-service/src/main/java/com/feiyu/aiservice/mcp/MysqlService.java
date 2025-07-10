package com.feiyu.aiservice.mcp;

import java.util.List;
import java.util.Map;

/**
 * 接口
 *
 * @Author: ws
 * @Date: 2025/4/27 19:48
 */
public interface MysqlService {
    List<String> listAllDatabaseNames();
    List<String> listTablesByDatabase(String database);
    String getTableSchema(String database, String tableName);
    List<Map<String, Object>> queryTable(String database, String table, int limit);

}
