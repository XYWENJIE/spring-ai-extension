package org.springframework.ai.dashscope;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.dashscope.api.DashScopeApi;
import org.springframework.ai.dashscope.api.dto.DashScopeRequest;
import org.springframework.ai.dashscope.api.dto.DashScopeResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.embedding.observation.DefaultEmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationDocumentation;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.util.Assert;

import io.micrometer.observation.ObservationRegistry;

/**
 * @author Huang Wenjie
 */
public class DashScopeEmbeddingModel extends AbstractEmbeddingModel{

	private static final Logger logger = LoggerFactory.getLogger(DashScopeEmbeddingModel.class);
	
	private static final EmbeddingModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultEmbeddingModelObservationConvention();
	
	private final MetadataMode metadataMode;
	
	private final DashScopeApi dashScopeApi;
	
	private final ObservationRegistry observationRegistry;
	
	private EmbeddingModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;
	
	private final DashScopeEmbeddingOptions defaultOptions;
	
	private final RetryTemplate retryTemplate;
	
	public DashScopeEmbeddingModel(DashScopeApi dashScopeApi) {
		this(dashScopeApi,MetadataMode.EMBED);
	}
	
	public DashScopeEmbeddingModel(DashScopeApi dashScopeApi,MetadataMode metadataMode) {
		this(dashScopeApi,metadataMode,DashScopeEmbeddingOptions.builder().model(DashScopeApi.DEFAULT_EMBEDDING_MODEL).build());
	}
	
	public DashScopeEmbeddingModel(DashScopeApi dashScopeApi,MetadataMode metadataMode,DashScopeEmbeddingOptions options) {
		this(dashScopeApi,metadataMode,options,RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}
	
	public DashScopeEmbeddingModel(DashScopeApi dashScopeApi,MetadataMode metadataMode,DashScopeEmbeddingOptions options,
			RetryTemplate retryTemplate) {
		this(dashScopeApi,metadataMode,options,retryTemplate,ObservationRegistry.NOOP);
	}
	
	public DashScopeEmbeddingModel(DashScopeApi dashScopeApi,MetadataMode metadataMode,DashScopeEmbeddingOptions options,
			RetryTemplate retryTemplate,ObservationRegistry observationRegistry) {
		Assert.notNull(dashScopeApi, "dashScopeApi must not be null");
		Assert.notNull(metadataMode, "metadataMode must not be null");
		Assert.notNull(options, "options must not be null");
		Assert.notNull(retryTemplate, "retryTemplate must not be null");
		Assert.notNull(observationRegistry, "observationRegistry must not be null");
		
		this.dashScopeApi = dashScopeApi;
		this.metadataMode = metadataMode;
		this.defaultOptions = options;
		this.retryTemplate = retryTemplate;
		this.observationRegistry = observationRegistry;
	}
	
	@Override
	public float[] embed(Document document) {
		Assert.notNull(document, "Document must not be null");
		return this.embed(document.getFormattedContent(this.metadataMode));
	}

	@Override
	public EmbeddingResponse call(EmbeddingRequest request) {
		EmbeddingRequest embeddingRequest = buildEmbeddingRequest(request);
		DashScopeRequest dashScopeRequest = createRequest(embeddingRequest);
		var observationContext = EmbeddingModelObservationContext.builder().embeddingRequest(embeddingRequest).provider("DASHSCOPE").build();
		String model = dashScopeRequest.getModel();
		return EmbeddingModelObservationDocumentation.EMBEDDING_MODEL_OPERATION
				.observation(this.observationConvention,DEFAULT_OBSERVATION_CONVENTION,()-> observationContext,this.observationRegistry)
				.observe(() -> {
					DashScopeResponse apiEmbeddingResponse = RetryUtils.execute(this.retryTemplate, 
							() -> this.dashScopeApi.embeddings(dashScopeRequest).getBody());
					if(apiEmbeddingResponse == null) {
						logger.warn("No embeddings returned for request: {}",request);
						return new EmbeddingResponse(List.of());
					}
					DashScopeResponse.Usage usage = apiEmbeddingResponse.getUsage();
					Usage embeddingResponseUsage = usage != null ? getDefaultUsage(usage) : new EmptyUsage();
					var metadata = new EmbeddingResponseMetadata(model,embeddingResponseUsage);
					List<Embedding> embeddings = apiEmbeddingResponse.getOutput().getEmbeddings()
							.stream()
							.map(e -> new Embedding(e.getEmbedding(), e.getTextIndex()))
							.toList();
					EmbeddingResponse embeddingResponse = new EmbeddingResponse(embeddings,metadata);
					observationContext.setResponse(embeddingResponse);
					return embeddingResponse;
				});
	}
	
	private DefaultUsage getDefaultUsage(DashScopeResponse.Usage usage) {
		return new DefaultUsage(usage.getTotalTokens(), usage.getTotalTokens(),usage.getTotalTokens());
	}
	
	private DashScopeRequest createRequest(EmbeddingRequest request) {
		DashScopeEmbeddingOptions requestOptions = (DashScopeEmbeddingOptions)request.getOptions();
		return DashScopeRequest.builder().model(requestOptions.getModel()).texts(request.getInstructions()).duration(requestOptions.getDimensions()).build();
	}



	private EmbeddingRequest buildEmbeddingRequest(EmbeddingRequest embeddingRequest) {
		DashScopeEmbeddingOptions runtimeOptions = null;
		if(embeddingRequest.getOptions() != null){
			runtimeOptions = ModelOptionsUtils.copyToTarget(embeddingRequest.getOptions(), EmbeddingOptions.class,
			 DashScopeEmbeddingOptions.class);
		}

		//TODO
		DashScopeEmbeddingOptions requestOptions = runtimeOptions == null ? this.defaultOptions : DashScopeEmbeddingOptions
				.builder()
				.model(ModelOptionsUtils.mergeOption(runtimeOptions.getModel(), this.defaultOptions.getModel()))
				.dimensions(ModelOptionsUtils.mergeOption(runtimeOptions.getDimensions(), this.defaultOptions.getDimensions()))
				.build();
		return new EmbeddingRequest(embeddingRequest.getInstructions(), requestOptions);
	}
	
	public void setObservationConvention(EmbeddingModelObservationConvention observationConvention) {
		Assert.notNull(observationConvention, "observationConvention cannot be null");
		this.observationConvention = observationConvention;
	}

}
