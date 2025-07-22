package com.feiyu.aiservice.util;

import java.util.*;

public class PromptBuilder {
    // 近似每个汉字/英文单词1 token，实际可用 tiktoken-java 精确统计
    public static String buildContext(List<String> allContents, int maxToken, int maxContentLen) {
        int usedToken = 0;
        StringBuilder sb = new StringBuilder();
        for (String content : allContents) {
            if (content == null) continue;
            String truncated = content.length() > maxContentLen ? content.substring(0, maxContentLen) + "..." : content;
            int contentToken = truncated.length(); // 近似统计
            if (usedToken + contentToken > maxToken * 0.8) break;
            sb.append(truncated).append("\n");
            usedToken += contentToken;
        }
        return sb.toString().trim();
    }
} 