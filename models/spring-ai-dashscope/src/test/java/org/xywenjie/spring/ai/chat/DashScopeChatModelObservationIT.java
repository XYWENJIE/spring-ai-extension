package org.xywenjie.spring.ai.chat;

import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.xywenjie.spring.ai.dashscope.DashScopeChatModel;
import org.xywenjie.spring.ai.dashscope.DashScopeChatOptions;
import org.xywenjie.spring.ai.dashscope.api.DashScopeApi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.ai.chat.observation.ChatModelObservationDocumentation.HighCardinalityKeyNames;
import static org.springframework.ai.chat.observation.ChatModelObservationDocumentation.LowCardinalityKeyNames;

@SpringBootTest(classes = DashScopeChatModelObservationIT.Config.class)
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY",matches = ".+")
public class DashScopeChatModelObservationIT {

    @Autowired
    TestObservationRegistry observationRegistry;

    @Autowired
    DashScopeChatModel chatModel;

    @Test
    void observationForChatOperation(){
        var options = DashScopeChatOptions.builder()
                .build();

        Prompt prompt = new Prompt("Why does a raven look like a desk?",options);
        ChatResponse chatResponse = this.chatModel.call(prompt);
        assertThat(chatResponse.getResult().getOutput().getText()).isNotNull();

        ChatResponseMetadata responseMetadata = chatResponse.getMetadata();
        assertThat(responseMetadata).isNotNull();

        validate(responseMetadata);
    }

    private void validate(ChatResponseMetadata responseMetadata){
        TestObservationRegistryAssert.assertThat(this.observationRegistry)
                .doesNotHaveAnyRemainingCurrentObservation()
                .hasObservationWithNameEqualTo(DefaultChatModelObservationConvention.DEFAULT_NAME)
                .that()
                .hasLowCardinalityKeyValue(LowCardinalityKeyNames.AI_OPERATION_TYPE.asString(), AiOperationType.CHAT.value())
                .hasLowCardinalityKeyValue(LowCardinalityKeyNames.AI_PROVIDER.asString(), DashScopeApi.ChatModel.QWEN_MAX.getValue())
                .hasLowCardinalityKeyValue(LowCardinalityKeyNames.RESPONSE_MODEL.asString(), responseMetadata.getModel())
                .hasHighCardinalityKeyValue(HighCardinalityKeyNames.REQUEST_FREQUENCY_PENALTY.asString(),"0.0");
    }

    static class Config{

        @Bean
        public TestObservationRegistry observationRegistry(){
            return TestObservationRegistry.create();
        }

        @Bean
        public DashScopeApi dashScopeApi(){
            return DashScopeApi.builder().apiKey(System.getenv("DASHSCOPE_API_KEY")).build();
        }

        @Bean
        public DashScopeChatModel dashScopeChatModel(DashScopeApi dashScopeApi, TestObservationRegistry observationRegistry){
            return new DashScopeChatModel(dashScopeApi,
                    DashScopeChatOptions.builder().build(),
                    ToolCallingManager.builder().build(),
                    new RetryTemplate(RetryPolicy.withDefaults()),
                    observationRegistry);
        }
    }
}
