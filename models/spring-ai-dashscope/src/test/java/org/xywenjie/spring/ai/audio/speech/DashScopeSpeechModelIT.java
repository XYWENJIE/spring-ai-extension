package org.xywenjie.spring.ai.audio.speech;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.dashscope.DashScopeTestConfiguration;
import org.springframework.ai.dashscope.testutils.AbstractIT;
import org.springframework.boot.test.context.SpringBootTest;

import reactor.core.publisher.Flux;

@SpringBootTest(classes = DashScopeTestConfiguration.class)
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY",matches = ".+")
public class DashScopeSpeechModelIT extends AbstractIT{
	
	private final Logger log = LoggerFactory.getLogger(getClass());
	
	@Test
	void shouldSuccessfullyStreamAudioBytesForEmptyMessage() {
		TextToSpeechPrompt prompt = new TextToSpeechPrompt("Today is a wonderful day to build something people love!");
		Flux<TextToSpeechResponse> response = this.speechModel.stream(prompt);
		assertThat(response).isNotNull();
		List<TextToSpeechResponse> responses = response.collectList().block();
		assertThat(responses).isNotNull();
		log.info("Received {} audio chounks",responses.size());
	}

}
