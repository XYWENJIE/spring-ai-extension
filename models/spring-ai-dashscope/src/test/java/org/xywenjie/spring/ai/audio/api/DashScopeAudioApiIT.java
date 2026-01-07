package org.xywenjie.spring.ai.audio.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestClient;
import org.xywenjie.spring.ai.dashscope.api.DashScopeAudioApi;
import org.xywenjie.spring.ai.dashscope.api.DashScopeAudioApi.SpeechResponse;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeRequest;

@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY",matches = ".+")
public class DashScopeAudioApiIT {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	DashScopeAudioApi audioApi = DashScopeAudioApi.builder()
			.apiKey(new SimpleApiKey(System.getenv("DASHSCOPE_API_KEY"))).build();
	
	RestClient restClient = RestClient.create();
	
	@Test
	void speechTranscriptionAndTranslation() throws IOException, InterruptedException {
		SpeechResponse speechResponse = this.audioApi.createSpeech(DashScopeRequest.builder()
				.model("qwen3-tts-flash")
				.text("Hello, my name is Chris and I love Spring A.I.")
				.voice("Cherry")
				.build()).getBody();
		String audioUrl = speechResponse.output().audio().url();
		log.info("{}",audioUrl);
		assertThat(audioUrl).isNotEmpty();
		try(InputStream in = new URL(audioUrl).openStream()){
			FileCopyUtils.copy(in.readAllBytes(), new File("target/speech.wav"));
		}
		//byte[] speech = this.restClient.get().uri(audioUrl).headers(header -> header.clear()).retrieve().body(byte[].class);
		
	}

}
