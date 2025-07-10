package com.feiyu.aiservice.mcp.impl;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class RandomTool {

    @Tool(name = "random-mcp", description = "生成随机整数工具，参数：min(最小值), max(最大值)")
    public String generateRandom(int min, int max) {
        try {
            if (min > max) {
                return "随机数生成失败: min 不能大于 max";
            }
            return "随机数：" + (new Random().nextInt(max - min + 1) + min);
        } catch (Exception e) {
            return "随机数生成失败: " + e.getMessage();
        }
    }
} 