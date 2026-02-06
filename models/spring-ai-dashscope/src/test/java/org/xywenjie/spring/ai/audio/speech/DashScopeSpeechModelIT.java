package org.xywenjie.spring.ai.audio.speech;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.dashscope.testutils.AbstractIT;
import org.springframework.boot.test.context.SpringBootTest;
import org.xywenjie.spring.ai.DashScopeTestConfiguration;
import org.xywenjie.spring.ai.dashscope.DashScopeAudioSpeechOptions;
import org.xywenjie.spring.ai.dashscope.metadata.DashScopeAudioSpeechResponseMetadata;

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
	
	@Test
	void shouldProduceAudioBytesDirectlyFromMessage() {
		byte[] audioBytes = this.speechModel.call("Today is a wonderful day to build something people love!");
		assertThat(audioBytes).hasSizeGreaterThan(0);
	}
	
	@Test
	void shouldGenerateNonEmptyMp3AudioFromTextToSpeechPrompt() {
		//TODO QWen-TTS不支持MP3
	}
	
	@Test
	void speechRateLimitTest() {
		DashScopeAudioSpeechOptions speechOptions = DashScopeAudioSpeechOptions.builder()
				.voice("Cherry")
				.model("qwen3-tts-flash")
				.build();
		TextToSpeechPrompt speechPrompt = new TextToSpeechPrompt("Today is a wonderful day to build something people love!",speechOptions);
		TextToSpeechResponse response = this.speechModel.call(speechPrompt);
		DashScopeAudioSpeechResponseMetadata metadata = (DashScopeAudioSpeechResponseMetadata)response.getMetadata();
		assertThat(metadata).isNotNull();
		assertThat(metadata.getRateLimit()).isNotNull();
		assertThat(metadata.getRateLimit().getRequestsLimit()).isPositive();
		assertThat(metadata.getRateLimit().getRequestsLimit()).isPositive();
	}

}
