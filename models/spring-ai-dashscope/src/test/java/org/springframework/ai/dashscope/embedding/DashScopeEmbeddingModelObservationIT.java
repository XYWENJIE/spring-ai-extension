package org.springframework.ai.dashscope.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.embedding.observation.DefaultEmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationDocumentation.HighCardinalityKeyNames;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationDocumentation.LowCardinalityKeyNames;
import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.xywnejie.spring.ai.dashscope.DashScopeEmbeddingModel;
import org.xywnejie.spring.ai.dashscope.DashScopeEmbeddingOptions;
import org.xywnejie.spring.ai.dashscope.api.DashScopeApi;

import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;

@SpringBootTest(classes = DashScopeEmbeddingModelObservationIT.Config.class)
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY",matches = ".+")
public class DashScopeEmbeddingModelObservationIT {
	
	@Autowired
	TestObservationRegistry observationRegistry;
	
	@Autowired
	EmbeddingModel embeddingModel;
	
	@Test
	void observationForEmbeddingOperation() {
		var options = DashScopeEmbeddingOptions.builder()
				.model(DashScopeApi.DEFAULT_EMBEDDING_MODEL)
				.dimensions(1536)
				.build();
		
		EmbeddingRequest embeddingRequest = new EmbeddingRequest(List.of("Here comes the sun"), options);
		
		EmbeddingResponse embeddingResponse = this.embeddingModel.call(embeddingRequest);
		assertThat(embeddingResponse.getResults()).isNotEmpty();
		
		EmbeddingResponseMetadata responseMetadata = embeddingResponse.getMetadata();
		
		TestObservationRegistryAssert.assertThat(this.observationRegistry)
			.doesNotHaveAnyRemainingCurrentObservation()
			.hasObservationWithNameEqualTo(DefaultEmbeddingModelObservationConvention.DEFAULT_NAME)
			.that()
			.hasContextualNameEqualTo("embedding "+ DashScopeApi.DEFAULT_EMBEDDING_MODEL)
			.hasLowCardinalityKeyValue(LowCardinalityKeyNames.AI_OPERATION_TYPE.asString(),AiOperationType.EMBEDDING.value())
			.hasLowCardinalityKeyValue(LowCardinalityKeyNames.AI_PROVIDER.asString(),"DASHSCOPE")
			.hasLowCardinalityKeyValue(LowCardinalityKeyNames.REQUEST_MODEL.asString(),DashScopeApi.DEFAULT_EMBEDDING_MODEL)
			.hasLowCardinalityKeyValue(LowCardinalityKeyNames.RESPONSE_MODEL.asString(),responseMetadata.getModel())
			.hasHighCardinalityKeyValue(HighCardinalityKeyNames.REQUEST_EMBEDDING_DIMENSIONS.asString(),"1536")
			.hasHighCardinalityKeyValue(HighCardinalityKeyNames.USAGE_INPUT_TOKENS.asString(),
					String.valueOf(responseMetadata.getUsage().getPromptTokens()))
			.hasHighCardinalityKeyValue(HighCardinalityKeyNames.USAGE_TOTAL_TOKENS.asString(),
					String.valueOf(responseMetadata.getUsage().getTotalTokens()))
			.hasBeenStarted()
			.hasBeenStopped();
	}
	
	@SpringBootConfiguration
	static class Config {
		
		@Bean
		public TestObservationRegistry observationRegistry() {
			return TestObservationRegistry.create();
		}
		
		@Bean
		public DashScopeApi dashScopeApi() {
			return DashScopeApi.builder().apiKey(System.getenv("DASHSCOPE_API_KEY")).build();
		}
		
		@Bean
		public DashScopeEmbeddingModel dashScopeEmbeddingModel(DashScopeApi dashScopeApi,
				TestObservationRegistry observationRegistry) {
			return new DashScopeEmbeddingModel(dashScopeApi,MetadataMode.EMBED,DashScopeEmbeddingOptions.builder().build(),
					new RetryTemplate(RetryPolicy.withDefaults()),observationRegistry);
		}
	}

}
