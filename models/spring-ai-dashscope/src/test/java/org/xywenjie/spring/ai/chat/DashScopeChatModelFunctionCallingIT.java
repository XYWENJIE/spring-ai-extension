package org.xywenjie.spring.ai.chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.dashscope.api.tool.MockWeatherService;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.xywenjie.spring.ai.dashscope.DashScopeChatModel;
import org.xywenjie.spring.ai.dashscope.DashScopeChatOptions;
import org.xywenjie.spring.ai.dashscope.api.DashScopeApi;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DashScopeChatModelFunctionCallingIT.Config.class)
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY",matches = ".+")
public class DashScopeChatModelFunctionCallingIT {

    private static final Logger logger = LoggerFactory.getLogger(DashScopeChatModelFunctionCallingIT.class);

    @Autowired
    ChatModel chatModel;

    @Test
    void functionCallSupplier(){
        Map<String,Object> state = new ConcurrentHashMap<>();

        String response = ChatClient.create(this.chatModel).prompt()
                .user("Turn the light on in the living room")
                .toolCallbacks(FunctionToolCallback.builder("turnsLightOnInTheLivingRoom",() -> state.put("Light","ON")).build())
                .call().content();

        logger.info("Response: {}",response);
        assertThat(state).containsEntry("Light","ON");
    }

    @Test
    void functionCallTest(){
        functionCallTest(DashScopeChatOptions.builder().model("qwen3-max").toolCallbacks(List.of(FunctionToolCallback.builder("getCurrentWeather",new MockWeatherService())
                .description("Get the weather in location")
                .inputType(MockWeatherService.Request.class)
                .build()))
                .build());
    }



    @Test
    void functionCallWithToolContextTest(){
        var biFunction = new BiFunction<MockWeatherService.Request, ToolContext,MockWeatherService.Response>(){

            @Override
            public MockWeatherService.Response apply(MockWeatherService.Request request, ToolContext toolContext) {
                assertThat(toolContext.getContext()).containsEntry("sessionId","123");

                double temperature = 0;
                if(request.location().contains("Paris")){
                    temperature = 15;
                }else if(request.location().contains("Tokyo")){
                    temperature = 10;
                }else if(request.location().contains("San Francisco")){
                    temperature = 30;
                }
                return new MockWeatherService.Response(temperature,15,20,2,53,45,MockWeatherService.Unit.C);
            }
        };

        functionCallTest(DashScopeChatOptions.builder()
                .model("qwen3-max")
                .toolCallbacks(List.of(FunctionToolCallback.builder("getCurrentWeather",biFunction)
                        .description("Get the weather in location").
                        inputType(MockWeatherService.Request.class)
                        .build()))
                .toolContext(Map.of("sessionId","123"))
                .build());
    }

    void functionCallTest(DashScopeChatOptions promptOptions){
        UserMessage userMessage = new UserMessage("What's the weather like in San Francisco, Tokyo, and Paris?");
        List<Message> messages = new ArrayList<>(List.of(userMessage));

        ChatResponse response = this.chatModel.call(new Prompt(messages,promptOptions));

        logger.info("Response:{}",response);
        assertThat(response.getResult().getOutput().getText()).contains("30","10","15");
    }

    @Test
    void streamFunctionCallWithToolContextTest(){
        var biFunction = new BiFunction<MockWeatherService.Request,ToolContext,MockWeatherService.Response>(){

            @Override
            public MockWeatherService.Response apply(MockWeatherService.Request request, ToolContext toolContext) {

                assertThat(toolContext.getContext()).containsEntry("sessionId", "123");

                double temperature = 0;
                if (request.location().contains("Paris")) {
                    temperature = 15;
                }
                else if (request.location().contains("Tokyo")) {
                    temperature = 10;
                }
                else if (request.location().contains("San Francisco")) {
                    temperature = 30;
                }

                return new MockWeatherService.Response(temperature, 15, 20, 2, 53, 45, MockWeatherService.Unit.C);
            }
        };

        DashScopeChatOptions promptOptions = DashScopeChatOptions.builder()
                .toolCallbacks(List.of((FunctionToolCallback.builder("getCurrentWeather",biFunction)
                        .description("Get the weather in location")
                        .inputType(MockWeatherService.Request.class)
                        .build())))
                .toolContext(Map.of("sessionId","123"))
                .build();

        streamFunctionCallTest(promptOptions);
    }

    void streamFunctionCallTest(DashScopeChatOptions promptOptions){
        UserMessage userMessage = new UserMessage("What's the weather like in San Francisco, Tokyo, and Paris?");
        List<Message> messages = new ArrayList<>(List.of(userMessage));
        Flux<ChatResponse> response = this.chatModel.stream(new Prompt(messages,promptOptions));

        String content = response.collectList()
                .block()
                .stream()
                .map(ChatResponse::getResults)
                .flatMap(List::stream)
                .map(Generation::getOutput)
                .map(AssistantMessage::getText)
                .collect(Collectors.joining());
        logger.info("Response:{}",content);

        assertThat(content).contains("30","10","15");
    }

    @SpringBootConfiguration
    static class Config {

        @Bean
        public DashScopeApi chatCompletionApi(){
            return DashScopeApi.builder().apiKey(System.getenv("DASHSCOPE_API_KEY")).build();
        }

        @Bean
        public DashScopeChatModel dashScopeClient(DashScopeApi dashScopeApi){
            return DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
        }
    }
}
