package org.xywnejie.spring.ai.dashscope.api;

import java.util.function.Consumer;

import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeRequest;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeResponse;

/**
 * @author Huang Wenjie
 */
public class DashScopeVideoApi {

    private final RestClient restClient;

    public DashScopeVideoApi(String baseUrl, ApiKey apiKey, HttpHeaders headers, RestClient.Builder restClientBuilder,ResponseErrorHandler responseErrorHandler) {
        Consumer<HttpHeaders> finalHeaders = h -> {
            if(!(apiKey instanceof  NoopApiKey)){
                h.add(HttpHeaders.AUTHORIZATION,"Bearer "+ apiKey.getValue());
            }
            h.setContentType(MediaType.APPLICATION_JSON);
            h.add("X-DashScope-Async", "enable");
            h.addAll(headers);
        };
        this.restClient = restClientBuilder.baseUrl(baseUrl).defaultHeaders(finalHeaders).defaultStatusHandler(responseErrorHandler).build();
    }

    public HttpEntity<DashScopeResponse> getImageGenTaskResult(String taskId){
        return this.restClient.get().uri("/api/v1/tasks/{task_id}", taskId).retrieve().toEntity(DashScopeResponse.class);
    }

    public HttpEntity<DashScopeResponse> submitImageGenTask(DashScopeRequest request){
        return this.restClient.post().uri("/api/v1/services/aigc/video-generation/video-synthesis").body(request).retrieve().toEntity(DashScopeResponse.class);
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder {

        private String baseUrl = "https://dashscope.aliyuncs.com";

        private ApiKey apiKey;

        private HttpHeaders headers = new HttpHeaders();

        private ResponseErrorHandler responseErrorHandler = RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER;

        private RestClient.Builder restClientBuilder = RestClient.builder();

        public Builder baseUrl(String baseUrl){
            Assert.hasText(baseUrl, "baseUrl must not be empty");
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiKey(ApiKey apiKey){
            this.apiKey = apiKey;
            return this;
        }

        public Builder apiKey(String apiKey){
            this.apiKey = new SimpleApiKey(apiKey);
            return this;
        }


        public DashScopeVideoApi build(){
            Assert.notNull(this.apiKey, "apiKey must not be null");
            Assert.hasText(baseUrl, "baseUrl must not be empty");
            return new DashScopeVideoApi(this.baseUrl,this.apiKey,this.headers,this.restClientBuilder,this.responseErrorHandler);
        }
    }

}
