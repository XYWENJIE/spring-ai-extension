package org.xywenjie.spring.ai.moyin.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import org.xywenjie.spring.ai.model.AppKeySecret;
import org.xywenjie.spring.ai.moyin.api.MoyinAudioApi.SpeechRequest;

@EnabledIfEnvironmentVariable(named = "MOYIN_API_KEY",matches = ".+")
public class MoyinAudioApiIT {
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	MoyinAudioApi audioApi = MoyinAudioApi.builder()
			.apiKey(new AppKeySecret(System.getenv("MOYIN_API_KEY"),"")).build();
	
	@Test
	void spechTranscriptonAndTranslation() throws IOException {
		ResponseEntity<byte[]> responseEntity =this.audioApi.createSpeech(SpeechRequest.builder()
				.text("Hello, my name is Chris and I love Spring A.I.").build());
		log.info("{}",responseEntity.getStatusCode());
		byte[] speech = responseEntity.getBody();
		assertThat(speech).isNotEmpty();
		
		FileCopyUtils.copy(speech, new File("target/speech.mp3"));
		
		//St
	}

}
