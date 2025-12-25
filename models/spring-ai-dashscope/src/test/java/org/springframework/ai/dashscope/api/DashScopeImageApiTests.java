package org.springframework.ai.dashscope.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.dashscope.api.DashScopeApi.ChatCompletionRequest;
import org.springframework.ai.dashscope.api.DashScopeImageApi.DashScopeImageResponse;
import org.springframework.ai.dashscope.api.dto.DashScopeImageRequest;
import org.springframework.http.HttpEntity;

//@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
public class DashScopeImageApiTests {

    private static final Logger log = LoggerFactory.getLogger(DashScopeImageApiTests.class);

    DashScopeImageApi dashScopeImageApi = DashScopeImageApi.builder().apiKey(System.getenv("DASHSCOPE_API_KEY")).build();
    
    DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("DASHSCOPE_API_KEY")).build();

    @Test
    public void submit(){
        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest(null, false);
        dashScopeApi.chatCompletionEntity(chatCompletionRequest);
    }

}
