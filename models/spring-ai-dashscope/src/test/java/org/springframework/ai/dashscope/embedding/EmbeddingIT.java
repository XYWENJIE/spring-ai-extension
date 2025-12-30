package org.springframework.ai.dashscope.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.dashscope.DashScopeEmbeddingModel;
import org.springframework.ai.dashscope.DashScopeEmbeddingOptions;
import org.springframework.ai.dashscope.DashScopeTestConfiguration;
import org.springframework.ai.dashscope.api.DashScopeApi;
import org.springframework.ai.dashscope.testutils.AbstractIT;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

@SpringBootTest(classes = DashScopeTestConfiguration.class)
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY",matches = ".+")
public class EmbeddingIT extends AbstractIT{
	
	private Resource resource = new DefaultResourceLoader().getResource("classpath:text_source.txt");
	
	@Autowired
	private DashScopeEmbeddingModel embeddingModel;
	
	@Test
	void defaultEmbedding() {
		assertThat(this.embeddingModel).isNotNull();
		
		EmbeddingResponse embeddingResponse = this.embeddingModel.embedForResponse(List.of("Hello Word"));
		assertThat(embeddingResponse.getResults()).hasSize(1);
		assertThat(embeddingResponse.getResults().get(0)).isNotNull();
		assertThat(embeddingResponse.getResults().get(0).getOutput()).hasSize(1024);
		assertThat(embeddingResponse.getMetadata().getModel()).isEqualTo("text-embedding-v4");
	}
	
	@Test
	void embeddingBatchDocuments() throws Exception {
		assertThat(this.embeddingModel).isNotNull();
		List<float[]> embeddings = this.embeddingModel.embed(List.of(new Document("Hello word"),new Document("Hello Spring"),new Document("Hello Spring AI!")),
				DashScopeEmbeddingOptions.builder().model(DashScopeApi.DEFAULT_EMBEDDING_MODEL).build(),
				new TokenCountBatchingStrategy());
		assertThat(embeddings.size()).isEqualTo(3);
		embeddings.forEach(embedding -> assertThat(embedding.length).isEqualTo(this.embeddingModel.dimensions()));
	}
	
	@Test
	void embeddingBatchDocumentsThatExceedTheLimit() throws Exception {
		assertThat(this.embeddingModel).isNotNull();
		String contentAsString = this.resource.getContentAsString(StandardCharsets.UTF_8);
		assertThatThrownBy(
				() -> this.embeddingModel.embed(List.of(new Document("Hello Word"),new Document(contentAsString)),
						DashScopeEmbeddingOptions.builder().model(DashScopeApi.DEFAULT_EMBEDDING_MODEL).build(),
						new TokenCountBatchingStrategy()))
		.isInstanceOf(IllegalArgumentException.class);
	}
	
	

}
