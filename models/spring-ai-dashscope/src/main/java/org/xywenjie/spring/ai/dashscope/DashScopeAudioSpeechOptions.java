package org.xywenjie.spring.ai.dashscope;

import org.springframework.ai.audio.tts.TextToSpeechOptions;

public class DashScopeAudioSpeechOptions implements TextToSpeechOptions {

    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

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
