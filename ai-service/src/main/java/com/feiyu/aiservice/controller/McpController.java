package com.feiyu.aiservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mcp")
public class McpController {
    
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
    
    private Map<String, Object> createToolInfo(String name, String description) {
        Map<String, Object> toolInfo = new HashMap<>();
        toolInfo.put("name", name);
        toolInfo.put("description", description);
        return toolInfo;
    }
    

} 