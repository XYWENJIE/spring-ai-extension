package org.xywnejie.spring.ai.dashscope;

import org.springframework.ai.embedding.EmbeddingOptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Huang Wenjie
 */
@JsonInclude(Include.NON_NULL)
public class DashScopeEmbeddingOptions implements EmbeddingOptions {
	
	private @JsonProperty("model") String model;
	
	private @JsonProperty("dimension") Integer dimensions;
	
	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String getModel() {
		return model;
	}
	
	public void setModel(String model) {
		this.model = model;
	}

	@Override
	public Integer getDimensions() {
		return dimensions;
	}
	
	public void setDimensions(Integer dimensions) {
		this.dimensions = dimensions;
	}
	
	public static final class Builder {
		protected DashScopeEmbeddingOptions options;
		
		public Builder model(String model) {
			options.model = model;
			return this;
		}
		
		public Builder dimensions(Integer dimensions) {
			options.dimensions = dimensions;
			return this;
		}
		
		public Builder() {
			this.options = new DashScopeEmbeddingOptions();
		}
		
		public DashScopeEmbeddingOptions build() {
			return this.options;
		}
	}

}
