package org.xywenjie.spring.ai.hunyuan.api;

import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HunyuanApi {
	
	private static final String HTTP_USER_AGENT_HEADER = "User-Agent";
	
	private static final String SPRING_AI_USER_AGENT = "Spring-ai-extends";
	
	private final String baseUrl;
	
	private final ApiKey apiKey;
	
	private final RestClient restClient;
	
	private final WebClient webClient;
	
	public HunyuanApi(String baseUrl,ApiKey apiKey,HttpHeaders headers,ResponseErrorHandler responseErrorHandler,
			RestClient restClient,WebClient webClient) {
		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
		this.restClient = restClient;
		this.webClient = webClient;
	}
	
	public ResponseEntity<ChatCompletions> chatCompletionEntity(ChatCompletionRequest request,HttpHeaders additionalHttpHeader){
		return this.restClient.post()
				.headers(headers -> {
					headers.add("X-TC-Action", "ChatCompletions");
					headers.add("X-TC-Version", "2023-09-01");
					headers.addAll(additionalHttpHeader);
					addDefaultHeadersIfMissing(headers);
				})
				.body(request)
				.retrieve()
				.toEntity(ChatCompletions.class);
	}
	
	private void addDefaultHeadersIfMissing(HttpHeaders headers) {
		if(headers.get(HttpHeaders.AUTHORIZATION) == null && !(this.apiKey instanceof NoopApiKey)) {
			headers.setBearerAuth(this.apiKey.getValue());
		}
	}
	
	public class ChatCompletionRequest{
		
		@JsonProperty("Model")
		private String model;
		
		@JsonProperty("Stream")
		private Boolean stream;
		
		@JsonProperty("StreamModeration")
		private Boolean streamModeration;
		
		@JsonProperty("TopP")
		private Float topP;
		
		@JsonProperty("Temperature")
		private Float temperature;
		
		@JsonProperty("EnbaleEnhancement")
		private Boolean enableEnhancement;
		
		private Object tools;
		
		@JsonProperty("ToolChoice")
		private String toolChoice;
	}
	
	public class ChatCompletions{
		
	}

}
