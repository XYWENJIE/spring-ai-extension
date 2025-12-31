package org.xywenjie.spring.ai.dashscope.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeImageRequest {

	@JsonProperty("model")
	private String model;

	@JsonProperty("input")
	private Input input = new Input();
	
	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public class Input{

		@JsonProperty("prompt")
		private String prompt;

		public String getPrompt() {
			return prompt;
		}

		public void setPrompt(String prompt) {
			this.prompt = prompt;
		}
	}

	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder{
		private final DashScopeImageRequest imageRequest = new DashScopeImageRequest();
		
		public Builder model(String model) {
			imageRequest.setModel(model);
			return this;
		}

		public Builder prompt(String prompt){
			imageRequest.input.setPrompt(prompt);
			return this;
		}
		
		public DashScopeImageRequest build() {
			return imageRequest;
		}
	}
}
