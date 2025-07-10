package com.feiyu.aiservice.mcp.impl;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class UnitConvertTool {

    @Tool(name = "unitconvert-mcp", description = "单位换算工具，参数：value(数值), type(换算类型，m2ft为米转英尺，ft2m为英尺转米)")
    public String convert(double value, String type) {
        switch (type) {
            case "m2ft":
                return value + "米 = " + (value * 3.28084) + "英尺";
            case "ft2m":
                return value + "英尺 = " + (value / 3.28084) + "米";
            default:
                return "单位换算失败: 不支持的类型";
        }
    }
} 