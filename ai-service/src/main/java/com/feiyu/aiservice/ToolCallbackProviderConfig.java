package com.feiyu.aiservice;

import com.feiyu.aiservice.mcp.impl.*;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

/**
 * MCP工具配置类
 * 使用Spring AI 1.0.0版本支持的方式注册所有MCP工具
 *
 * @Author: ws
 * @Date: 2025/4/27 15:41
 */
@Configuration
public class ToolCallbackProviderConfig {

    /**
     * 注册所有MCP工具为ToolCallback数组
     * 使用MethodToolCallbackProvider方式，兼容Spring AI 1.0.0
     */
    @Bean
    public MethodToolCallbackProvider mcpToolCallbacks(
            TimeTool timeTool,
            RandomTool randomTool,
            FileSystemTool fileSystemTool,
            UnitConvertTool unitConvertTool,
            MysqlServiceImpl mysqlService
    ) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(
                        timeTool,
                        randomTool,
                        fileSystemTool,
                        unitConvertTool,
                        mysqlService
                )
                .build();
    }
}