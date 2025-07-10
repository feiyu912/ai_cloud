package com.feiyu.aiservice.mcp.impl;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class FileSystemTool {

    @Tool(name = "filesystem-mcp", description = "文件系统操作工具，参数：path(文件或目录路径)")
    public String listFiles(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "文件系统操作失败: 路径不能为空";
        }
        File file = new File(path);
        if (!file.exists()) {
            return "文件系统操作失败: 路径不存在";
        }
        if (file.isDirectory()) {
            String[] files = file.list();
            return "目录内容：" + String.join(", ", files != null ? files : new String[0]);
        } else {
            return "文件：" + file.getName();
        }
    }
} 