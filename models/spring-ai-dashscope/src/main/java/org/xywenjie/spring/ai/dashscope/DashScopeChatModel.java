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

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeDefinition;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeRequest;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeResponse;
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

    private static final ChatModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultChatModelObservationConvention();

    private static final ToolCallingManager DEFAULT_TOOL_CALLING_MANAGER = ToolCallingManager.builder().build();

    private final DashScopeChatOptions defaultOptions;

    private final RetryTemplate retryTemplate;

    private final DashScopeApi dashScopeApi;

    private final ObservationRegistry observationRegistry;

    private final ToolCallingManager toolCallingManager;

    private final ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate;

    private final ChatModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

    public DashScopeChatModel(DashScopeApi dashScopeApi,DashScopeChatOptions defaultOptions,ToolCallingManager toolCallingManager,
                              RetryTemplate retryTemplate,ObservationRegistry observationRegistry){
        this(dashScopeApi,defaultOptions,toolCallingManager,retryTemplate,observationRegistry,new DefaultToolExecutionEligibilityPredicate());
    }


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
        DashScopeRequest request = createRequest(prompt, Boolean.FALSE);
        ChatModelObservationContext observationContext = ChatModelObservationContext.builder()
                .prompt(prompt)
                .provider("DashScope")
                .build();
        ChatResponse response = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION
                .observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext,
                        this.observationRegistry)
                .observe(() -> {
                    ResponseEntity<DashScopeResponse> completionEntity = RetryUtils.execute(this.retryTemplate,
                            () -> this.dashScopeApi.chatCompletionEntity(request, getAdditionalHttpHeaders(prompt)));
                    var chatCompletion = completionEntity.getBody();

                    if (chatCompletion == null) {
                        logger.warn("No chat completion returned for prompt: {}", prompt);
                        return new ChatResponse(List.of());
                    }
                    List<DashScopeResponse.Choice> choices = chatCompletion.getOutput().getChoices();
                    if (choices == null) {
                        logger.warn("No choices returned for prompt: {}", prompt);
                        return new ChatResponse(List.of());
                    }
                    List<Generation> generations = choices.stream().map(choice -> {
                    	
                        Map<String, Object> metadata = Map.of(
                                "id", chatCompletion.getRequestId(),
                                "role",choice.getMessage().getRole() != null ? choice.getMessage().getRole() : "",
                                "finishReason",choice.getFinishReason());
                        return buildGeneration(choice, metadata, request);
                    }).toList();

                    //RateLimit rateLimit = OpenAiResponseHeaderExtractor.extractAiResponseHeaders(completionEntity);

                    // Current usage
                    DashScopeResponse.Usage usage = chatCompletion.getUsage();
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
            DashScopeRequest request = createRequest(prompt, true);
            request.getParameters().setIncrementalOutput(Boolean.TRUE);
            Flux<DashScopeResponse> completionChunks = this.dashScopeApi.chatCompletionStream(request,
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
            Map<Integer,DashScopeResponse.ToolCall> toolCallMap = new HashMap<>();
            Flux<ChatResponse> chatResponse = completionChunks.switchMap(chatCompletion -> Mono.just(chatCompletion).map(chatCompletion2 -> {
                //logger.info("Flux响应内容：{}",ModelOptionsUtils.toJsonString(chatCompletion2));
                try {
                    String id = chatCompletion2.getRequestId();
                    List<Generation> generations = chatCompletion2.getOutput().getChoices().stream().map(choice -> {
                    	if(choice.getMessage().getToolCalls() != null) {
                    		for(DashScopeResponse.ToolCall toolCalll : choice.getMessage().getToolCalls()) {
                        		toolCallMap.compute(toolCalll.getIndex(), (key,existing) -> {
                        			if(existing == null) {
                        				return new DashScopeResponse.ToolCall(toolCalll.getIndex(),toolCalll.getType(),toolCalll.getId(),toolCalll.getFunction());
                        			}else {
                                        if(StringUtils.hasText(toolCalll.getFunction().getArguments())){
                                            existing.getFunction().setArguments(existing.getFunction().getArguments() + toolCalll.getFunction().getArguments());
                                        }
                        				return existing;
                        			}
                        		});
                        	}
                    	}
                        if (choice.getMessage().getRole() != null) {
                            roleMap.putIfAbsent(id, choice.getMessage().getRole());
                        }
                        Map<String, Object> metadata = Map.of(
                                "id",chatCompletion2.getRequestId(),
                                "finishReason",choice.getFinishReason() != null ? choice.getFinishReason() : "");
                        if(choice.getFinishReason().equals("tool_calls")) {
                        	List<DashScopeResponse.ToolCall> accumulatedToolCalls = toolCallMap.values().stream().toList();
                        	choice.getMessage().setToolCalls(accumulatedToolCalls);
                        	return buildGeneration(choice, metadata, request);
                        }else {
                        	choice.getMessage().setToolCalls(null);
                        	return buildGeneration(choice,metadata,request);
                        }
                        
                    }).toList();
                    DashScopeResponse.Usage usage = chatCompletion2.getUsage();
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

    private Generation buildGeneration(DashScopeResponse.Choice choice, Map<String, Object> metadata, DashScopeRequest chatRequest) {
        // List<AssistantMessage.ToolCall> toolCalls = choice.message();
        List<AssistantMessage.ToolCall> toolCalls = choice.getMessage().getToolCalls() == null ? List.of()
                : choice.getMessage()
                .getToolCalls()
                .stream()
                .map(toolCall -> new AssistantMessage.ToolCall(toolCall.getId(),"function",toolCall.getFunction().getName(),toolCall.getFunction().getArguments())).toList();
        String finishReason = (choice.getFinishReason() != null ? choice.getFinishReason() : "");
        var generationMetadataBuilder = ChatGenerationMetadata.builder().finishReason(finishReason);
        List<Media> media = new ArrayList<>();
        String textContent;
        if(choice.getMessage().getContent() instanceof List<?> mediaContentList) {

        	textContent = mediaContentList.stream().map(item -> ((DashScopeResponse.MediaContent)item).getText()).collect(Collectors.joining("\n"));
        }else{
        	textContent = choice.getMessage().getContent().toString();
        }
        var assistantMessage = AssistantMessage.builder().content(textContent).properties(metadata).toolCalls(toolCalls).media(media).build();
        return new Generation(assistantMessage,generationMetadataBuilder.build());
    }

    private ChatResponseMetadata from(DashScopeResponse result, RateLimit rateLimit, Usage usage) {
        Assert.notNull(result, "DashScope ChatCompletionResult must not be null");
        var builder = ChatResponseMetadata.builder().id(result.getRequestId() != null ? result.getRequestId() : "")
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
//    private ChatCompletion toChatCompletion() {
//        return null;
//    }

    private DefaultUsage getDefaultUsage(DashScopeResponse.Usage usage) {
        return new DefaultUsage(usage.getInputTokens(), usage.getOutputTokens(), usage.getTotalTokens());
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

    DashScopeRequest createRequest(Prompt prompt, Boolean stream) {
        List<DashScopeRequest.Message> chatCompletionMessages = prompt.getInstructions().stream().map(message -> {
            if (message.getMessageType() == MessageType.USER || message.getMessageType() == MessageType.SYSTEM) {
                Object content = message.getText();
                if (message instanceof UserMessage userMessage) {
                    if (!CollectionUtils.isEmpty(userMessage.getMedia())) {
                        List<DashScopeRequest.MediaContent> contentList = new ArrayList<>(List.of(new DashScopeRequest.MediaContent(message.getText())));
                        contentList.addAll(userMessage.getMedia().stream().map(this::mapToMediaContent).toList());
                        content = contentList;
                    }
                }
                return List.of(DashScopeRequest.Message.builder().content(content).role(DashScopeDefinition.Role.valueOf(message.getMessageType().name())).build());
            } else if (message.getMessageType() == MessageType.ASSISTANT) {
                var assistantMessage = (AssistantMessage) message;
                List<DashScopeResponse.ToolCall> toolCalls = null;
                if (!CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {
                    toolCalls = assistantMessage.getToolCalls().stream().map(toolCall -> {
                        var function = new DashScopeResponse.ChatFunction(toolCall.name(), toolCall.arguments());
                        return new DashScopeResponse.ToolCall(toolCall.id(), toolCall.type(), function);
                    }).toList();
                }
                // 返回包含内容的消息，支持字符串或媒体内容列表
                Object content = message.getText();
                if (message instanceof AssistantMessage assistantMsg && !CollectionUtils.isEmpty(assistantMsg.getMedia())) {
                    // 如果有媒体内容，构造媒体内容列表
                    List<DashScopeRequest.MediaContent> contentList = new ArrayList<>();
                    if (message.getText() != null) {
                        contentList.add(new DashScopeRequest.MediaContent(message.getText()));
                    }
                    contentList.addAll(assistantMsg.getMedia().stream()
                            .map(this::mapToMediaContent)
                            .filter(java.util.Objects::nonNull)
                            .toList());
                    content = contentList;
                }
                return List.of(DashScopeRequest.Message.builder().content(content).role(DashScopeDefinition.Role.ASSISTANT).build());
            } else if (message.getMessageType() == MessageType.TOOL) {
                ToolResponseMessage toolMessage = (ToolResponseMessage) message;
                toolMessage.getResponses().forEach(
                        response -> Assert.isTrue(response.id() != null, "ToolResponseMessage must have an id"));
                return toolMessage.getResponses().stream().map(tr -> new DashScopeRequest.Message(tr.responseData(), DashScopeDefinition.Role.TOOL,tr.id())).toList();
            } else {
                throw new IllegalArgumentException("Unsupported message type: " + message.getMessageType());
            }
        }).flatMap(List::stream).toList();

        //DashScopeRequest request = new DashScopeRequest(chatCompletionMessages, stream);
        DashScopeRequest request = new DashScopeRequest();
        DashScopeChatOptions requestOptions = (DashScopeChatOptions) prompt.getOptions();
        request = ModelOptionsUtils.merge(requestOptions, request, DashScopeRequest.class);

        List<ToolDefinition> toolDefinitions = this.toolCallingManager.resolveToolDefinitions(requestOptions);
        if (!CollectionUtils.isEmpty(toolDefinitions)) {
            request = ModelOptionsUtils.merge(
                    DashScopeChatOptions.builder().tools(this.getFunctionTools(toolDefinitions)).build(), request,
                    DashScopeRequest.class);
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
    private DashScopeRequest.MediaContent mapToMediaContent(Media media) {
        var mimeType = media.getMimeType();
        if (MimeTypeUtils.parseMimeType("audio/mp3").equals(mimeType)) {
            return DashScopeRequest.MediaContent.builder().audio(this.fromAudioData(media.getData())).build();
        }
        if (MimeTypeUtils.parseMimeType("audio/wav").equals(mimeType)) {
            return DashScopeRequest.MediaContent.builder().audio(this.fromAudioData(media.getData())).build();
        } else {
            return DashScopeRequest.MediaContent.builder().image(this.fromMediaData(media.getMimeType(),media.getData())).build();
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

    private List<DashScopeRequest.FunctionTool> getFunctionTools(List<ToolDefinition> toolDefinitions) {
        return toolDefinitions.stream().map(toolDefinition -> {
            var function = new DashScopeRequest.Function(toolDefinition.description(), toolDefinition.name(),
                    toolDefinition.inputSchema());
            return new DashScopeRequest.FunctionTool(function);
        }).toList();
    }

    @Override
    public String toString() {
        return "DashScopeChatModel [defaultOptions="+this.defaultOptions+"]";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private DashScopeApi dashScopeApi;

        private DashScopeChatOptions defaultOptions = DashScopeChatOptions.builder()
                .model(DashScopeApi.DEFAULT_CHAT_MODEL.getName()).build();

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
