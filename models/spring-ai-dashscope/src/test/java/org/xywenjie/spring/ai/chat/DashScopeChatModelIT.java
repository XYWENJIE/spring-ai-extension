package org.xywenjie.spring.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.content.Media;
import org.springframework.ai.dashscope.testutils.AbstractIT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeTypeUtils;
import org.xywenjie.spring.ai.DashScopeTestConfiguration;
import org.xywenjie.spring.ai.dashscope.DashScopeChatOptions;
import reactor.core.publisher.Flux;

@SpringBootTest(classes = DashScopeTestConfiguration.class)
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY",matches = ".+")
public class DashScopeChatModelIT extends AbstractIT {

    private static final Logger logger = LoggerFactory.getLogger(DashScopeChatModelIT.class);
    
    @Value("classpath:/prompts/system-message.st")
    private Resource systemResource;

    @Test
    void roleTest() {
        UserMessage userMessage = new UserMessage(
                "Tell me about 3 famous pirates from the Golden Age of Piracy and what they did.");
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(this.systemResource);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("name","Bob","voice","pirate"));
        Prompt prompt = new Prompt(List.of(userMessage,systemMessage));
        ChatResponse chatResponse = this.chatModel.call(prompt);
        assertThat(chatResponse.getResults()).hasSize(1);
        assertThat(chatResponse.getResults().get(0).getOutput().getText()).contains("Blackbeard");
    }

    @Test
    void testMessageHistory() {
        UserMessage userMessage = new UserMessage(
                "Tell me about 3 famous pirates from the Golden Age of Piracy and why they did.");
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(this.systemResource);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("name","Bob","voice","pirate"));
        Prompt prompt = new Prompt(List.of(userMessage,systemMessage));

        ChatResponse response = this.chatModel.call(prompt);
        assertThat(response.getResult().getOutput().getText()).containsAnyOf("Blackbeard", "Bartholomew");

        var promptWithMessageHistory = new Prompt(List.of(new UserMessage("Dummy"), response.getResult().getOutput(),
                new UserMessage("Repeat the last assistant message.")));
        response = this.chatModel.call(promptWithMessageHistory);

        assertThat(response.getResult().getOutput().getText()).containsAnyOf("Blackbeard", "Bartholomew");
    }

    @Test
    void streamCompletenessTest() throws InterruptedException {
        UserMessage userMessage = new UserMessage(
                "List ALL natural numbers in range [1, 100]. Make sure to not omit any.");
        Prompt prompt = new Prompt(List.of(userMessage));
        
        StringBuilder answer = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        Flux<ChatResponse> chatResponseFlux = this.streamingChatModel.stream(prompt).doOnNext(chatResponse -> {
            String responseContent = chatResponse.getResults().get(0).getOutput().getText();
            answer.append(responseContent);
        }).doOnComplete(() -> {
            logger.info(answer.toString());
            latch.countDown();
        });
        chatResponseFlux.subscribe();
        assertThat(latch.await(120, TimeUnit.SECONDS)).isTrue();
        IntStream.rangeClosed(1, 100).forEach(n -> assertThat(answer).contains(String.valueOf(n)));
    }

    @Test
    void streamCompletenessTestWithChatResponse() throws InterruptedException {
        UserMessage userMessage = new UserMessage("Who is George Washington? - use first as 1st");
        Prompt prompt = new Prompt(List.of(userMessage));

        StringBuilder answer = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        ChatClient chatClient = ChatClient.builder(this.dashScopeChatModel).build();

        Flux<String> chatResponseFlux = chatClient.prompt(prompt).stream().content().doOnNext(answer::append)
                .doOnComplete(() -> {
                    logger.info(answer.toString());
                    latch.countDown();
                });
        chatResponseFlux.subscribe();
        assertThat(latch.await(120, TimeUnit.SECONDS)).isTrue();
        assertThat(answer).contains("1st");
    }
    
    @Test
    void ensureChatResponseAsContentDoesNotSwallowBlankSpace() throws InterruptedException {
    	UserMessage userMessage = new UserMessage("Who is George Washington? - use first as 1st");
    	Prompt prompt = new Prompt(List.of(userMessage));
    	
    	StringBuilder answer = new StringBuilder();
    	CountDownLatch latch = new CountDownLatch(1);
    	
    	ChatClient chatClient = ChatClient.builder(this.dashScopeChatModel).build();
    	
    	Flux<String> chatResponseFlux = chatClient.prompt(prompt)
    			.stream().content().doOnNext(answer::append).doOnComplete(() -> {
    				logger.info(answer.toString());
    				latch.countDown();
    			});
    	chatResponseFlux.subscribe();
    	assertThat(latch.await(120,TimeUnit.SECONDS)).isTrue();
    	assertThat(answer).contains("1st");
    }
    
    @Test
    void streamRoleTest() {
    	UserMessage userMessage = new UserMessage(
    			"Tell me about 3 famous pirates from the Golden Age of Piracy and what they did.");
    	SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(this.systemResource);
    	Message systemMessage = systemPromptTemplate.createMessage(Map.of("name","Bob","voice","pirate"));
    	Prompt prompt = new Prompt(List.of(userMessage,systemMessage));
    	Flux<ChatResponse> flux = this.streamingChatModel.stream(prompt);
    	
    	List<ChatResponse> response = flux.collectList().block();
    	assertThat(response.size()).isGreaterThan(1);
    	
    	String stitchedResponseContent = response.stream()
    			.map(ChatResponse::getResults)
    			.flatMap(List::stream).map(Generation::getOutput)
    			.map(AssistantMessage::getText)
    			.collect(Collectors.joining());
    	assertThat(stitchedResponseContent).contains("Blackbeard");
    }

    @Test
    void streamingWithTokenUsage() {
        var promptOptions = DashScopeChatOptions.builder().seed(1).build();

        var prompt = new Prompt("List two colors of the Polish flag. Be brief.", promptOptions);
        var streamingTokenUsage = this.chatModel.stream(prompt).blockLast().getMetadata().getUsage();
        var referenceTokenUsage = this.chatModel.call(prompt).getMetadata().getUsage();
        logger.info(streamingTokenUsage.toString());
        logger.info(referenceTokenUsage.toString());
        assertThat(streamingTokenUsage.getPromptTokens()).isGreaterThan(0);
        assertThat(streamingTokenUsage.getCompletionTokens()).isGreaterThan(0);
        assertThat(streamingTokenUsage.getTotalTokens()).isGreaterThan(0);

        assertThat(streamingTokenUsage.getPromptTokens()).isCloseTo(referenceTokenUsage.getPromptTokens(),
                Percentage.withPercentage(25));
        assertThat(streamingTokenUsage.getCompletionTokens()).isCloseTo(referenceTokenUsage.getCompletionTokens(),
                Percentage.withPercentage(25));
        assertThat(streamingTokenUsage.getTotalTokens()).isCloseTo(referenceTokenUsage.getTotalTokens(),
                Percentage.withPercentage(25));
    }

    @Test
    void listOutputConverter() {
//        DefaultConversionService conversionService = new DefaultConversionService();
//        ListOutputConverter outputConverter = new ListOutputConverter(conversionService);
//
//        String format = outputConverter.getFormat();
//        String template = """
//                List five {subject}
//                {format}
//                """;
//        PromptTemplate promptTemplate = new PromptTemplate(template, Map.of("subject", "ice cream flavors", "format", format));
//        Prompt prompt = new Prompt(promptTemplate.createMessage());
//        Generation generation = this.chatModel.call(prompt).getResult();
//
//        List<String> list = outputConverter.convert(generation.getOutput().getText());
//        assertThat(list).hasSize(5);
    }

    @Test
    void mapOutputConverter() {
//        MapOutputConverter outputConverter = new MapOutputConverter();
//        String format = outputConverter.getFormat();
//        String template = """
//                Provide me List of {subject}
//                {format}
//                """;
//        PromptTemplate promptTemplate = new PromptTemplate(template,
//                Map.of("subject", "numbers from 1 to 9 under they key name 'numbers'", "format", format));
//        Prompt prompt = new Prompt(promptTemplate.createMessage());
//        Generation generation = this.chatModel.call(prompt).getResult();
//
//        Map<String, Object> result = outputConverter.convert(generation.getOutput().getText());
//        assertThat(result.get("numbers")).isEqualTo(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    @Test
    void functionCallTest() {
        UserMessage userMessage = new UserMessage("What's the weather like in San Francisco, Tokyo, and Paris?");

        List<Message> messages = new ArrayList<>(List.of(userMessage));

//        var promptOptions = DashScopeChatOptions.builder()
//                .model(ChatModel.QWEN_MAX.getName())
//                .toolCallbacks(List.of(FunctionToolCallback.builder("getCurrentWeather", new MockWeatherService())
//                        .description("Get the weather in location")
//                        .inputType(MockWeatherService.Request.class)
//                        .build()))
//                .build();

//        ChatResponse response = this.chatModel.call(new Prompt(messages, promptOptions));
//        logger.info("Response: {}", response.getResult().getOutput().getText());
//        assertThat(response.getResult().getOutput().getText()).contains("30", "10", "15");
    }

    @Test
    void streamFunctionCallTest() throws InterruptedException {
        UserMessage userMessage = new UserMessage("What's the weather like in San Francisco, Tokyo, and Paris?");
        List<Message> messages = new ArrayList<>(List.of(userMessage));

//        var promptOptions = DashScopeChatOptions.builder()
//                .toolCallbacks(List.of(FunctionToolCallback.builder("getCurrentWeather", new MockWeatherService())
//                        .description("Get the weather in location")
//                        .inputType(MockWeatherService.Request.class)
//                        .build())).build();
//        StringBuilder answer = new StringBuilder();
//        CountDownLatch latch = new CountDownLatch(1);
//        Flux<ChatResponse> responseFlux = this.streamingChatModel.stream(new Prompt(messages,promptOptions));
//        responseFlux.doOnNext((chatResponse)->{
//            String responseContent = chatResponse.getResults().get(0).getOutput().getText();
//            //chatResponse.get
//            answer.append(responseContent);
//        }).doOnComplete(()->{
//            logger.info(answer.toString());
//            latch.countDown();
//        }).doOnError(error -> {
//        	logger.info("Stream processing failed");
//        	latch.countDown();
//        }).subscribe();
//        assertThat(latch.await(120, TimeUnit.SECONDS)).isTrue();
    }

    @ParameterizedTest(name = "{0} : {displayName} ")
    @ValueSource(strings = {"qwen-vl-max"})
    void multiModalityEmbeddedImage(String modelName) throws IOException {
        var imageData = new ClassPathResource("/test.png");
        var userMessage = UserMessage.builder()
                .text("Explain what do you see on this picture?")
                .media(List.of(new Media(MimeTypeUtils.IMAGE_PNG,imageData)))
                .build();

        var response = this.chatModel
                .call(new Prompt(List.of(userMessage),DashScopeChatOptions.builder().model(modelName).build()));
        logger.info(response.getResult().getOutput().getText());
        assertThat(response.getResult().getOutput().getText()).containsAnyOf("bananas","apple","bowl","basket","fruit stand");
    }

    @ParameterizedTest(name = "{0}:{displayName}")
    @ValueSource(strings = {"qwen-vl-max"})
    void multiModalityImageUrl(String modelName) throws IOException {
        var userMessage = UserMessage.builder()
                .text("Explain what do you see on this picture?")
                .media(List.of(Media.builder()
                        .mimeType(MimeTypeUtils.IMAGE_PNG)
                        .data(URI.create("https://docs.spring.io/spring-ai/reference/_images/multimodal.test.png"))
                        .build()))
                .build();

        ChatResponse response = this.chatModel
                .call(new Prompt(List.of(userMessage),DashScopeChatOptions.builder().model(modelName).build()));

        logger.info(response.getResult().getOutput().getText());
        assertThat(response.getResult().getOutput().getText()).containsAnyOf("bananas","apple", "bowl", "basket",
                "fruit stand");
    }

    @Test
    void streamingMultiModalityImageUrl() throws IOException {
        var userMessage = UserMessage.builder()
                .text("Explain what do you see on this picture?")
                .media(List.of(Media.builder()
                        .mimeType(MimeTypeUtils.IMAGE_PNG)
                        .data(URI.create("https://docs.spring.io/spring-ai/reference/_images/multimodal.test.png"))
                        .build()))
                .build();

        Flux<ChatResponse> response = this.streamingChatModel.stream(new Prompt(List.of(userMessage),
                DashScopeChatOptions.builder().model("qwen-vl-max").build()));

        String content = response.collectList()
                .block()
                .stream()
                .map(ChatResponse::getResults)
                .flatMap(List::stream)
                .map(Generation::getOutput)
                .map(AssistantMessage::getText)
                .collect(Collectors.joining());
        logger.info("Response: {}",content);
        assertThat(content).containsAnyOf("bananas", "apple", "bowl", "basket", "fruit stand");
    }

    @ParameterizedTest(name = "{0}:{displayName}")
    @ValueSource(strings = {"qwen-tts"})
    void multiModalityOutputAudio(String modelName) throws IOException {
        var userMessage = new UserMessage("Tell me joke about Spring Framework");

        ChatResponse response = this.chatModel.call(new Prompt(List.of(userMessage),
                DashScopeChatOptions.builder()
                        .model(modelName)
                        .build()));

//        logger.info(response.getResult().getOutput().getText());
//        assertThat(response.getResult().getOutput().getText()).isNotEmpty();

        byte[] audio = response.getResult().getOutput().getMedia().get(0).getDataAsByteArray();
        assertThat(audio).isNotEmpty();
    }

}
