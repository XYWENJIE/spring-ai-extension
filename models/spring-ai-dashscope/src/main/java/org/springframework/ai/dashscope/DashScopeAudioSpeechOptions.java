package org.springframework.ai.dashscope;

import org.springframework.ai.audio.tts.TextToSpeechOptions;

public class DashScopeAudioSpeechOptions implements TextToSpeechOptions {
    @Override
    public String getModel() {
        return "";
    }

    @Override
    public String getVoice() {
        return "";
    }

    @Override
    public String getFormat() {
        return "";
    }

    @Override
    public Double getSpeed() {
        return 0.0;
    }

    @Override
    public <T extends TextToSpeechOptions> T copy() {
        return null;
    }
}
