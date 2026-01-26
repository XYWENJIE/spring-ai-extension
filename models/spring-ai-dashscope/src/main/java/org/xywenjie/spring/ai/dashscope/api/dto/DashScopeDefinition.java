package org.xywenjie.spring.ai.dashscope.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DashScopeDefinition {
	
	public enum ImageModel {
		
	}
	
	public enum ChatModel {
		QWEN3_MAX("qwen3-max"),
		QWEN_MAX("qwen-max"),
		QWEN_PLUS("qwen-plus"),
		QWEN_FLASH("qwen-flash");

		private final String value;

		ChatModel(String value){
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}
	
	public enum VideoModel {
		
	}

	public enum Role{
		@JsonProperty("system")
		SYSTEM,
		@JsonProperty("user")
		USER,
		@JsonProperty("assistant")
		ASSISTANT,
		@JsonProperty("tool")
		TOOL
	}

}
