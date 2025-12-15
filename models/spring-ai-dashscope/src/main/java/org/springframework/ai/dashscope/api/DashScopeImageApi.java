package org.springframework.ai.dashscope.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.dashscope.DashScopeImageModel;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

import java.util.List;

public class DashScopeImageApi {

    private static final String DEFAULT_IMAGE_MODEL = ImageModel.WAN2_2_T2I_FLASH.getValue();

    private final RestClient restClient;

    public static Builder builder(){
        return new Builder();
    }

    public DashScopeImageApi(String baseUrl, ApiKey apiKey, HttpHeaders headers,
                             RestClient.Builder restClientBuilder, ResponseErrorHandler responseErrorHandler){
        this.restClient = restClientBuilder.clone()
                .baseUrl(baseUrl)
                .defaultHeaders(h -> {
                    h.setContentType(MediaType.APPLICATION_JSON);
                    h.addAll(HttpHeaders.readOnlyHttpHeaders(headers));
                })
                .defaultStatusHandler(responseErrorHandler)
                .defaultRequest(requestHeadersSpec -> {
                    if(!(apiKey instanceof NoopApiKey)){
                        requestHeadersSpec.header(HttpHeaders.AUTHORIZATION,"Bearer "+apiKey.getValue());
                    }
                })
                .build();
    }

    public HttpEntity<DashScopeImageResponse> getImageGenTaskResult(String taskId){
        return this.restClient.get().uri("/api/v1/tasks/{taskId}",taskId).retrieve().toEntity(DashScopeImageResponse.class);
    }

    public HttpEntity<DashScopeImageResponse> submitImageGenTask(ImageRequest imageRequest){
        return this.restClient.post().uri("/api/v1/services/aigc/text2image/image-synthesis").body(imageRequest).retrieve().toEntity(DashScopeImageResponse.class);
    }

    public enum ImageModel{
        WAN2_5_T2I_PREVIEW("wan2.5-t2i-preview"),
        WAN2_2_T2I_FLASH("wan2.2-t2i-flash");

        private final String value;

        ImageModel(String value){
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ImageRequest(String model,Input input,Parameters parameters){

        public record Input(String prompt,@JsonProperty("negative_prompt") String negativePrompt){}

        public record Parameters(
                @JsonProperty("size") String size,
                Integer n,
                @JsonProperty("prompt_extend") Boolean promptExtend,
                @JsonProperty("watermark") Boolean watermark,
                Integer seed){

        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DashScopeImageResponse(Output output,@JsonProperty("request_id") String requestId,String code,String message,Usage usage){

        public record Output(String taskId, String taskStatus, List<Result> results,TaskMetrics taskMetrics,String code,String message){}

        public record Result(String url,String code,String message){}

        public record TaskMetrics(
                @JsonProperty("TOTAL") Integer total,
                @JsonProperty("SUCCEEDED") Integer succeeded,
                @JsonProperty("FAILED") Integer failed){}

        public record Usage(@JsonProperty("image_count") Integer imageCount){}

    }

    public static final class Builder{

        private String baseUrl = "https://dashscope.aliyuncs.com";

        private ApiKey apiKey;

        private HttpHeaders headers = new HttpHeaders();

        private RestClient.Builder restClientBuilder = RestClient.builder();

        private ResponseErrorHandler responseErrorHandler = RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER;

        public Builder apiKey(ApiKey apiKey){
            Assert.notNull(apiKey,"apiKey cannot be null");
            this.apiKey = apiKey;
            return this;
        }

        public DashScopeImageApi build(){
            Assert.notNull(this.apiKey,"apiKey cannot be null");
            return new DashScopeImageApi(this.baseUrl,this.apiKey,this.headers,this.restClientBuilder,responseErrorHandler);
        }
    }
}
