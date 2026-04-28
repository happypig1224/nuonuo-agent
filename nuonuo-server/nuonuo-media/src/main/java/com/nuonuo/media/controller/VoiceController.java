package com.nuonuo.media.controller;

import com.nuonuo.common.model.Result;
import com.nuonuo.common.utils.LlmClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 语音转文字控制器
 */
@RestController
@RequestMapping("/api/v1/voice")
public class VoiceController {

    private final LlmClient llmClient;

    public VoiceController(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @PostMapping("/transcribe")
    public Result<Map<String, String>> transcribe(@RequestParam("audio") MultipartFile audio) throws Exception {
        if (audio == null || audio.isEmpty()) {
            throw new IllegalArgumentException("音频文件不能为空");
        }
        String text = llmClient.transcribeAudio(audio.getBytes(), audio.getOriginalFilename());
        return Result.success(Map.of("text", text));
    }
}
