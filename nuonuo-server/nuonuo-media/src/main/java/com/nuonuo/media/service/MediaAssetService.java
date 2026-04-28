package com.nuonuo.media.service;

/**
 * 多模态素材异步持久化服务（对象存储 + 数据库记录）
 */
public interface MediaAssetService {

    /**
     * 异步保存图片或语音原始素材，不阻塞主链路 SSE 响应
     */
    void persistAsync(String userId, String sessionId, String mediaType, String mediaTag, String base64Data, String ext);

    /**
     * 将 JSON 文本上传至 OSS（按用户与会话隔离目录），并返回可由客户端直接 fetch 渲染的预签名 URL
     */
    String uploadJsonAndGetPresignedUrl(String userId, String sessionId, String fileName, String jsonContent);
}
