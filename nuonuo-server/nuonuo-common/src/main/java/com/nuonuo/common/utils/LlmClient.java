package com.nuonuo.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nuonuo.common.config.LlmProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * 大模型调用通用工具类：
 * 1. 动态加载 XML 提示词模板
 * 2. 指数退避 + 随机抖动重试
 * 3. 主模型失败时自动切换备用模型兜底
 */
@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Map<String, String> promptCache = new ConcurrentHashMap<>();

    public LlmClient(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @PostConstruct
    public void loadPrompts() {
        promptCache.clear();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(properties.getPromptLocation());
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    Document doc = factory.newDocumentBuilder().parse(is);
                    NodeList nodes = doc.getElementsByTagName("prompt");
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Element el = (Element) nodes.item(i);
                        String id = el.getAttribute("id");
                        String content = el.getTextContent();
                        if (StringUtils.hasText(id) && StringUtils.hasText(content)) {
                            promptCache.put(id.trim(), content.trim());
                        }
                    }
                }
            }
            log.info("Loaded {} prompt templates from {}", promptCache.size(), properties.getPromptLocation());
        } catch (Exception ex) {
            log.error("Failed to load XML prompt templates from {}", properties.getPromptLocation(), ex);
        }
    }

    public String getPrompt(String promptId) {
        String prompt = promptCache.get(promptId);
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("Prompt template not found: " + promptId);
        }
        return prompt;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(properties.getApiKey());
    }

    /**
     * 流式调用大模型：先尝试主模型（带指数退避+随机抖动重试），失败后自动切换备用模型兜底
     */
    public void streamChat(String promptId, String userPrompt, Consumer<String> onDelta) throws Exception {
        streamChat(promptId, userPrompt, null, onDelta);
    }

    /**
     * 流式调用大模型（支持多模态图片 Base64 Data URL）：先尝试主模型（带指数退避+随机抖动重试），失败后自动切换备用模型兜底
     */
    public void streamChat(String promptId, String userPrompt, String imageBase64, Consumer<String> onDelta) throws Exception {
        streamChat(promptId, userPrompt, imageBase64, null, null, onDelta);
    }

    /**
     * 流式调用大模型（统一支持文字 + 图片 Base64 + 语音 Base64 多模态）：
     * 先尝试主模型（带指数退避+随机抖动重试），失败后自动切换备用模型兜底
     */
    public void streamChat(
            String promptId,
            String userPrompt,
            String imageBase64,
            String audioBase64,
            String audioFormat,
            Consumer<String> onDelta) throws Exception {
        String systemPrompt = getPrompt(promptId);
        try {
            executeWithRetry(properties.getModel(), systemPrompt, userPrompt, imageBase64, audioBase64, audioFormat, onDelta);
        } catch (Exception primaryEx) {
            String fallback = properties.getFallbackModel();
            if (StringUtils.hasText(fallback) && !fallback.equals(properties.getModel())) {
                log.warn("Primary model [{}] failed after retries, switching to fallback model [{}]",
                        properties.getModel(), fallback, primaryEx);
                executeWithRetry(fallback, systemPrompt, userPrompt, imageBase64, audioBase64, audioFormat, onDelta);
            } else {
                throw primaryEx;
            }
        }
    }

    private void executeWithRetry(
            String model,
            String systemPrompt,
            String userPrompt,
            String imageBase64,
            String audioBase64,
            String audioFormat,
            Consumer<String> onDelta) throws Exception {
        int maxAttempts = Math.max(1, properties.getMaxRetries());
        Exception lastEx = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                doStreamRequest(model, systemPrompt, userPrompt, imageBase64, audioBase64, audioFormat, onDelta);
                return;
            } catch (Exception ex) {
                lastEx = ex;
                if (attempt == maxAttempts) {
                    break;
                }
                long backoff = computeBackoffWithJitter(attempt);
                log.warn("LLM call failed (model={}, attempt={}/{}), retrying in {} ms: {}",
                        model, attempt, maxAttempts, backoff, ex.getMessage());
                Thread.sleep(backoff);
            }
        }
        throw lastEx;
    }

    private long computeBackoffWithJitter(int attempt) {
        long exponential = properties.getInitialBackoffMs() * (1L << Math.min(attempt - 1, 10));
        long capped = Math.min(exponential, properties.getMaxBackoffMs());
        return ThreadLocalRandom.current().nextLong(capped / 2, capped + 1);
    }

    private void doStreamRequest(
            String model,
            String systemPrompt,
            String userPrompt,
            String imageBase64,
            String audioBase64,
            String audioFormat,
            Consumer<String> onDelta) throws Exception {
        boolean hasMedia = StringUtils.hasText(imageBase64) || StringUtils.hasText(audioBase64);
        Object userContent;
        if (hasMedia) {
            java.util.ArrayList<Map<String, Object>> parts = new java.util.ArrayList<>();
            parts.add(Map.of("type", "text", "text", userPrompt));
            if (StringUtils.hasText(imageBase64)) {
                parts.add(Map.of("type", "image_url", "image_url", Map.of("url", imageBase64)));
            }
            if (StringUtils.hasText(audioBase64)) {
                String rawAudio = audioBase64.contains(",") ? audioBase64.substring(audioBase64.indexOf(',') + 1) : audioBase64;
                String fmt = StringUtils.hasText(audioFormat) ? audioFormat : "wav";
                parts.add(Map.of("type", "input_audio", "input_audio", Map.of("data", rawAudio, "format", fmt)));
            }
            userContent = parts;
        } else {
            userContent = userPrompt;
        }

        Map<String, Object> payload = Map.of(
                "model", model,
                "stream", true,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContent)
                )
        );

        String endpoint = properties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(45))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("LLM API returned HTTP " + response.statusCode());
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data) || data.isEmpty()) {
                    continue;
                }
                JsonNode root = objectMapper.readTree(data);
                JsonNode deltaNode = root.path("choices").path(0).path("delta").path("content");
                if (deltaNode.isTextual()) {
                    onDelta.accept(deltaNode.asText());
                }
            }
        }
    }

    /**
     * 调用 302.AI / OpenAI 兼容 Whisper 接口进行语音转文字
     */
    public String transcribeAudio(byte[] audioBytes, String filename) throws Exception {
        String boundary = "----NuonuoBoundary" + System.currentTimeMillis();
        String safeFilename = StringUtils.hasText(filename) ? filename : "voice.webm";

        String contentType = safeFilename.endsWith(".wav") ? "audio/wav"
                : safeFilename.endsWith(".mp3") ? "audio/mpeg"
                : "audio/webm";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Disposition: form-data; name=\"model\"\r\n\r\nwhisper-1\r\n".getBytes(StandardCharsets.UTF_8));
        baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + safeFilename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write(audioBytes);
        baos.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        String endpoint = properties.getBaseUrl().replaceAll("/+$", "") + "/audio/transcriptions";
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("STT API returned HTTP " + response.statusCode() + ": " + response.body());
        }
        return objectMapper.readTree(response.body()).path("text").asText("");
    }
}
