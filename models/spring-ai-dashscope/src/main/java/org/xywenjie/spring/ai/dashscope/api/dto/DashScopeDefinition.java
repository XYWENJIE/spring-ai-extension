package org.xywenjie.spring.ai.dashscope.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.model.ChatModelDescription;

public class DashScopeDefinition {
	
	public enum ImageModel {
		WAN2_2_T2I_FLASH("wan2.2-t2i-flash"),
		WAN2_5_I2I_PREVIEW("wan2.5-i2i-preview");

		private final String value;

		ImageModel(String value){
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}
	
	public enum ChatModel implements ChatModelDescription {
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

		@Override
		public String getName() {
			return this.value;
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
