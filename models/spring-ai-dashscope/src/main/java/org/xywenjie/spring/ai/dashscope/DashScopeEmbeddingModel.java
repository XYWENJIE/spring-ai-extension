package org.xywenjie.spring.ai.dashscope;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
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
import org.xywenjie.spring.ai.dashscope.api.DashScopeApi;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeRequest;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeResponse;

import io.micrometer.observation.ObservationRegistry;

/**
 * DashScope 向量嵌入模型实现类，用于对接阿里云 DashScope AI 服务的文本向量化能力。
 *
 * <p>支持功能：</p>
 * <ul>
 *   <li>文本向量化（Embedding）</li>
 *   <li>批量文本处理</li>
 *   <li>可配置的元数据模式</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * DashScopeEmbeddingModel embeddingModel = new DashScopeEmbeddingModel(dashScopeApi);
 *
 * EmbeddingResponse response = embeddingModel.call(
 *     new EmbeddingRequest(List.of("文本内容"), 
 *     DashScopeEmbeddingOptions.builder().model("text-embedding-v3").build())
 * );
 * }</pre>
 *
 * @author 黄文杰
 * @since 1.0.0
 * @see AbstractEmbeddingModel
 * @see DashScopeApi
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
	
	/**
	 * 使用默认元数据模式构造嵌入模型。
	 *
	 * @param dashScopeApi DashScope API 客户端
	 */
	public DashScopeEmbeddingModel(DashScopeApi dashScopeApi) {
		this(dashScopeApi,MetadataMode.EMBED);
	}

	/**
	 * 使用指定元数据模式构造嵌入模型。
	 *
	 * @param dashScopeApi DashScope API 客户端
	 * @param metadataMode 元数据模式
	 */
	public DashScopeEmbeddingModel(DashScopeApi dashScopeApi,MetadataMode metadataMode) {
		this(dashScopeApi,metadataMode,DashScopeEmbeddingOptions.builder().model(DashScopeApi.DEFAULT_EMBEDDING_MODEL).build());
	}

	/**
	 * 使用自定义选项构造嵌入模型。
	 *
	 * @param dashScopeApi DashScope API 客户端
	 * @param metadataMode 元数据模式
	 * @param options 嵌入选项
	 */
	public DashScopeEmbeddingModel(DashScopeApi dashScopeApi,MetadataMode metadataMode,DashScopeEmbeddingOptions options) {
		this(dashScopeApi,metadataMode,options,RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	/**
	 * 使用自定义选项和重试策略构造嵌入模型。
	 *
	 * @param dashScopeApi DashScope API 客户端
	 * @param metadataMode 元数据模式
	 * @param options 嵌入选项
	 * @param retryTemplate 重试模板
	 */
	public DashScopeEmbeddingModel(DashScopeApi dashScopeApi,MetadataMode metadataMode,DashScopeEmbeddingOptions options,
			RetryTemplate retryTemplate) {
		this(dashScopeApi,metadataMode,options,retryTemplate,ObservationRegistry.NOOP);
	}
	
	/**
	 * 使用完整参数构造嵌入模型。
	 *
	 * @param dashScopeApi DashScope API 客户端
	 * @param metadataMode 元数据模式
	 * @param options 嵌入选项
	 * @param retryTemplate 重试模板
	 * @param observationRegistry 观测注册表
	 */
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
