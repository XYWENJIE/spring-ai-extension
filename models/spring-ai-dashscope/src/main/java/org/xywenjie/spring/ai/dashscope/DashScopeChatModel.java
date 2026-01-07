package org.xywenjie.spring.ai.dashscope;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationDocumentation;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.*;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.support.UsageCalculator;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.*;
import org.xywenjie.spring.ai.dashscope.api.DashScopeApi;
import org.xywenjie.spring.ai.dashscope.api.DashScopeApi.ChatCompletion;
import org.xywenjie.spring.ai.dashscope.api.DashScopeApi.ChatCompletionMessage;
import org.xywenjie.spring.ai.dashscope.api.DashScopeApi.ChatCompletionRequest;
import org.xywenjie.spring.ai.dashscope.api.DashScopeApi.Choice;
import org.xywenjie.spring.ai.dashscope.api.DashScopeApi.ChatCompletionMessage.ChatCompletionFunction;
import org.xywenjie.spring.ai.dashscope.api.DashScopeApi.ChatCompletionMessage.MediaContent;
import org.xywenjie.spring.ai.dashscope.api.DashScopeApi.ChatCompletionMessage.Role;
import org.xywenjie.spring.ai.dashscope.api.DashScopeApi.ChatCompletionMessage.ToolCall;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 新方法
 *
 * @author 黄文杰
 */
public class DashScopeChatModel implements ChatModel {

    private final Logger logger = LoggerFactory.getLogger(DashScopeChatModel.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final ChatModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultChatModelObservationConvention();

    private static final ToolCallingManager DEFAULT_TOOL_CALLING_MANAGER = ToolCallingManager.builder().build();

    private final DashScopeChatOptions defaultOptions;

    private final RetryTemplate retryTemplate;

    private final DashScopeApi dashScopeApi;

    private final ObservationRegistry observationRegistry;

    private final ToolCallingManager toolCallingManager;

    private final ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate;

    private final ChatModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;


    public DashScopeChatModel(DashScopeApi dashScopeApi, DashScopeChatOptions defaultOptions,
                              ToolCallingManager toolCallingManager, RetryTemplate retryTemplate,
                              ObservationRegistry observationRegistry, ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate) {
        Assert.notNull(dashScopeApi, "dashScopeApi cannot be null");
        Assert.notNull(defaultOptions, "defaultOptions cannot be null");
        Assert.notNull(toolCallingManager, "toolCallingManager cannot be null");
        Assert.notNull(retryTemplate, "retryTemplate cannot be null");
        Assert.notNull(observationRegistry, "observationRegistry cannot be null");
        Assert.notNull(toolExecutionEligibilityPredicate, "toolExecutionEligibilityPredicate cannot be null");
        this.dashScopeApi = dashScopeApi;
        this.defaultOptions = defaultOptions;
        this.toolCallingManager = toolCallingManager;
        this.retryTemplate = retryTemplate;
        this.observationRegistry = observationRegistry;
        this.toolExecutionEligibilityPredicate = toolExecutionEligibilityPredicate;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        Prompt requestPrompt = buildRequestPrompt(prompt);
        return this.internalCall(requestPrompt, null);
    }

    public ChatResponse internalCall(Prompt prompt, ChatResponse previousChatResponse) {
        ChatCompletionRequest request = createRequest(prompt, Boolean.FALSE);
        ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
                .prompt(prompt)
                .provider("DashScope")
                .build();
        ChatResponse response = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION
                .observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
                        this.observationRegistry)
                .observe(() -> {
                    ResponseEntity<ChatCompletion> completionEntity = RetryUtils.execute(this.retryTemplate,
                            () -> this.dashScopeApi.chatCompletionEntity(request, getAdditionalHttpHeaders(prompt)));
                    var chatCompletion = completionEntity.getBody();

                    //logger.info("同步Call响应: {}", ModelOptionsUtils.toJsonString(chatCompletion));
                    if (chatCompletion == null) {
                        logger.warn("No chat completion returned for prompt: {}", prompt);
                        return new ChatResponse(List.of());
                    }
                    List<Choice> choices = chatCompletion.output().choices();
                    if (choices == null) {
                        logger.warn("No choices returned for prompt: {}", prompt);
                        return new ChatResponse(List.of());
                    }
                    List<Generation> generations = choices.stream().map(choice -> {
                    	
                        Map<String, Object> metadata = Map.of(
                                "id", chatCompletion.requestId(),
                                "role",choice.message().getRole() != null ? choice.message().getRole().name() : "",
                                "finishReason",choice.finishReason());
                        return buildGeneration(choice, metadata, request);
                    }).toList();

                    //RateLimit rateLimit = OpenAiResponseHeaderExtractor.extractAiResponseHeaders(completionEntity);

                    // Current usage
                    DashScopeApi.Usage usage = chatCompletion.usage();
                    Usage currentChatResponseUsage = usage != null ? getDefaultUsage(usage) : new EmptyUsage();
                    Usage accumulatedUsage = UsageCalculator.getCumulativeUsage(currentChatResponseUsage,
                            previousChatResponse);
                    ChatResponse chatResponse = new ChatResponse(generations,
                            from(chatCompletion, null, accumulatedUsage));
                    observationContext.setResponse(chatResponse);
                    return chatResponse;
                });

        if (this.toolExecutionEligibilityPredicate.isToolExecutionRequired(prompt.getOptions(),response)) {
            var toolExecutionResult = this.toolCallingManager.executeToolCalls(prompt, response);
            if (toolExecutionResult.returnDirect()) {
                return ChatResponse.builder()
                        .from(response)
                        .generations(ToolExecutionResult.buildGenerations(toolExecutionResult))
                        .build();
            } else {
                return this.internalCall(new Prompt(toolExecutionResult.conversationHistory(),prompt.getOptions()), response);
            }
        }
        return response;
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        Prompt requestPrompt = buildRequestPrompt(prompt);
        return internalStream(requestPrompt, null);
    }

