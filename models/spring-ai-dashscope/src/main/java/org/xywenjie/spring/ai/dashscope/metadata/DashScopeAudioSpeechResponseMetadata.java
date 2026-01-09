package org.xywenjie.spring.ai.dashscope.metadata;

import org.springframework.ai.audio.tts.TextToSpeechResponseMetadata;
import org.springframework.ai.chat.metadata.RateLimit;

public class DashScopeAudioSpeechResponseMetadata extends TextToSpeechResponseMetadata {
	
	private RateLimit rateLimit;
	
	public DashScopeAudioSpeechResponseMetadata(RateLimit rateLimit) {
		this.rateLimit = rateLimit;
	}
	
	public RateLimit getRateLimit() {
		return rateLimit;
	}

}
