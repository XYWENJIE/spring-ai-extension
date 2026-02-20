package org.xywenjie.spring.ai.audio.transcription;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.dashscope.testutils.AbstractIT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.xywenjie.spring.ai.DashScopeTestConfiguration;
import org.xywenjie.spring.ai.dashscope.DashScopeAudioTranscriptionOptions;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DashScopeTestConfiguration.class)
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY",matches = ".+")
public class DashScopeTranscriptionModelIT extends AbstractIT {

    @Value("classpath:/speech/jfk.flac")
    private Resource audioFile;

    @Test
    void callTest(){
        DashScopeAudioTranscriptionOptions transcriptionOptions = DashScopeAudioTranscriptionOptions.builder().build();
        AudioTranscriptionPrompt transcriptionPrompt = new AudioTranscriptionPrompt(this.audioFile,transcriptionOptions);
        AudioTranscriptionResponse response = this.transcriptionModel.call(transcriptionPrompt);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getOutput().toLowerCase().contains("fellow")).isTrue();
    }

    @Test
    void callTestWithOptions() {

        DashScopeAudioTranscriptionOptions transcriptionOptions = DashScopeAudioTranscriptionOptions.builder()
                .language("en")
                .prompt("Ask not this, but ask that")
                .build();
        AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(this.audioFile,
                transcriptionOptions);
        AudioTranscriptionResponse response = this.transcriptionModel.call(transcriptionRequest);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getOutput().toLowerCase().contains("fellow")).isTrue();
    }

    @Test
    void transcribeTest() {
        String response = this.transcriptionModel.transcribe(this.audioFile);
        assertThat(response).isNotNull();
        assertThat(response.toLowerCase().contains("fellow")).isTrue();
    }

    @Test
    void transcribeTestWithOptions() {
        DashScopeAudioTranscriptionOptions transcriptionOptions = DashScopeAudioTranscriptionOptions.builder()
                .language("en")
                .prompt("Ask not this, but ask that")
                .build();
        String response = this.transcriptionModel.transcribe(this.audioFile, transcriptionOptions);
        assertThat(response).isNotNull();
        assertThat(response.toLowerCase().contains("fellow")).isTrue();
    }
}
