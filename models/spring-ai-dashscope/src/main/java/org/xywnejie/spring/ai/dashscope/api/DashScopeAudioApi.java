package org.xywnejie.spring.ai.dashscope.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Consumer;

public class DashScopeAudioApi {

    private final RestClient restClient;

    private final WebClient webClient;

    private final WebSocketClient webSocketClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DashScopeAudioApi(String baseUrl, ApiKey apiKey, HttpHeaders headers, RestClient.Builder restClientBuilder,
                             WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler){
        Consumer<HttpHeaders> authHeaders = h -> h.addAll(HttpHeaders.readOnlyHttpHeaders(headers));

        this.restClient = restClientBuilder.clone()
                .baseUrl(baseUrl)
                .defaultHeaders(authHeaders)
                .defaultStatusHandler(responseErrorHandler)
                .defaultRequest(requestHeadersSpec -> {
                    if(!(apiKey instanceof NoopApiKey)){
                        requestHeadersSpec.header(HttpHeaders.AUTHORIZATION,"Bearer "+ apiKey.getValue());
                    }
                })
                .build();

        this.webClient = webClientBuilder.clone()
                .baseUrl(baseUrl)
                .defaultHeaders(authHeaders)
                .defaultRequest(requestHeadersSpec -> {
                    if(!(apiKey instanceof  NoopApiKey)){
                        requestHeadersSpec.header(HttpHeaders.AUTHORIZATION,"Bearer "+ apiKey.getValue());
                    }
                })
                .build();

        webSocketClient = new ReactorNettyWebSocketClient();
    }

    public ResponseEntity<SpeechResponse> createSpeech(SpeechRequest requestBody){
        Assert.isTrue(!requestBody.stream,"Non-streaming only for qwen3-tts-flash");
        Assert.isTrue(requestBody.model.equals("qwen3-tts-flash"),"Only qwen3-tts-flash supported here");
        return this.restClient.post()
                .uri("/api/v1/services/aigc/multimodal-generation/generation")
                .body(requestBody).retrieve().toEntity(SpeechResponse.class);
    }

    public Flux<SpeechResponse> stream(SpeechRequest requestBody){
        Assert.isTrue(requestBody.stream,"Streaming only");
        Assert.isTrue(requestBody.model.equals("qwen3-tts-flash"),"Only qwen3-tts-flash supported here");
        return this.webClient.post()
                .uri("/api/v1/services/aigc/multimodal-generation/generation")
                .body(Mono.just(requestBody),SpeechRequest.class)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve().bodyToFlux(SpeechResponse.class);
    }

    private String generateRunTaskCommand(String taskId){
        Map initMap = Map.of(
                "header",Map.of(
                        "action","run-task",
                        "task_id",taskId,
                        "streaming","duplex"
                ),
                "payload",Map.of(
                        "task_group","audio",
                        "task","tts",
                        "function","SpeechSynthesizer",
                        "model","cosyvoice-v3-flash"
                )
        );
        try {
            return objectMapper.writeValueAsString(initMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CosyVoiceRequest(Header header,Payload payload){

        public record Header(
                @JsonProperty("action") String action,
                @JsonProperty("task_id") String taskId,
                @JsonProperty("streaming") String streaming
        ){}

        public record Payload(){}
    }

    public enum TtsModel {
        QWEN3_TTS_FLASH("qwen3-tts-flash");

        public final String value;

        TtsModel(String value){
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public record RunTaskCommand(){}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SpeechRequest(
            @JsonProperty("model") String model,
            @JsonProperty("text") String text,
            @JsonProperty("voice") String voice,
            @JsonProperty("language_type") String languageType,
            @JsonProperty("stream") Boolean stream
    ){}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SpeechResponse(
            @JsonProperty("output") Output output,
            @JsonProperty("usage") Usage usage,
            @JsonProperty("request_id") String requestId
    ){
        public record Output(
                @JsonProperty("finish_reason") String finishReason,
                @JsonProperty("audio") Audio audio
        ){}

        public record Audio(
                @JsonProperty("url") String url,
                @JsonProperty("data") String data,
                @JsonProperty("id") String id,
                @JsonProperty("expires_at") Integer expiresAt

        ){}

        public record Usage(
                @JsonProperty("input_tokens_details") InputTokensDetails inputTokensDetails,
                @JsonProperty("total_tokens") Integer totalTokens,
                @JsonProperty("output_tokens") Integer outputTokens,
                @JsonProperty("input_tokens") Integer inputTokens,
                @JsonProperty("output_tokens_details") OutputTokenDetails outputTokenDetails

        ){}

        public record InputTokensDetails(){}

        public record OutputTokenDetails(){}
    }
}
