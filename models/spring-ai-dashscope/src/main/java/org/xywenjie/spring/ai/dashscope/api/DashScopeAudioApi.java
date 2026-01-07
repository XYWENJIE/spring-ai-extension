package org.xywenjie.spring.ai.dashscope.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeRequest;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeResponse;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 阿里云DashScope音频API客户端
 * 支持文本转语音(TTS)功能
 * 
 * @author Huang Wenjie(黄文杰)
 */
public class DashScopeAudioApi {

	/**
	 * REST客户端，用于同步HTTP请求
	 */
	private final RestClient restClient;

	/**
	 * Web客户端，用于异步HTTP请求和流式响应
	 */
	private final WebClient webClient;

	/**
	 * WebSocket客户端，用于WebSocket连接
	 */
	private final WebSocketClient webSocketClient;

	/**
	 * JSON对象映射器，用于JSON序列化和反序列化
	 */
	private final ObjectMapper objectMapper = new ObjectMapper();

	public DashScopeAudioApi(String baseUrl, ApiKey apiKey, HttpHeaders headers, RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {
		Consumer<HttpHeaders> authHeaders = h -> h.addAll(HttpHeaders.readOnlyHttpHeaders(headers));

		this.restClient = restClientBuilder.clone().baseUrl(baseUrl).defaultHeaders(authHeaders)
				.defaultStatusHandler(responseErrorHandler).defaultRequest(requestHeadersSpec -> {
					if (!(apiKey instanceof NoopApiKey)) {
						requestHeadersSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.getValue());
					}
				}).build();

		this.webClient = webClientBuilder.clone().baseUrl(baseUrl).defaultHeaders(authHeaders)
				.defaultRequest(requestHeadersSpec -> {
					if (!(apiKey instanceof NoopApiKey)) {
						requestHeadersSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.getValue());
					}
				}).build();

