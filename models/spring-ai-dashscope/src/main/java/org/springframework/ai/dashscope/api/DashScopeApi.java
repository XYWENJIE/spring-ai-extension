package org.springframework.ai.dashscope.api;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.ChatModelDescription;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DashScopeApi {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final ObjectMapper objectMapper = new ObjectMapper();

	public static Builder builder() {
		return new Builder();
	}

	public static final DashScopeApi.ChatModel DEFAULT_CAHT_MODEL = ChatModel.QWEN_PLUS;

	private String completionsPath;

	private final RestClient restClient;

	private final WebClient webClient;

	public DashScopeApi(String baseUrl, ApiKey apiKey, MultiValueMap<String, String> headers, String completionsPath,
			RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder,
			ResponseErrorHandler responseErrorHandler) {
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

	public ResponseEntity<ChatCompletion> chatCompletionEntity(ChatCompletionRequest chatRequest) {
		return chatCompletionEntity(chatRequest, new LinkedMultiValueMap<>());
	}

	public ResponseEntity<ChatCompletion> chatCompletionEntity(ChatCompletionRequest chatRequest,
			MultiValueMap<String, String> additionalHttpHeader) {
		Assert.notNull(chatRequest, "The request body can not be null.");
		Assert.isTrue(!chatRequest.stream(), "Request must set the stream property to false.");
		Assert.notNull(additionalHttpHeader, "The additional HTTP headers can not be null.");
		return this.restClient.post().uri(completionsPath).headers(headers -> headers.addAll(additionalHttpHeader))
				.body(chatRequest).retrieve().toEntity(ChatCompletion.class);
	}

	public Flux<ChatCompletion> chatCompletionStream(ChatCompletionRequest chatRequest) {
		return this.chatCompletionStream(chatRequest, new LinkedMultiValueMap<String, String>());
	}

	public Flux<ChatCompletion> chatCompletionStream(ChatCompletionRequest chatRequest,
			MultiValueMap<String, String> additionalHttpHeader) {
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
		
		public ChatCompletionMessage(String content,Role role,List<ToolCall> toolCalls){
			this.rawContent = content;
			this.role = role;
			this.toolCalls = toolCalls;
		}
		
		public ChatCompletionMessage(String content,Role role,String toolCallId){
			this.rawContent = content;
			this.role = role;
			this.toolCallId = toolCallId;
		}

		public String getContent() {
			if (this.rawContent == null) {
				return null;
			}
			if (this.rawContent instanceof String text) {
				return text;
			}
			throw new IllegalStateException("The content is not a string!");
		}

		public void setContent(Object rawContent) {
			this.rawContent = rawContent;
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

		public record MediaContent(@JsonProperty("text") String text, @JsonProperty("image") String image,
				@JsonProperty("video") String[] videos, @JsonProperty("fps") Float fps,
				@JsonProperty("audio") String audio) {

			public MediaContent(String text) {
				this(text, null, null, null, null);
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

		private MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

		private String completionsPath = "/api/v1/services/aigc/text-generation/generation";

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

		public Builder completionsPath(String completionsPath) {
			Assert.hasText(completionsPath, "completionsPath cannot be null or empty");
			this.completionsPath = completionsPath;
			return this;
		}

		public DashScopeApi build() {
			Assert.notNull(this.apiKey, "apiKey must be set");
			return new DashScopeApi(this.baseUrl, this.apiKey, this.headers, this.completionsPath,
					this.restClientBuilder, this.webClientBuilder, this.responseErrorHandler);
		}
	}
}
