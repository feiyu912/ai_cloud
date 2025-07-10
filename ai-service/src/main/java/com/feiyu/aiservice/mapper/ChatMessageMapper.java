package com.feiyu.aiservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.feiyu.aiservice.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
} 