		webSocketClient = new ReactorNettyWebSocketClient();
	}

	/**
	 * 创建构建器实例
	 * @return Builder构建器实例
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 创建语音（文本转语音）
	 * @param request 语音请求参数
	 * @return 语音响应实体
	 */
	public ResponseEntity<DashScopeResponse> createSpeech(DashScopeRequest request) {
		Assert.isTrue(request.getModel().equals("qwen3-tts-flash"), "Only qwen3-tts-flash supported here");
		return this.restClient.post().uri("/api/v1/services/aigc/multimodal-generation/generation").body(request)
				.retrieve().toEntity(DashScopeResponse.class);
	}

	/**
	 * 流式创建语音
	 * @param requestBody 语音请求参数
	 * @return 流式响应实体
	 */
	public Flux<ResponseEntity<DashScopeResponse>> stream(DashScopeRequest requestBody) {
		Assert.isTrue(requestBody.getModel().equals("qwen3-tts-flash"), "Only qwen3-tts-flash supported here");
		return this.webClient.post().uri("/api/v1/services/aigc/multimodal-generation/generation")
				.body(Mono.just(requestBody), DashScopeRequest.class).accept(MediaType.APPLICATION_JSON)
				.exchangeToFlux(clientResponse -> {
					HttpHeaders headers = clientResponse.headers().asHttpHeaders();
					return clientResponse.bodyToFlux(DashScopeResponse.class)
							.map(response -> ResponseEntity.ok().headers(headers).body(response));
				});
	}

	/**
	 * 生成运行任务的WebSocket命令
	 * @param taskId 任务ID
	 * @return JSON格式的命令字符串
	 */
	private String generateRunTaskCommand(String taskId) {
		Map initMap = Map.of("header", Map.of("action", "run-task", "task_id", taskId, "streaming", "duplex"),
				"payload", Map.of("task_group", "audio", "task", "tts", "function", "SpeechSynthesizer", "model",
						"cosyvoice-v3-flash"));
		try {
			return objectMapper.writeValueAsString(initMap);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * CosyVoice语音合成请求
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record CosyVoiceRequest(Header header, Payload payload) {

		/**
		 * 请求头信息
		 */
		public record Header(@JsonProperty("action") String action, @JsonProperty("task_id") String taskId,
				@JsonProperty("streaming") String streaming) {
		}

		/**
		 * 请求负载信息
		 */
		public record Payload() {
		}
	}

	/**
	 * TTS模型枚举
	 */
	public enum TtsModel {
		/**
		 * Qwen3 TTS Flash模型
		 */
		QWEN3_TTS_FLASH("qwen3-tts-flash");

		/**
		 * 模型值
		 */
		public final String value;

		TtsModel(String value) {
			this.value = value;
		}

		/**
		 * 获取模型值
		 * @return 模型值字符串
		 */
		public String getValue() {
			return value;
		}
	}

	/**
	 * 运行任务命令
	 */
	public record RunTaskCommand() {
	}

	/**
	 * 语音合成请求参数
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record SpeechRequest(@JsonProperty("model") String model, @JsonProperty("text") String text,
			@JsonProperty("voice") String voice, @JsonProperty("language_type") String languageType,
			@JsonProperty("stream") Boolean stream) {
	}

	/**
	 * 语音合成响应
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record SpeechResponse(@JsonProperty("output") Output output, @JsonProperty("usage") Usage usage,
			@JsonProperty("request_id") String requestId) {
		/**
		 * 输出信息
		 */
		public record Output(@JsonProperty("finish_reason") String finishReason, @JsonProperty("audio") Audio audio) {
		}

		/**
		 * 音频信息
		 */
		public record Audio(@JsonProperty("url") String url, @JsonProperty("data") String data,
				@JsonProperty("id") String id, @JsonProperty("expires_at") Integer expiresAt

		) {
		}

		/**
		 * 使用量信息
		 */
		public record Usage(@JsonProperty("input_tokens_details") InputTokensDetails inputTokensDetails,
				@JsonProperty("total_tokens") Integer totalTokens, @JsonProperty("output_tokens") Integer outputTokens,
				@JsonProperty("input_tokens") Integer inputTokens,
				@JsonProperty("output_tokens_details") OutputTokenDetails outputTokenDetails

		) {
		}

		/**
		 * 输入token详情
		 */
		public record InputTokensDetails() {
		}

		/**
		 * 输出token详情
		 */
		public record OutputTokenDetails() {
		}
	}

	/**
	 * DashScopeAudioApi构建器
	 */
	public static final class Builder {

		/**
		 * API密钥
		 */
		private ApiKey apiKey;

		/**
		 * 基础URL，默认为阿里云DashScope服务地址
		 */
		private String baseUrl = "https://dashscope.aliyuncs.com";

		/**
		 * HTTP请求头
		 */
		private HttpHeaders httpHeaders = new HttpHeaders();

		/**
		 * REST客户端构建器
		 */
		private RestClient.Builder restClientBuilder = RestClient.builder();

		/**
		 * Web客户端构建器
		 */
		private WebClient.Builder webClientBuilder = WebClient.builder();

		/**
		 * 响应错误处理器
		 */
		private ResponseErrorHandler responseErrorHandler = RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER;

		/**
		 * 设置API密钥
		 * @param apiKey API密钥
		 * @return 构建器实例
		 */
		public Builder apiKey(ApiKey apiKey) {
			Assert.notNull(apiKey, "apikey cannot be null");
			this.apiKey = apiKey;
			return this;
		}

		/**
		 * 构建DashScopeAudioApi实例
		 * @return DashScopeAudioApi实例
		 */
		public DashScopeAudioApi build() {
			Assert.notNull(this.apiKey, "apikey must be set");
			return new DashScopeAudioApi(this.baseUrl, this.apiKey, this.httpHeaders, this.restClientBuilder,
					this.webClientBuilder, this.responseErrorHandler);
		}

	}
}
