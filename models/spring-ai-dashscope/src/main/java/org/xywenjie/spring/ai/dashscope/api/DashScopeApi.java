package org.xywenjie.spring.ai.dashscope.api;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.*;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.util.*;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeRequest;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Huang Wenjie
 */
public class DashScopeApi {

	private static final Logger log = LoggerFactory.getLogger(DashScopeApi.class);

	public static Builder builder() {
		return new Builder();
	}

	public static final DashScopeApi.ChatModel DEFAULT_CHAT_MODEL = ChatModel.QWEN_PLUS;
	
	public static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-v4";

	private String completionsPath;
	
	private String multimodelPath = "/api/v1/services/aigc/multimodal-generation/generation";
	
	private String embeddingsPath = "/api/v1/services/embeddings/text-embedding/text-embedding";
	
	private final ApiKey apiKey;

	private final RestClient restClient;

	private final WebClient webClient;

	/**
	 * 构造函数，用于初始化DashScopeApi实例。
	 * 设置API的基本配置，包括基础URL、认证密钥、HTTP头、客户端构建器等。
	 *
	 * @param baseUrl 基础URL，API服务的基础地址
	 * @param apiKey API密钥，用于身份验证
	 * @param headers HTTP头信息，用于设置额外的请求头
	 * @param completionsPath 补全API端点路径
	 * @param restClientBuilder REST客户端构建器，用于创建同步HTTP客户端
	 * @param webClientBuilder Web客户端构建器，用于创建异步Web客户端
	 * @param responseErrorHandler 响应错误处理器，处理API响应错误
	 */
	public DashScopeApi(String baseUrl, ApiKey apiKey, HttpHeaders headers, String completionsPath,
			RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder,
			ResponseErrorHandler responseErrorHandler) {
		this.apiKey = apiKey;
		this.completionsPath = completionsPath;
		Consumer<HttpHeaders> finalHeaders = h -> {
			if (!(apiKey instanceof NoopApiKey)) {
				h.setBearerAuth(apiKey.getValue());
			}
			h.setContentType(MediaType.APPLICATION_JSON);
			h.addAll(headers);
		};

		this.restClient = restClientBuilder.baseUrl(baseUrl).defaultHeaders(finalHeaders)
				.defaultStatusHandler(responseErrorHandler).build();
		this.webClient = webClientBuilder.baseUrl(baseUrl).defaultHeaders(finalHeaders).build();
	}

	/**
	 * 发起聊天完成请求并返回响应实体。
	 * 此方法是便捷方法，使用默认的HTTP头信息调用带HttpHeaders参数的重载方法。
	 *
	 * @param chatRequest 聊天完成请求对象，包含输入消息、模型等参数
	 * @return ResponseEntity<ChatCompletion> 包含聊天完成结果的响应实体
	 */
	public ResponseEntity<DashScopeResponse> chatCompletionEntity(DashScopeRequest chatRequest) {
		return chatCompletionEntity(chatRequest, new HttpHeaders());
	}

	public ResponseEntity<DashScopeResponse> chatCompletionEntity(DashScopeRequest chatRequest,
			HttpHeaders additionalHttpHeader) {
		Assert.notNull(chatRequest, "The request body can not be null.");
		//Assert.isTrue(!chatRequest.stream(), "Request must set the stream property to false.");
		Assert.notNull(additionalHttpHeader, "The additional HTTP headers can not be null.");
		String requestUri = chatRequest.getModel().contains("vl") || chatRequest.getModel().contains("tts") ? this.multimodelPath : this.completionsPath;
		log.info("提交参数：body:{}", toJsonStringForLog(chatRequest));
		ResponseEntity<String> stringResponseEntity = this.restClient.post().uri(requestUri).headers(headers -> {
					headers.addAll(additionalHttpHeader);
					//headers.setBearerAuth(apiKey.getValue());
					//log.info("Header:{}",headers);
				})
				.body(chatRequest).retrieve().toEntity(String.class);
		String body = stringResponseEntity.getBody();
		log.info("Response Body: {}",body);
		DashScopeResponse dashScopeResponse = ModelOptionsUtils.jsonToObject(body,DashScopeResponse.class);
		return ResponseEntity.ok(dashScopeResponse);
	}

	public Flux<DashScopeResponse> chatCompletionStream(DashScopeRequest chatRequest) {
		return this.chatCompletionStream(chatRequest, new HttpHeaders());
	}

