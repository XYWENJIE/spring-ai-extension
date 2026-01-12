package org.xywenjie.spring.ai.dashscope.api;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
		if(chatRequest.getModel().contains("vl")) {
			completionsPath = multimodelPath;
		}
		//log.info("提交参数：body:{}", ModelOptionsUtils.toJsonString(chatRequest));
		return this.restClient.post().uri(completionsPath).headers(headers -> {
					headers.addAll(additionalHttpHeader);
					//headers.setBearerAuth(apiKey.getValue());
					//log.info("Header:{}",headers);
				})
				.body(chatRequest).retrieve().toEntity(DashScopeResponse.class);
	}

	public Flux<ChatCompletion> chatCompletionStream(ChatCompletionRequest chatRequest) {
		return this.chatCompletionStream(chatRequest, new HttpHeaders());
	}

	/*** 发起流式聊天完成请求，以Flux形式返回响应数据。
	 ** 此方法支持服务器发送事件(SSE)，用于接收实时的聊天完成结果流。
	 *
	 * @param chatRequest 聊天完成请求对象，必须设置stream属性为true
	 * @param additionalHttpHeader 额外的HTTP头信息，用于自定义请求头
	 * @return Flux<ChatCompletion> 包含聊天完成结果流的响应对象
	 **/
	public Flux<ChatCompletion> chatCompletionStream(ChatCompletionRequest chatRequest,
			HttpHeaders additionalHttpHeader) {
		Assert.notNull(chatRequest, "The request body can not be null.");
		Assert.isTrue(chatRequest.stream(), "Request must set the stream property to true.");
		additionalHttpHeader.add("X-DashScope-SSE", "enable");
		return this.webClient.post().uri(this.completionsPath).headers(headers -> headers.addAll(additionalHttpHeader))
				.body(Mono.just(chatRequest), ChatCompletionRequest.class).retrieve().bodyToFlux(ChatCompletion.class);
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

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ChatCompletionRequest(@JsonProperty("input") Input input, @JsonProperty("model") String model,
			@JsonProperty("stream") Boolean stream, @JsonProperty("parameters") Parameters parameters) {

		public ChatCompletionRequest(List<ChatCompletionMessage> messages, Boolean stream) {
			this(new Input(messages), null, stream, new Parameters());
		}
	};

	public record Input(@JsonProperty("messages") List<ChatCompletionMessage> messages) {

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Parameters {

		@JsonProperty("temperature")
		private Float temperature;

		@JsonProperty("seed")
		private Integer seed;

		@JsonProperty("stream")
		private Boolean stream;

		@JsonProperty("incremental_output")
		private Boolean incrementalOutput;

		@JsonProperty("result_format")
		private String resultFormat = "message";

		@JsonProperty("tool_choice")
		private String toolChoice = "auto";

		@JsonProperty("tools")
		private List<FunctionTool> tools;

		@JsonProperty("parallel_tool_calls")
		private Boolean parallelToolCalls = Boolean.TRUE;

		public Float getTemperature() {
			return temperature;
		}

		public void setTemperature(Float temperature) {
			this.temperature = temperature;
		}

		public Integer getSeed() {
			return seed;
		}

		public void setSeed(Integer seed) {
			this.seed = seed;
		}

		public Boolean getStream() {
			return stream;
		}

		public void setStream(Boolean stream) {
			this.stream = stream;
		}

		public Boolean getIncrementalOutput() {
			return incrementalOutput;
		}

		public void setIncrementalOutput(Boolean incrementalOutput) {
			this.incrementalOutput = incrementalOutput;
		}

		public String getResultFormat() {
			return resultFormat;
		}

		public void setResultFormat(String resultFormat) {
			this.resultFormat = resultFormat;
		}

		public String getToolChoice() {
			return toolChoice;
		}

		public void setToolChoice(String toolChoice) {
			this.toolChoice = toolChoice;
		}

		public List<FunctionTool> getTools() {
			return tools;
		}

		public void setTools(List<FunctionTool> tools) {
			this.tools = tools;
		}

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ChatCompletionMessage{
		
		@JsonProperty("content") Object rawContent;
		@JsonProperty("role") Role role;
		@JsonProperty("tool_call_id") String toolCallId;
		@JsonProperty("reasoning_content") String reasoningContent;
		@JsonProperty("tool_calls") List<ToolCall> toolCalls;
		
		public ChatCompletionMessage() {
			
		}

		public ChatCompletionMessage(Object content,Role role){
			this.rawContent = content;
			this.role = role;
		}
		
		public ChatCompletionMessage(Object content,Role role,List<ToolCall> toolCalls){
			this.rawContent = content;
			this.role = role;
			this.toolCalls = toolCalls;
		}
		
		public ChatCompletionMessage(Object content,Role role,String toolCallId){
			this.rawContent = content;
			this.role = role;
			this.toolCallId = toolCallId;
		}

		public Object getContent() {
			if (this.rawContent == null) {
				return null;
			}
			if (this.rawContent instanceof String text) {
				return text;
			}
			//throw new IllegalStateException("The content is not a string!");
			return rawContent;
		}

		@SuppressWarnings("unchecked")
		public void setContent(Object rawContent) {
			if(rawContent instanceof ArrayList<?> contentListValue) {
				this.rawContent = contentListValue.stream().map(element -> {
					LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>)element;
					MediaContent mediaContent = new MediaContent();
					if(!ObjectUtils.isEmpty(map.get("text"))){
						mediaContent.setText(ObjectUtils.getDisplayString(map.get("text")));
					}
					if(!ObjectUtils.isEmpty(map.get("image"))){
						mediaContent.setImage(ObjectUtils.getDisplayString(map.get("image")));
					}
					return mediaContent;
				}).toList();
			}else {
				this.rawContent = rawContent;	
			}
		}

		public Role getRole() {
			return role;
		}

		public void setRole(Role role) {
			this.role = role;
		}

		public String getToolCallId() {
			return toolCallId;
		}

		public void setToolCallId(String toolCallId) {
			this.toolCallId = toolCallId;
		}

		public String getReasoningContent() {
			return reasoningContent;
		}

		public void setReasoningContent(String reasoningContent) {
			this.reasoningContent = reasoningContent;
		}

		public List<ToolCall> getToolCalls() {
			return toolCalls;
		}

		public void setToolCalls(List<ToolCall> toolCalls) {
			this.toolCalls = toolCalls;
		}



		public enum Role {
			@JsonProperty("system")
			SYSTEM, @JsonProperty("user")
			USER, @JsonProperty("assistant")
			ASSISTANT, @JsonProperty("tool")
			TOOL
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class MediaContent {
			
			private @JsonProperty("text") String text;
			private @JsonProperty("image") String image;
			private @JsonProperty("video") String[] videos;
			private @JsonProperty("fps") Float fps;
			private @JsonProperty("audio") String audio;
			
			public MediaContent() {
			}
			
			public MediaContent(String text, String image, String[] videos, Float fps, String audio) {
				this.text = text;
				this.image = image;
				this.videos = videos;
				this.fps = fps;
				this.audio = audio;
			}

			public MediaContent(String text) {
				this(text, null, null, null, null);
			}

			public String getText() {
				return text;
			}

			public void setText(String text) {
				this.text = text;
			}

			public String getImage() {
				return image;
			}

			public void setImage(String image) {
				this.image = image;
			}

			public String[] getVideos() {
				return videos;
			}

			public void setVideos(String[] videos) {
				this.videos = videos;
			}

			public Float getFps() {
				return fps;
			}

			public void setFps(Float fps) {
				this.fps = fps;
			}

			public String getAudio() {
				return audio;
			}

			public void setAudio(String audio) {
				this.audio = audio;
			}
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public record ToolCall(@JsonProperty("index") Integer index, @JsonProperty("id") String id,
				@JsonProperty("type") String type, @JsonProperty("function") ChatCompletionFunction function) {

			public ToolCall(String id, String type, ChatCompletionFunction function) {
				this(null, id, type, function);
			}

		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class ChatCompletionFunction {
			
			@JsonProperty("name")
			private String name;
			
			@JsonProperty("arguments")
			private String arguments;
			
			public ChatCompletionFunction() {}

			public ChatCompletionFunction(String name, String arguments) {
				super();
				this.name = name;
				this.arguments = arguments;
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getArguments() {
				return arguments;
			}

			public void setArguments(String arguments) {
				this.arguments = arguments;
			}
			
		}

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ChatCompletion(@JsonProperty("status_code") Integer statusCode,
			@JsonProperty("request_id") String requestId, @JsonProperty("code") String code,
			@JsonProperty("output") Output output, @JsonProperty("usage") Usage usage) {

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Output(@JsonProperty("text") String text, @JsonProperty("finish_reason") String finishReason,
			@JsonProperty("choices") List<Choice> choices) {

	}

	public record Choice(@JsonProperty("finish_reason") String finishReason,
			@JsonProperty("message") ChatCompletionMessage message) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Usage(@JsonProperty("total_tokens") Integer totalTokens,
			@JsonProperty("input_tokens") Integer inputTokens, @JsonProperty("output_tokens") Integer outputTokens) {

	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class FunctionTool {

		@JsonProperty("type")
		private Type type = Type.FUNCTION;

		@JsonProperty("function")
		private Function function;

		public FunctionTool() {

		}

		public FunctionTool(Type type, Function function) {
			this.type = type;
			this.function = function;
		}

		public FunctionTool(Function function) {
			this(Type.FUNCTION, function);
		}

		public Type getType() {
			return type;
		}

		public void setType(Type type) {
			this.type = type;
		}

		public Function getFunction() {
			return function;
		}

		public void setFunction(Function function) {
			this.function = function;
		}

		public enum Type {
			@JsonProperty("function")
			FUNCTION
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class Function {

			@JsonProperty("name")
			private String name;

			@JsonProperty("description")
			private String description;

			@JsonProperty("parameters")
			private Map<String, Object> parameters;

			public Function() {
			}

			public Function(String description, String name, Map<String, Object> parameters) {
				this.description = description;
				this.name = name;
				this.parameters = parameters;
			}

			public Function(String description, String name, String jsonSchema) {
				this(description, name, ModelOptionsUtils.jsonToMap(jsonSchema));
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getDescription() {
				return description;
			}

			public void setDescription(String description) {
				this.description = description;
			}

			public Map<String, Object> getParameters() {
				return parameters;
			}

			public void setParameters(Map<String, Object> parameters) {
				this.parameters = parameters;
			}

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

		public DashScopeApi build() {
			Assert.notNull(this.apiKey, "apiKey must be set");
			HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
			JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
			requestFactory.setReadTimeout(Duration.ofSeconds(45));
			this.restClientBuilder.requestFactory(requestFactory);
			return new DashScopeApi(this.baseUrl, this.apiKey, this.headers, this.completionsPath,
					this.restClientBuilder, this.webClientBuilder, this.responseErrorHandler);
		}
	}
}