    public Flux<ChatResponse> internalStream(Prompt prompt, ChatResponse previousChatResponse) {
        return Flux.deferContextual(contentView -> {
            ChatCompletionRequest request = createRequest(prompt, true);
            request.parameters().setIncrementalOutput(Boolean.TRUE);
            Flux<DashScopeApi.ChatCompletion> completionChunks = this.dashScopeApi.chatCompletionStream(request,
                    getAdditionalHttpHeaders(prompt));

            ConcurrentHashMap<String, String> roleMap = new ConcurrentHashMap<>();

            final ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
                    .prompt(prompt)
                    .provider("DashScope")
                    .build();

            Observation observation = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION.observation(
                    this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
                    this.observationRegistry);

            observation.parentObservation(contentView.getOrDefault(ObservationThreadLocalAccessor.KEY,null)).start();
            Map<Integer,ToolCall> toolCallMap = new HashMap<>();
            Flux<ChatResponse> chatResponse = completionChunks.switchMap(chatCompletion -> Mono.just(chatCompletion).map(chatCompletion2 -> {
                logger.info("Flux响应内容：{}",ModelOptionsUtils.toJsonString(chatCompletion2));
                try {
                    String id = chatCompletion2.requestId();
                    List<Generation> generations = chatCompletion2.output().choices().stream().map(choice -> {
                    	if(choice.message().getToolCalls() != null) {
                    		for(ToolCall toolCalll : choice.message().getToolCalls()) {
                        		toolCallMap.compute(toolCalll.index(), (key,existing) -> {
                        			if(existing == null) {
                        				return new ToolCall(toolCalll.index(),toolCalll.type(),toolCalll.id(),toolCalll.function());
                        			}else {
                                        if(StringUtils.hasText(toolCalll.function().getArguments())){
                                            existing.function().setArguments(existing.function().getArguments() + toolCalll.function().getArguments());
                                        }
                        				return existing;
                        			}
                        		});
                        	}
                    	}
                        if (choice.message().getRole() != null) {
                            roleMap.putIfAbsent(id, choice.message().getRole().name());
                        }
                        Map<String, Object> metadata = Map.of(
                                "id",chatCompletion2.requestId(),
                                "finishReason",choice.finishReason() != null ? choice.finishReason() : "");
                        if(choice.finishReason().equals("tool_calls")) {
                        	List<ToolCall> accumulatedToolCalls = toolCallMap.values().stream().toList();
                        	choice.message().setToolCalls(accumulatedToolCalls);
                        	return buildGeneration(choice, metadata, request);
                        }else {
                        	choice.message().setToolCalls(null);
                        	return buildGeneration(choice,metadata,request);
                        }
                        
                    }).toList();
                    DashScopeApi.Usage usage = chatCompletion2.usage();
                    Usage currentChatResponseUsage = usage != null ? getDefaultUsage(usage) : new EmptyUsage();
                    Usage accumulatedUsage = UsageCalculator.getCumulativeUsage(currentChatResponseUsage,
                            previousChatResponse);
                    return new ChatResponse(generations, from(chatCompletion2, null, accumulatedUsage));
                } catch (Exception e) {
                    logger.error("Error processing chat completion", e);
                    return new ChatResponse(List.of());
                }
            })).buffer(2, 1).map(bufferList -> {
                ChatResponse firstResponse = bufferList.get(0);
                if(bufferList.size() == 2) {
                	ChatResponse secondResponse = bufferList.get(1);
                	if(secondResponse != null && secondResponse.getMetadata() != null) {
                		Usage usage = secondResponse.getMetadata().getUsage();
                		if(!UsageCalculator.isEmpty(usage)) {
                			return new ChatResponse(firstResponse.getResults(),from(firstResponse.getMetadata(),usage));
                		}
                	}
                }
                return firstResponse;
            });
            Flux<ChatResponse> flux = chatResponse.flatMap(response -> {
                        if (ToolCallingChatOptions.isInternalToolExecutionEnabled(prompt.getOptions())
                                && response.hasToolCalls()) {
                        	logger.info("查看工具参数:{}",ModelOptionsUtils.toJsonStringPrettyPrinter(response.getResult().getOutput().getToolCalls()));
                        	if(response.hasFinishReasons(Set.of("tool_calls"))) {
                        		//response.get
                        		return Flux.defer(() -> {
                        			logger.info("开始调用函数工具,{}",ModelOptionsUtils.toJsonStringPrettyPrinter(response));
                                    var toolExecutionResult = this.toolCallingManager.executeToolCalls(prompt, response);
                                    if (toolExecutionResult.returnDirect()) {
                                        return Flux.just(ChatResponse.builder().from(response)
                                                .generations(ToolExecutionResult.buildGenerations(toolExecutionResult)).build());
                                    } else {
                                        return this.internalStream(new Prompt(toolExecutionResult.conversationHistory(),prompt.getOptions()), response);
                                    }
                                }).subscribeOn(Schedulers.boundedElastic());
                        	}else {
                        		return Flux.just(response);
                        	}
                        } else {
                            return Flux.just(response);
                        }
                    }).doOnError(observation::error).doFinally(s -> observation.stop())
                    .contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, observation));
            return new MessageAggregator().aggregate(flux, observationContext::setResponse);
        });
    }

    private HttpHeaders getAdditionalHttpHeaders(Prompt prompt) {
        Map<String, String> headers = new HashMap<>(this.defaultOptions.getHttpHeaders());
        if (prompt.getOptions() != null && prompt.getOptions() instanceof DashScopeChatOptions chatOptions) {
            headers.putAll(chatOptions.getHttpHeaders());
        }
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach((key,value) -> httpHeaders.set(key, value));
        return httpHeaders;
    }

    private Generation buildGeneration(Choice choice, Map<String, Object> metadata, ChatCompletionRequest chatRequest) {
        // List<AssistantMessage.ToolCall> toolCalls = choice.message();
        List<AssistantMessage.ToolCall> toolCalls = choice.message().getToolCalls() == null ? List.of()
                : choice.message()
                .getToolCalls()
                .stream()
                .map(toolCall -> new AssistantMessage.ToolCall(toolCall.id(),"function",toolCall.function().getName(),toolCall.function().getArguments())).toList();
        String finishReason = (choice.finishReason() != null ? choice.finishReason() : "");
        var generationMetadataBuilder = ChatGenerationMetadata.builder().finishReason(finishReason);
        List<Media> media = new ArrayList<>();
        String textContent;
        if(choice.message().getContent() instanceof List<?> mediaContentList) {

        	textContent = mediaContentList.stream().map(item -> ((MediaContent)item).getText()).collect(Collectors.joining("\n"));
        }else{
        	textContent = choice.message().getContent().toString();
        }
        var assistantMessage = AssistantMessage.builder().content(textContent).properties(metadata).toolCalls(toolCalls).media(media).build();
        return new Generation(assistantMessage,generationMetadataBuilder.build());
    }

    private ChatResponseMetadata from(DashScopeApi.ChatCompletion result, RateLimit rateLimit, Usage usage) {
        Assert.notNull(result, "DashScope ChatCompletionResult must not be null");
        var builder = ChatResponseMetadata.builder().id(result.requestId() != null ? result.requestId() : "")
                .usage(usage).model("");
        if (rateLimit != null) {
            builder.rateLimit(rateLimit);
        }
        return builder.build();
    }
    
    private ChatResponseMetadata from(ChatResponseMetadata chatCompletionMessages,Usage usage) {
    	Assert.notNull(chatCompletionMessages, "DashScope ChatResponseMetadata must not be null");
    	var builder = ChatResponseMetadata.builder().id(chatCompletionMessages.getId() != null ? chatCompletionMessages.getId() : "")
    			.usage(usage).model(chatCompletionMessages.getModel() != null ? chatCompletionMessages.getModel() : "");
    	if(chatCompletionMessages.getRateLimit() != null) {
    		builder.rateLimit(chatCompletionMessages.getRateLimit());
    	}
    	return builder.build();
    }

    // TODO
    private ChatCompletion toChatCompletion() {
        return null;
    }

    private DefaultUsage getDefaultUsage(DashScopeApi.Usage usage) {
        return new DefaultUsage(usage.inputTokens(), usage.outputTokens(), usage.totalTokens());
    }

    Prompt buildRequestPrompt(Prompt prompt) {
        DashScopeChatOptions runtimeOptions = null;
        if (prompt.getOptions() != null) {
            if (prompt.getOptions() instanceof ToolCallingChatOptions toolCallingChatOptions) {
                runtimeOptions = ModelOptionsUtils.copyToTarget(toolCallingChatOptions, ToolCallingChatOptions.class,
                        DashScopeChatOptions.class);
            }
        }

        DashScopeChatOptions requestOptions = ModelOptionsUtils.merge(runtimeOptions, this.defaultOptions,
                DashScopeChatOptions.class);
        if (runtimeOptions != null) {
            requestOptions.setHttpHeaders(
                    mergeHttpHeader(runtimeOptions.getHttpHeaders(), this.defaultOptions.getHttpHeaders()));
            requestOptions.setToolNames(ToolCallingChatOptions.mergeToolNames(runtimeOptions.getToolNames(),
                    this.defaultOptions.getToolNames()));
            requestOptions.setToolCallbacks(ToolCallingChatOptions.mergeToolCallbacks(runtimeOptions.getToolCallbacks(),
                    this.defaultOptions.getToolCallbacks()));
            requestOptions.setToolContext(ToolCallingChatOptions.mergeToolContext(runtimeOptions.getToolContext(),
                    this.defaultOptions.getToolContext()));
        } else {
            requestOptions.setHttpHeaders(this.defaultOptions.getHttpHeaders());
            requestOptions.setInternalToolExecutionEnabled(this.defaultOptions.getInternalToolExecutionEnabled());
            requestOptions.setToolNames(this.defaultOptions.getToolNames());
            requestOptions.setToolCallbacks(this.defaultOptions.getToolCallbacks());

        }
        ToolCallingChatOptions.validateToolCallbacks(requestOptions.getToolCallbacks());
        return new Prompt(prompt.getInstructions(), requestOptions);
    }

    private Map<String, String> mergeHttpHeader(Map<String, String> runtimeHttpHeaders,
                                                Map<String, String> defaultHttpHeaders) {
        var mergedHttpHeader = new HashMap<>(defaultHttpHeaders);
        mergedHttpHeader.putAll(runtimeHttpHeaders);
        return mergedHttpHeader;
    }

    ChatCompletionRequest createRequest(Prompt prompt, Boolean stream) {
        List<ChatCompletionMessage> chatCompletionMessages = prompt.getInstructions().stream().map(message -> {
            if (message.getMessageType() == MessageType.USER || message.getMessageType() == MessageType.SYSTEM) {
                Object content = message.getText();
                if (message instanceof UserMessage userMessage) {
                    if (!CollectionUtils.isEmpty(userMessage.getMedia())) {
                        List<MediaContent> contentList = new ArrayList<>(List.of(new MediaContent(message.getText())));
                        contentList.addAll(userMessage.getMedia().stream().map(this::mapToMediaContent).toList());
                        content = contentList;
                    }
                }
                return List.of(new ChatCompletionMessage(content,
                        ChatCompletionMessage.Role.valueOf(message.getMessageType().name())));
            } else if (message.getMessageType() == MessageType.ASSISTANT) {
                var assistantMessage = (AssistantMessage) message;
                List<ToolCall> toolCalls = null;
                if (!CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {
                    toolCalls = assistantMessage.getToolCalls().stream().map(toolCall -> {
                        var function = new ChatCompletionFunction(toolCall.name(), toolCall.arguments());
                        return new ToolCall(toolCall.id(), toolCall.type(), function);
                    }).toList();
                }
                // TODO
                return List.of(new ChatCompletionMessage(message.getText(), Role.ASSISTANT,toolCalls));
            } else if (message.getMessageType() == MessageType.TOOL) {
                ToolResponseMessage toolMessage = (ToolResponseMessage) message;
                toolMessage.getResponses().forEach(
                        response -> Assert.isTrue(response.id() != null, "ToolResponseMessage must have an id"));
                return toolMessage.getResponses().stream().map(tr -> new ChatCompletionMessage(tr.responseData(), Role.TOOL,tr.id())).toList();
            } else {
                throw new IllegalArgumentException("Unsupported message type: " + message.getMessageType());
            }
        }).flatMap(List::stream).toList();

        ChatCompletionRequest request = new ChatCompletionRequest(chatCompletionMessages, stream);
        DashScopeChatOptions requestOptions = (DashScopeChatOptions) prompt.getOptions();
        request = ModelOptionsUtils.merge(requestOptions, request, ChatCompletionRequest.class);

        List<ToolDefinition> toolDefinitions = this.toolCallingManager.resolveToolDefinitions(requestOptions);
        if (!CollectionUtils.isEmpty(toolDefinitions)) {
            request = ModelOptionsUtils.merge(
                    DashScopeChatOptions.builder().tools(this.getFunctionTools(toolDefinitions)).build(), request,
                    ChatCompletionRequest.class);
        }

        // if(request.s)
        return request;
    }

    /**
     * TODO
     *
     * @param media
     * @return
     */
    private MediaContent mapToMediaContent(Media media) {
        var mimeType = media.getMimeType();
        if (MimeTypeUtils.parseMimeType("audio/mp3").equals(mimeType)) {
            return new MediaContent(null, null, null, null, this.fromAudioData(media.getData()));
        }
        if (MimeTypeUtils.parseMimeType("audio/wav").equals(mimeType)) {
            return new MediaContent(null, null, null, null, this.fromAudioData(media.getData()));
        } else {
            return new MediaContent(null, this.fromMediaData(media.getMimeType(), media.getData()), null, null, null);
        }
    }

    private String fromAudioData(Object audioData) {
        if (audioData instanceof byte[] byets) {
            return Base64.getEncoder().encodeToString(byets);
        }
        throw new IllegalArgumentException("Unsupported audio data type: " + audioData.getClass().getSimpleName());
    }

    private String fromMediaData(MimeType mineType, Object mediaContentData) {
        if (mediaContentData instanceof byte[] byes) {
            return String.format("data:%s;base64,%s", mineType.toString(), Base64.getEncoder().encodeToString(byes));
        } else if (mediaContentData instanceof String text) {
            return text;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported media data type: " + mediaContentData.getClass().getSimpleName());
        }
    }

    private List<DashScopeApi.FunctionTool> getFunctionTools(List<ToolDefinition> toolDefinitions) {
        return toolDefinitions.stream().map(toolDefinition -> {
            var function = new DashScopeApi.FunctionTool.Function(toolDefinition.description(), toolDefinition.name(),
                    toolDefinition.inputSchema());
            return new DashScopeApi.FunctionTool(function);
        }).toList();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private DashScopeApi dashScopeApi;

        private DashScopeChatOptions defaultOptions = DashScopeChatOptions.builder()
                .model(DashScopeApi.DEFAULT_CAHT_MODEL.getName()).build();

        private ToolCallingManager toolCallingManager;

        private RetryTemplate retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;

        private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

        private ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate = new DefaultToolExecutionEligibilityPredicate();

        public Builder dashScopeApi(DashScopeApi dashScopeApi) {
            this.dashScopeApi = dashScopeApi;
            return this;
        }

        public Builder defaultOptions(DashScopeChatOptions defaultOptions) {
            this.defaultOptions = defaultOptions;
            return this;
        }

        public Builder toolCallingManager(ToolCallingManager toolCallingManager) {
            this.toolCallingManager = toolCallingManager;
            return this;
        }

        public DashScopeChatModel build() {
            if (this.toolCallingManager != null) {
                return new DashScopeChatModel(dashScopeApi, defaultOptions, toolCallingManager, retryTemplate,
                        observationRegistry,this.toolExecutionEligibilityPredicate);
            }
            return new DashScopeChatModel(dashScopeApi, defaultOptions, DEFAULT_TOOL_CALLING_MANAGER, retryTemplate,
                    observationRegistry,this.toolExecutionEligibilityPredicate);

        }
    }

}
