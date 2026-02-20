package org.xywenjie.spring.ai.dashscope;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.audio.transcription.AudioTranscriptionOptions;

public class DashScopeAudioTranscriptionOptions implements AudioTranscriptionOptions {

    private @JsonProperty("model") String model = "qwen3-asr-flash";

    private @JsonProperty("language") String language;

    private @JsonProperty("prompt") String prompt;

    public static Builder builder(){
        return new Builder();
    }

    @Override
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public static final class Builder {

        public Builder model(String model){
            this.options.model = model;
            return this;
        }

        public Builder language(String language){
            this.options.language = language;
            return this;
        }

        public Builder prompt(String prompt){
            this.options.prompt = prompt;
            return this;
        }

        protected DashScopeAudioTranscriptionOptions options;

        public Builder(){
            this.options = new DashScopeAudioTranscriptionOptions();
        }

        public DashScopeAudioTranscriptionOptions build(){
            return this.options;
        }
    }
}
