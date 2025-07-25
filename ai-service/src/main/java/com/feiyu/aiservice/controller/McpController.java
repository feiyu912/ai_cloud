package com.feiyu.aiservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mcp")
public class McpController {
    @Autowired
    private com.feiyu.aiservice.mcp.impl.TimeTool timeTool;
    @Autowired
    private com.feiyu.aiservice.mcp.impl.RandomTool randomTool;
    @Autowired
    private com.feiyu.aiservice.mcp.impl.FileSystemTool fileSystemTool;
    @Autowired
    private com.feiyu.aiservice.mcp.impl.UnitConvertTool unitConvertTool;
    @Autowired
    private com.feiyu.aiservice.mcp.impl.MysqlServiceImpl mysqlService;
    
    @GetMapping("/tools")
    public List<Map<String, Object>> getTools() {
        List<Map<String, Object>> tools = new ArrayList<>();
        
        // 静态返回本地MCP工具列表
        tools.add(createToolInfo("time-mcp", "获取当前时间"));
        tools.add(createToolInfo("random-mcp", "生成随机整数"));
        tools.add(createToolInfo("filesystem-mcp", "文件系统操作"));
        tools.add(createToolInfo("unitconvert-mcp", "单位换算"));
        tools.add(createToolInfo("mysql-mcp", "MySQL数据库操作"));
        
        return tools;
    }
    
    @PostMapping("/invoke")
    public Object invokeTool(@RequestBody Map<String, Object> body) {
        String tool = (String) body.get("tool");
        Map<String, Object> params = (Map<String, Object>) body.get("params");
        switch (tool) {
            case "time-mcp":
                return timeTool.getCurrentTime();
            case "random-mcp":
                int min = params.get("min") != null ? Integer.parseInt(params.get("min").toString()) : 0;
                int max = params.get("max") != null ? Integer.parseInt(params.get("max").toString()) : 100;
                return randomTool.generateRandom(min, max);
            case "filesystem-mcp":
                return fileSystemTool.listFiles((String) params.get("path"));
            case "unitconvert-mcp":
                double value = params.get("value") != null ? Double.parseDouble(params.get("value").toString()) : 0.0;
                String type = params.get("type") != null ? params.get("type").toString() : "";
                return unitConvertTool.convert(value, type);
            case "mysql-mcp":
                // 这里只做演示，实际可根据params路由到MysqlServiceImpl的不同方法
                return "请指定MySQL操作类型";
            default:
                return "不支持的MCP工具: " + tool;
        }
    }
    
    private Map<String, Object> createToolInfo(String name, String description) {
        Map<String, Object> toolInfo = new HashMap<>();
        toolInfo.put("name", name);
        toolInfo.put("description", description);
        return toolInfo;
    }
    

} 