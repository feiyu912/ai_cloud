package com.feiyu.aiservice.mcp.impl;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TimeTool {

    @Tool(name = "time-mcp", description = "获取当前时间工具，无需参数")
    public String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        return "当前时间：" + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
} 