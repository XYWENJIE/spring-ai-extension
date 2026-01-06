package org.xywenjie.spring.ai.moyin.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestClient;
import org.xywenjie.spring.ai.model.AppKeySecret;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.charset.StandardCharsets;

public class MoyinAudioApi {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final RestClient restClient;

	private final ApiKey apiKey;
	
	public MoyinAudioApi(String baseUrl,ApiKey apiKey,RestClient.Builder restClientBuilder) {
		this.apiKey = apiKey;
		this.restClient = restClientBuilder
				.baseUrl(baseUrl)
				.defaultHeaders(header -> 
					header.setContentType(MediaType.APPLICATION_JSON)
				)
				.build();
	}

	public static Builder builder(){
		return new Builder();
	}
	
	public ResponseEntity<byte[]> createSpeech(SpeechRequest request){
		AppKeySecret appKeySecret = (AppKeySecret)apiKey;
		String appKey = appKeySecret.getValue();
		String secret = appKeySecret.getSecret();
		Long timestamp = System.currentTimeMillis() / 1000;
		String signature = DigestUtils.md5DigestAsHex((appKey+secret+timestamp).getBytes(StandardCharsets.UTF_8));
		SpeechRequest signatureRequest = SpeechRequest.builder().appKey(appKey).signature(signature).timestamp(timestamp).build();
		SpeechRequest targetRequest = ModelOptionsUtils.merge(signatureRequest, request, SpeechRequest.class);
		
		ResponseEntity<byte[]> response = this.restClient.post()
				.uri("/api/tts/v1")
				.accept(MediaType.parseMediaType("audio/mpeg"))
				.body(targetRequest)
				.retrieve()
				.toEntity(byte[].class);
		
		MediaType contentType = response.getHeaders().getContentType();
		if (contentType != null && !contentType.includes(MediaType.parseMediaType("audio/mpeg"))) {
			String errorResponse = new String(response.getBody(), StandardCharsets.UTF_8);
			logger.error("Moyin API returned error: {}", errorResponse);
			throw new RuntimeException("Moyin API error: " + errorResponse);
		}
		
		return response;
	}
	
	@JsonInclude(Include.NON_NULL)
	public record SpeechRequest(
			@JsonProperty("text") String text,
			@JsonProperty("appkey") String appKey,
			@JsonProperty("signature") String signature,
			@JsonProperty("timestamp") Long timestamp,
			@JsonProperty("speaker") String speaker,
			@JsonProperty("audio_type") String audioType,
			@JsonProperty("speed") Float speed,
			@JsonProperty("convert") String convert,
			@JsonProperty("rate") Long rate,
			@JsonProperty("volume") Float volume,
			@JsonProperty("pitch") Float pitch,
			@JsonProperty("pause_map") String pauseMap,
			@JsonProperty("symbol_sil") String symbolSil,
			@JsonProperty("ignore_limit") Boolean ignoreLimit,
			@JsonProperty("gen_srt") Boolean genSrt,
			@JsonProperty("merge_symbol") Boolean mergeSymbol,
			@JsonProperty("srt_len") Long srtLen,
			@JsonProperty("streaming") Boolean streaming) {
		
		public static Builder builder() {
			return new Builder();
		}
		
		public static final class Builder {
			
			private String text;
			
			private String appKey;
			
			private String signature;
			
			private Long timestamp;
			
			private String speaker;
			
			private String audioType;
			
			private Float speed;
			
			private String convert;
			
			private Long rate;
			
			private Float volume;
			
			private Float pitch;
			
			private String pauseMap;
			
			private String symbolSil;
			
			private Boolean ignoreLimit;
			
			private Boolean genSrt;
			
			private Boolean mergeSymbol;
			
			private Long srtLen;
			
			private Boolean streaming;
			
			public Builder text(String text) {
				this.text = text;
				return this;
			}
			
			public Builder appKey(String appKey) {
				this.appKey = appKey;
				return this;
			}
			
			public Builder signature(String signature) {
				this.signature = signature;
				return this;
			}
			
			public Builder timestamp(Long timestamp) {
				this.timestamp = timestamp;
				return this;
			}
			
			public Builder speaker(String speaker) {
				this.speaker = speaker;
				return this;
			}
			
			public Builder audioType(String audioType) {
				this.audioType = audioType;
				return this;
			}
			
			public Builder speed(Float speed) {
				this.speed = speed;
				return this;
			}
			
			public Builder convert(String convert) {
				this.convert = convert;
				return this;
			}
			
			public Builder rate(Long rate) {
				this.rate = rate;
				return this;
			}
			
			public Builder volume(Float volume) {
				this.volume = volume;
				return this;
			}
			
			public Builder pitch(Float pitch) {
				this.pitch = pitch;
				return this;
			}
			
			public Builder pauseMap(String pauseMap) {
				this.pauseMap = pauseMap;
				return this;
			}
			
			public Builder symbolSil(String symbolSil) {
				this.symbolSil = symbolSil;
				return this;
			}
			
			public Builder ignoreLimit(Boolean ignoreLimit) {
				this.ignoreLimit = ignoreLimit;
				return this;
			}
			
			public Builder genSrt(Boolean genSrt) {
				this.genSrt = genSrt;
				return this;
			}
			
			public Builder mergeSymbol(Boolean mergeSymbol) {
				this.mergeSymbol = mergeSymbol;
				return this;
			}
			
			public Builder srtLen(Long srtLen) {
				this.srtLen = srtLen;
				return this;
			}
			
			public Builder streaming(Boolean streaming) {
				this.streaming = streaming;
				return this;
			}
			
			public SpeechRequest build() {
				return new SpeechRequest(text, appKey, signature, timestamp, speaker, audioType, speed, convert, rate, volume, pitch, pauseMap, symbolSil, ignoreLimit, genSrt, mergeSymbol, srtLen, streaming);
			}
			
		}
	}

	public static final class Builder {

		private String baseUrl = "https://open.mobvoi.com";

		private ApiKey apiKey;

		private RestClient.Builder restClientBuilder = RestClient.builder();

		public Builder apiKey(ApiKey apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		public Builder apiKey(String appKey, String secret){
			this.apiKey = new AppKeySecret(appKey, secret);
			return this;
		}

		public MoyinAudioApi build() {
			Assert.notNull(this.apiKey, "apiKey must not be null");
			return new MoyinAudioApi(this.baseUrl,this.apiKey, this.restClientBuilder);
		}
	}

}
