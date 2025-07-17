-- 为chat_session表添加dataset_id字段
ALTER TABLE chat_session ADD COLUMN dataset_id VARCHAR(255) COMMENT '绑定的知识库ID';
 
-- 更新现有会话，设置默认知识库ID（如果有的话）
-- UPDATE chat_session SET dataset_id = 'your_default_dataset_id' WHERE dataset_id IS NULL; 