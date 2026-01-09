package org.xywenjie.spring.ai.dashscope.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	
	private final Logger log = LoggerFactory.getLogger(getClass());

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