	/*** 发起流式聊天完成请求，以Flux形式返回响应数据。
	 ** 此方法支持服务器发送事件(SSE)，用于接收实时的聊天完成结果流。
	 *
	 * @param chatRequest 聊天完成请求对象，必须设置stream属性为true
	 * @param additionalHttpHeader 额外的HTTP头信息，用于自定义请求头
	 * @return Flux<ChatCompletion> 包含聊天完成结果流的响应对象
	 **/
	public Flux<DashScopeResponse> chatCompletionStream(DashScopeRequest chatRequest,
			HttpHeaders additionalHttpHeader) {
		Assert.notNull(chatRequest, "The request body can not be null.");
		//Assert.isTrue(chatRequest.stream(), "Request must set the stream property to true.");
		additionalHttpHeader.add("X-DashScope-SSE", "enable");
		String requestUri = chatRequest.getModel().contains("vl") || chatRequest.getModel().contains("tts") ? this.multimodelPath : this.completionsPath;
		return this.webClient.post().uri(requestUri).headers(headers -> headers.addAll(additionalHttpHeader))
				.body(Mono.just(chatRequest), DashScopeRequest.class).retrieve()
				.bodyToFlux(DashScopeResponse.class);
				//.doOnNext(rawJson -> log.info("流返回JSON块：{}",rawJson)).map(rawJson -> ModelOptionsUtils.jsonToObject(rawJson,DashScopeResponse.class));
	}

	public enum ChatModel implements ChatModelDescription {
		QWQ_PLUS("qwq-plus"), QWQ_PLUS_LATEST("qwq-plus-latest"), QWEN_MAX("qwen-max"),
		QWEN_MAX_LATEST("qwen-max-latest"), QWEN_PLUS("qwen-plus"), QWEN_PLUS_LATEST("qwen-plus-latest"),
		QWEN_TURBO("qwen-turbo");

		public final String value;

		ChatModel(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		@Override
		public String getName() {
			return this.value;
		}

	}
	
	public ResponseEntity<DashScopeResponse> embeddings(DashScopeRequest request){
		Assert.notNull(request, "");
		
		Assert.notNull(request.getInput(), "The input can not be null.");
		
		return this.restClient.post()
				.uri(this.embeddingsPath)
				.headers(this::addDefaultHeadersIfMissing)
				.body(request)
				.retrieve()
				.toEntity(DashScopeResponse.class);
	}
	
	private void addDefaultHeadersIfMissing(HttpHeaders headers) {
		if(headers.get(HttpHeaders.AUTHORIZATION) == null && !(this.apiKey instanceof NoopApiKey)) {
			headers.setBearerAuth(this.apiKey.getValue());
		}
	}

	public static class Builder {
		private String baseUrl = "https://dashscope.aliyuncs.com";

		private ApiKey apiKey;

		private HttpHeaders headers = new HttpHeaders();

		private String completionsPath = "/api/v1/services/aigc/text-generation/generation";
		
		private String multimodelPath = "/api/v1/services/aigc/multimodal-generation/generation";

		private RestClient.Builder restClientBuilder = RestClient.builder();

		private WebClient.Builder webClientBuilder = WebClient.builder();

		private ResponseErrorHandler responseErrorHandler = RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER;

		public Builder baseUrl(String baseUrl) {
			Assert.hasText(baseUrl, "baseUrl cannot be null or empty");
			this.baseUrl = baseUrl;
			return this;
		}

		public Builder restClientBuilder(RestClient.Builder restClientBuilder){
			Assert.notNull(restClientBuilder,"restClientBuilder cannot be null");
			this.restClientBuilder = restClientBuilder;
			return this;
		}

		public Builder apiKey(ApiKey apiKey) {
			Assert.notNull(apiKey, "apiKey cannot be null");
			this.apiKey = apiKey;
			return this;
		}
		
		public Builder apiKey(String apiKey) {
			Assert.notNull(apiKey, "apiKey cannot be null");
			this.apiKey = new SimpleApiKey(apiKey);
			return this;
		}

		public Builder completionsPath(String completionsPath) {
			Assert.hasText(completionsPath, "completionsPath cannot be null or empty");
			this.completionsPath = completionsPath;
			return this;
		}

		public Builder webClientBuilder(WebClient.Builder webClientBuilder) {
			Assert.notNull(webClientBuilder, "webClientBuilder cannot be null");
			this.webClientBuilder = webClientBuilder;
			return this;
		}

		public DashScopeApi build() {
			Assert.notNull(this.apiKey, "apiKey must be set");
			HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
			JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
			requestFactory.setReadTimeout(Duration.ofSeconds(155));
			this.restClientBuilder.requestFactory(requestFactory);
			return new DashScopeApi(this.baseUrl, this.apiKey, this.headers, this.completionsPath,
					this.restClientBuilder, this.webClientBuilder, this.responseErrorHandler);
		}
	}

	private String toJsonStringForLog(Object obj){
		try{
			ObjectMapper objectMapper = new ObjectMapper();
			SimpleModule simpleModule = new SimpleModule();
			simpleModule.addSerializer(String.class, new JsonSerializer<String>() {
				@Override
				public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
					if(value != null){
						//
						if(value.startsWith("data:image/") && value.length() > 200){
							gen.writeString("[DATA IMAGE TRUNCATED:"+value.substring(0,60)+"]");
							return;
						}
						if(value.length() > 500){
							gen.writeString("[LONG STRING TRUNCATED:"+value.substring(0,100)+"]");
							return;
						}
					}
					gen.writeString(value);
				}
			});
			objectMapper.registerModule(simpleModule);
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
		}catch (Exception e){
			return "[LONG ERROR]"+e.getMessage();
		}
	}
}
