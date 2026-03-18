package com.kingdee.qa.http;

import com.google.gson.Gson;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 百度短语音识别（REST API）服务 - RAW 方式发送裸 PCM
 * 文档：https://ai.baidu.com/ai-doc/SPEECH/Jlbxdezuf
 *
 * 流程：
 *   1. 若前端发来 WAV 文件，去掉 44 字节 WAV 头，取出裸 PCM 数据
 *   2. 用 RAW 方式 POST 到百度，Content-Type: audio/pcm;rate=16000
 *   3. URL 携带 token、cuid、dev_pid=1537
 */
public class BaiduAsrService {

    private static final Logger log = LoggerFactory.getLogger(BaiduAsrService.class);
    private static final String ASR_URL = "http://vop.baidu.com/server_api";
    // WAV 标准文件头长度
    private static final int WAV_HEADER_SIZE = 44;

    private final BaiduTokenService tokenService;
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();

    public BaiduAsrService(BaiduTokenService tokenService, String asrApiKey) {
        this.tokenService = tokenService;
        this.httpClient   = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 语音识别
     *
     * @param audioBytes 音频字节（支持 wav / pcm）
     * @param format     前端传来的格式字符串（wav / pcm）
     * @param sampleRate 采样率（16000 或 8000）
     * @return 识别出的文字
     */
    public String recognize(byte[] audioBytes, String format, int sampleRate) {
        String token = tokenService.getAsrToken();

        // 若是 WAV 格式，去掉文件头取裸 PCM 数据
        byte[] pcmBytes;
        if ("wav".equalsIgnoreCase(format) && audioBytes.length > WAV_HEADER_SIZE) {
            pcmBytes = Arrays.copyOfRange(audioBytes, WAV_HEADER_SIZE, audioBytes.length);
            log.info("[百度ASR] WAV 去头后 PCM 大小: {} bytes", pcmBytes.length);
        } else {
            pcmBytes = audioBytes;
        }

        // RAW 方式：Content-Type 携带格式和采样率
        String url = ASR_URL
                + "?dev_pid=1537"
                + "&cuid=kingdee-qa-client"
                + "&token=" + token;

        String contentType = "audio/pcm;rate=" + sampleRate;

        RequestBody body = RequestBody.create(pcmBytes, MediaType.parse(contentType));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", contentType)
                .build();

        log.info("[百度ASR] 发送请求，采样率: {}, PCM 大小: {} bytes", sampleRate, pcmBytes.length);

        try (Response response = httpClient.newCall(request).execute()) {
            String respBody = response.body() != null ? response.body().string() : "";
            log.info("[百度ASR] 响应: {}", respBody);
            return parseAsrResult(respBody);
        } catch (Exception e) {
            throw new RuntimeException("ASR 请求失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private String parseAsrResult(String jsonBody) {
        Map<String, Object> root = gson.fromJson(jsonBody, Map.class);
        double errNo = root.containsKey("err_no") ? ((Number) root.get("err_no")).doubleValue() : -1;
        if (errNo != 0) {
            String errMsg = (String) root.getOrDefault("err_msg", "未知错误");
            throw new RuntimeException("百度ASR识别失败: " + errMsg + "（err_no=" + (int) errNo + "）");
        }
        List<String> results = (List<String>) root.get("result");
        if (results == null || results.isEmpty()) {
            return "（未识别到语音内容）";
        }
        return results.get(0).trim();
    }
}
