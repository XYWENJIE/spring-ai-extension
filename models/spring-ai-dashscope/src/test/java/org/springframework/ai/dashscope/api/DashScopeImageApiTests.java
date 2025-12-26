package org.springframework.ai.dashscope.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.dashscope.api.DashScopeApi.ChatCompletionRequest;
import org.springframework.ai.dashscope.api.dto.DashScopeRequest;
import org.springframework.ai.dashscope.api.dto.DashScopeResponse;
import org.springframework.ai.dashscope.api.dto.DashScopeRequest.Resolution;
import org.springframework.ai.dashscope.api.dto.DashScopeRequest.ShotType;
import org.springframework.http.HttpEntity;

@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
public class DashScopeImageApiTests {

    private static final Logger log = LoggerFactory.getLogger(DashScopeImageApiTests.class);

    DashScopeImageApi dashScopeImageApi = DashScopeImageApi.builder().apiKey(System.getenv("DASHSCOPE_API_KEY")).build();
    
    DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("DASHSCOPE_API_KEY")).build();

    DashScopeVideoApi dashScopeVideoApi = DashScopeVideoApi.builder().apiKey(System.getenv("DASHSCOPE_API_KEY")).build();

    //@Test
    public void submit(){
        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest(null, false);
        dashScopeApi.chatCompletionEntity(chatCompletionRequest);
    }

    /**
     * 通义万相-图生视频-基于首帧
     */
    //@Test
    public void testImageToVideoWithFirstFrame() throws InterruptedException {
        DashScopeRequest request = DashScopeRequest.builder()
            .model("wan2.6-i2v")
            .prompt(
                """
                一幅都市奇幻艺术的场景。一个充满动感的涂鸦艺术角色。一个由喷漆所画成的少年，正从一面混凝土墙上活过来。他一边用极快的语速演唱一首英文rap，一边摆着一个经典的、充满活力的说唱歌手姿势。场景设定在夜晚一个充满都市感的铁路桥下。灯光来自一盏孤零零的街灯，营造出电影般的氛围，充满高能量和惊人的细节。视频的音频部分完全由他的rap构成，没有其他对话或杂音。
                """
            )
            .imgUrl("https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20250925/wpimhv/rap.png")
            .audioUrl("https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20250925/ozwpvi/rap.mp3")
            .resolution(Resolution.P720)
            .promptExtend(true)
            .duration(10)
            .shotType(ShotType.MULTI)
            .build();
            
        HttpEntity<DashScopeResponse> responseEntity = dashScopeVideoApi.submitImageGenTask(request);
        
        // 添加日志输出以便调试
        log.info("Response headers: {}", responseEntity.getHeaders());
        if (responseEntity.getBody() != null) {
            log.info("Response body: {}", responseEntity.getBody());
        } else {
            log.warn("Response body is null");
        }
        
        DashScopeResponse response = responseEntity.getBody();
        if (response != null && response.getOutput() != null) {
            String taskId = response.getOutput().getTaskId();
            log.info("Submitted task with ID: {}", taskId);
            
            // 每15秒轮询一次任务结果，直到获取最终结果
            DashScopeResponse result = null;
            while (true) {
                HttpEntity<DashScopeResponse> resultEntity = dashScopeVideoApi.getImageGenTaskResult(taskId);
                if (resultEntity != null && resultEntity.getBody() != null) {
                    result = resultEntity.getBody();
                    String taskStatus = result.getOutput().getTaskStatus();
                    log.info("Task status: {}", taskStatus);
                    
                    // 如果任务完成或失败，则退出循环
                    if ("SUCCEEDED".equalsIgnoreCase(taskStatus) || "FAILED".equalsIgnoreCase(taskStatus)) {
                        log.info("Task completed with status: {}", taskStatus);
                        if ("SUCCEEDED".equalsIgnoreCase(taskStatus)) {
                            log.info("Video URL: {}", result.getOutput().getVideoUrl());
                        }
                        break;
                    }
                } else {
                    log.warn("Received null response for task result");
                    break;
                }
                
                // 等待15秒后再次查询
                Thread.sleep(15000);
            }
        } else {
            log.error("Failed to get response or task ID from initial request");
        }
    }

}