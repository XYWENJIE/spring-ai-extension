package org.xywenjie.spring.ai.hunyuan.api;

import java.util.function.Consumer;

import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import reactor.core.publisher.Flux;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HunyuanApi {

	private static final String HTTP_USER_AGENT_HEADER = "User-Agent";

	private static final String SPRING_AI_USER_AGENT = "spring-ai-extension";

	private final String secretId;

	private final String secretKey;

	private final RestClient restClient;

	private final WebClient webClient;

	public static Builder builder() {
		return new Builder();
	}

	public HunyuanApi(String baseUrl, String secretId, String secretKey, HttpHeaders headers,
			ResponseErrorHandler responseErrorHandler, RestClient.Builder restClientBulder,
			WebClient.Builder webClientBuilder) {
		this.secretId = secretId;
		this.secretKey = secretKey;
		Consumer<HttpHeaders> finalHeaders = h -> {
			h.setContentType(MediaType.APPLICATION_JSON);
			h.set(HTTP_USER_AGENT_HEADER, SPRING_AI_USER_AGENT);
			h.addAll(headers);
		};
		this.restClient = restClientBulder.baseUrl(baseUrl).defaultHeaders(finalHeaders).build();
		this.webClient = webClientBuilder.baseUrl(baseUrl)
				.defaultHeaders(finalHeaders).build();

	}

	public ResponseEntity<ChatCompletions> chatCompletionEntity(ChatCompletionRequest request,
			HttpHeaders additionalHttpHeader) {
		Assert.hasText(request.model, "model must be set");
		return this.restClient.post().headers(headers -> {
			headers.add("X-TC-Action", "ChatCompletions");
			headers.add("X-TC-Version", "2023-09-01");
			headers.addAll(additionalHttpHeader);
			addDefaultHeadersIfMissing(headers);
		}).body(request).retrieve().toEntity(ChatCompletions.class);
	}

	public Flux<ChatCompletions> chatCompletionStream(ChatCompletionRequest request, HttpHeaders additionalHttpHeader) {
		Assert.hasText(request.model, "model must be set");
		request.stream = true;
		return this.webClient.post().headers(headers -> {
			headers.add("X-TC-Action", "ChatCompletions");
			headers.add("X-TC-Version", "2023-09-01");
			headers.addAll(additionalHttpHeader);
			addDefaultHeadersIfMissing(headers);
		}).bodyValue(request).retrieve().bodyToFlux(ChatCompletions.class);
	}

	private void addDefaultHeadersIfMissing(HttpHeaders headers) {
//		if(headers.get(HttpHeaders.AUTHORIZATION) == null && !(this.apiKey instanceof NoopApiKey)) {
//			headers.setBearerAuth(this.apiKey.getValue());
//		}
	}

	@JsonInclude(Include.NON_NULL)
	public class ChatCompletionRequest {

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

	public class ChatCompletions {

	}

	public static final class Builder {

		private String baseUrl = "https://hunyuan.tencentcloudapi.com";

		private String secretId;

		private String secretKey;

		private HttpHeaders headers = new HttpHeaders();

		private RestClient.Builder restClientBuilder = RestClient.builder();

		private WebClient.Builder webClientBuilder = WebClient.builder();

		private ResponseErrorHandler responseErrorHandler = RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER;

		public Builder secretId(String secretId) {
			this.secretId = secretId;
			return this;
		}

		public Builder secretKey(String secretKey) {
			this.secretKey = secretKey;
			return this;
		}

		public Builder headers(HttpHeaders headers) {
			this.headers = headers;
			return this;
		}

		public Builder baseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
			return this;
		}

		public HunyuanApi build() {
			Assert.notNull(this.secretId, "secretId must be set");
			Assert.notNull(this.secretKey, "secretKey must be set");
			return new HunyuanApi(this.baseUrl, this.secretId, this.secretKey, this.headers, this.responseErrorHandler,
					this.restClientBuilder, this.webClientBuilder);
		}
	}

}
