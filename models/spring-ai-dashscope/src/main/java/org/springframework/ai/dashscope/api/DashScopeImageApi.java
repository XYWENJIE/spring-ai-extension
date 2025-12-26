package org.springframework.ai.dashscope.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 图像生成API，新的2.6版本用普通DashScopeApi来实现，该类兼容以往的操作。
 * @author Huang Wenjie
 */
public class DashScopeImageApi {

    private static final Logger logger = LoggerFactory.getLogger(DashScopeImageApi.class);

    private static final String DEFAULT_IMAGE_MODEL = ImageModel.WAN2_2_T2I_FLASH.getValue();

    private ObjectMapper objectMapper = new ObjectMapper();

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

    /**
     * 获取多模态生成结果的同步方法
     * @return 多模态生成的响应结果
     */
    public HttpEntity<DashScopeImageResponse> getMultimodalGenerationResult(ImageRequest imageRequest){
        return this.restClient.post().uri("/api/v1/services/aigc/multimodal-generation/generation")
        		.body(imageRequest).retrieve().toEntity(DashScopeImageResponse.class);
    }

    public HttpEntity<DashScopeImageResponse> submitImageGenTask(ImageRequest imageRequest) {
        Assert.isTrue(imageRequest.model().equals("wan2.6-t2i"), "当前模型"+imageRequest.model()+"该模型只能同步发送getMultimodalGenerationResult方法");
        try {
            String body = objectMapper.writeValueAsString(imageRequest);
            logger.info(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return this.restClient.post().uri("/api/v1/services/aigc/text2image/image-synthesis").header("X-DashScope-Async", "enable")
        		.body(imageRequest).retrieve().toEntity(DashScopeImageResponse.class);
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

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record Input(String prompt,@JsonProperty("negative_prompt") String negativePrompt){}

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record Parameters(
                @JsonProperty("size") String size,
                Integer n,
                @JsonProperty("prompt_extend") Boolean promptExtend,
                @JsonProperty("watermark") Boolean watermark,
                Integer seed){

        }
    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DashScopeImageResponse(
            @JsonProperty("output") Output output,
            @JsonProperty("request_id") String requestId,
            String code,String message,Usage usage){

        public record Output(
                @JsonProperty("task_id") String taskId,
                @JsonProperty("task_status") String taskStatus,
                @JsonProperty("results") List<Result> results,
                @JsonProperty("task_metrics") TaskMetrics taskMetrics,
                @JsonProperty("code") String code,
                @JsonProperty("message") String message){}

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

        public Builder apiKey(String apiKey){
            Assert.notNull(apiKey,"apiKey cannot be null");
            this.apiKey = new SimpleApiKey(apiKey);
            return this;
        }

        public DashScopeImageApi build(){
            Assert.notNull(this.apiKey,"apiKey cannot be null");
            return new DashScopeImageApi(this.baseUrl,this.apiKey,this.headers,this.restClientBuilder,responseErrorHandler);
        }
    }
}

