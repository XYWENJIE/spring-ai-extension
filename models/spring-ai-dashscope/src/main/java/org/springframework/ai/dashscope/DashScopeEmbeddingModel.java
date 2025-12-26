package org.springframework.ai.dashscope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.util.Assert;

/**
 * @author Huang Wenjie
 */
public class DashScopeEmbeddingModel extends AbstractEmbeddingModel{

	private static final Logger logger = LoggerFactory.getLogger(DashScopeEmbeddingModel.class);

	@Override
	public EmbeddingResponse call(EmbeddingRequest request) {
		EmbeddingRequest embeddingRequest = buildEmbeddingRequest(request);
		return null;
	}

	@Override
	public float[] embed(Document document) {
		Assert.notNull(document, "Document must not be null");
		return this.embed(document.getFormattedContent(this.metadataMode));
	}

	private EmbeddingRequest buildEmbeddingRequest(EmbeddingRequest request) {
		DashScopeEmbeddingOptions runtimeOptions = null;
		if(request.getOptions() != null){
			runtimeOptions = ModelOptionsUtils.copyToTarget(request.getOptions(), EmbeddingOptions.class,
			 DashScopeAudioSpeechOptions.class);
		}

		DashScopeEmbeddingOptions requestOptions = runtimeOptions == null ? 
	}

}
