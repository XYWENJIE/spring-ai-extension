package org.xywenjie.spring.ai.dashscope;

import org.springframework.ai.audio.tts.TextToSpeechOptions;

public class DashScopeAudioSpeechOptions implements TextToSpeechOptions {

    private String text;
    
    private String model;
    
    private String voice;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
		this.model = model;
	}

    @Override
    public String getVoice() {
        return voice;
    }
    
    public void setVoice(String voice) {
		this.voice = voice;
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
    
    public static Builder builder() {
    	return new Builder();
    }
    
    public static class Builder{
    	private final DashScopeAudioSpeechOptions options = new DashScopeAudioSpeechOptions();
    	
    	public Builder model(String model) {
    		this.options.model = model;
    		return this;
    	}
    	
    	public Builder text(String text) {
    		this.options.text = text;
    		return this;
    	}
    	
    	public Builder voice(String voice) {
			this.options.voice = voice;
			return this;
		}
    	
    	public DashScopeAudioSpeechOptions build() {
    		return this.options;
    	}
    	
    	
    }
}
