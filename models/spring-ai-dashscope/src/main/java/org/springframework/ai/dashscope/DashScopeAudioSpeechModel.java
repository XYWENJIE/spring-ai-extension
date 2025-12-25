package org.springframework.ai.dashscope;

import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import reactor.core.publisher.Flux;

public class DashScopeAudioSpeechModel implements TextToSpeechModel {
    @Override
    public TextToSpeechResponse call(TextToSpeechPrompt prompt) {
        return null;
    }

    @Override
    public Flux<TextToSpeechResponse> stream(TextToSpeechPrompt prompt) {
        return null;
    }
}
