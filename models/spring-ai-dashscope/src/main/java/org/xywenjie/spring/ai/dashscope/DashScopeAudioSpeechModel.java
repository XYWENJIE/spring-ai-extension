package org.xywenjie.spring.ai.dashscope;

import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.metadata.RateLimit;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeRequest;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeResponse;
import org.xywenjie.spring.ai.dashscope.metadata.DashScopeResponseHeaderExtractor;
import org.xywenjie.spring.ai.dashscope.api.DashScopeAudioApi;

import reactor.core.publisher.Flux;

public class DashScopeAudioSpeechModel implements TextToSpeechModel {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final DashScopeAudioApi audioApi;

	private final DashScopeAudioSpeechOptions defaultOptions;

	private final RetryTemplate retryTemplate;

	public DashScopeAudioSpeechModel(DashScopeAudioApi audioApi, DashScopeAudioSpeechOptions options,
			RetryTemplate retryTemplate) {
		Assert.notNull(audioApi, "");
		this.audioApi = audioApi;
		this.defaultOptions = options;
		this.retryTemplate = retryTemplate;
	}

	@Override
	public byte[] call(String text) {
		TextToSpeechPrompt prompt = new TextToSpeechPrompt(text);
		return call(prompt).getResult().getOutput();
	}

	@Override
	public TextToSpeechResponse call(TextToSpeechPrompt prompt) {
		DashScopeRequest dashScopeRequest = createRequest(prompt);
		ResponseEntity<DashScopeResponse> speechEntity = RetryUtils.execute(this.retryTemplate,
				() -> this.audioApi.createSpeech(dashScopeRequest));
		
		var speechResponse = speechEntity.getBody();
		if(speechResponse == null) {
			logger.warn("No speech response returned for speechRequest: {}",speechResponse);
			return new TextToSpeechResponse(List.of(new Speech(new byte[0])));
		}
		if(speechResponse.get)
		RateLimit rateLimit = DashScopeResponseHeaderExtractor;
		try(InputStream in = new URL(speechResponse.getOutput().getAudio().getUrl()).openStream()){
			return new TextToSpeechResponse(List.of(new Speech(in.readAllBytes())));
		}
		return new TextToSpeechResponse(List.of(new Speech(new byte[0])));;
	}

	@Override
	public Flux<TextToSpeechResponse> stream(TextToSpeechPrompt prompt) {
		DashScopeRequest speechRequest = createRequest(prompt);

		Flux<ResponseEntity<DashScopeResponse>> speechEntity = RetryUtils.execute(this.retryTemplate,
				() -> this.audioApi.stream(speechRequest));
		return speechEntity.map(entity -> {
			String base64Data = entity.getBody().getOutput().getAudio().getData();
			byte[] audioBytes = Base64.getDecoder().decode(base64Data);
			return new TextToSpeechResponse(List.of(new Speech(audioBytes)));
		});
	}

	private DashScopeRequest createRequest(TextToSpeechPrompt prompt) {
		DashScopeAudioSpeechOptions runtimeOptions = (prompt
				.getOptions() instanceof DashScopeAudioSpeechOptions dashScopeAudioSpeechOptions)
						? dashScopeAudioSpeechOptions
						: null;
		DashScopeAudioSpeechOptions options = (runtimeOptions != null) ? this.merge(runtimeOptions, this.defaultOptions)
				: this.defaultOptions;
		String text = StringUtils.hasText(options.getText()) ? options.getText() : prompt.getInstructions().getText();
		
		DashScopeRequest dsadsa = new DashScopeRequest();
		return dsadsa;

	}
}
