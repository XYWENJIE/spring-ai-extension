package org.xywenjie.spring.ai.moyin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.core.retry.RetryTemplate;
import org.xywenjie.spring.ai.moyin.api.MoyinAudioApi;

import reactor.core.publisher.Flux;

public class MoyinAudioSpeechModel implements TextToSpeechModel {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final MoyinAudioSpeechOptions defaultOptions;
	
	private RetryTemplate retryTemplate;
	
	private final MoyinAudioApi audioApi;
	
	public MoyinAudioSpeechModel(MoyinAudioApi audioApi) {
		this(audioApi,new MoyinAudioSpeechOptions());
	}
	
	public MoyinAudioSpeechModel(MoyinAudioApi audioApi,MoyinAudioSpeechOptions options) {
		this.audioApi = audioApi;
		this.defaultOptions = options;
	}
	
	@Override
	public byte[] call(String text) {
		TextToSpeechPrompt prompt = new TextToSpeechPrompt(text);
		return call(prompt).getResult().getOutput();
	}
	
	@Override
	public TextToSpeechResponse call(TextToSpeechPrompt prompt) {
		return null;
	}
	
	@Override
	public Flux<TextToSpeechResponse> stream(TextToSpeechPrompt prompt) {
		// TODO Auto-generated method stub
		return null;
	}

}
