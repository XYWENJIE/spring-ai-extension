package org.springframework.ai.dashscope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.dashscope.api.DashScopeApi;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeChatOptions implements ToolCallingChatOptions{
	
	private @JsonProperty("model") String model;
	
	@JsonProperty("parameters")
	private DashScopeApi.Parameters parameters = new DashScopeApi.Parameters();
	
	@JsonIgnore
	private Map<String,String> httpHeaders = new HashMap<>();
	
	@JsonIgnore
	private List<FunctionCallback> toolCallbacks = new ArrayList<>();
	
	@JsonIgnore
	private Set<String> toolNames = new HashSet<>();
	
	@JsonIgnore
	private Map<String,Object> toolContext = new HashMap<>();
	
	public static Builder builder() {
		return new Builder();
	}

	@Override
	public List<FunctionCallback> getFunctionCallbacks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFunctionCallbacks(List<FunctionCallback> functionCallbacks) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<String> getFunctions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFunctions(Set<String> functions) {
		// TODO Auto-generated method stub
		
	}

	@Override
	@JsonIgnore
	public Map<String, Object> getToolContext() {
		return this.toolContext;
	}

	@Override
	public void setToolContext(Map<String, Object> toolContext) {
		this.toolContext = toolContext;
	}

	@Override
	public String getModel() {
		return this.model;
	}
	
	public void setModel(String model) {
		this.model = model;
	}

	@Override
	public Double getFrequencyPenalty() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getMaxTokens() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double getPresencePenalty() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getStopSequences() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double getTemperature() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getTopK() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double getTopP() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends ChatOptions> T copy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@JsonIgnore
	public List<FunctionCallback> getToolCallbacks() {
		return this.toolCallbacks;
	}

	@Override
	@JsonIgnore
	public void setToolCallbacks(List<FunctionCallback> toolCallbacks) {
		Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
		Assert.noNullElements(toolCallbacks, "toolCallbacks cannot contain null elements");
		this.toolCallbacks = toolCallbacks;
	}

	@Override
	@JsonIgnore
	public Set<String> getToolNames() {
		return this.toolNames;
	}

	@Override
	public void setToolNames(Set<String> toolNames) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Boolean isInternalToolExecutionEnabled() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setInternalToolExecutionEnabled(Boolean internalToolExecutionEnabled) {
		// TODO Auto-generated method stub
		
	}
	
	public DashScopeApi.Parameters getParameters() {
		return parameters;
	}

	public void setParameters(DashScopeApi.Parameters parameters) {
		this.parameters = parameters;
	}
	
	public String toString() {
		return "DashScopeChatOptions:"+ModelOptionsUtils.toJsonString(this);
	}

	public static class Builder{
		
		protected DashScopeChatOptions options;
		
		public Builder() {
			this.options = new DashScopeChatOptions();
		}
		
		public Builder model(DashScopeApi.ChatModel chatModel) {
			this.options.model = chatModel.getName();
			return this;
		}
		
		public Builder tools(List<DashScopeApi.FunctionTool> tools) {
			this.options.parameters.setTools(tools);
			return this;
		}
		
		public Builder seed(Integer seed) {
			this.options.parameters.setSeed(seed);
			return this;
		}
		
		public Builder toolCallbacks(List<FunctionCallback> toolCallbacks) {
			this.options.setToolCallbacks(toolCallbacks);
			return this;
		}
		
		public DashScopeChatOptions build() {
			return this.options;
		}
	}

	public List<DashScopeApi.FunctionTool> getTools() {
		return parameters.getTools();
	}

	public void setTools(List<DashScopeApi.FunctionTool> tools) {
		this.parameters.setTools(tools);
	}



	public Map<String, String> getHttpHeaders() {
		return httpHeaders;
	}

	public void setHttpHeaders(Map<String, String> httpHeaders) {
		this.httpHeaders = httpHeaders;
	}
	
	

}
