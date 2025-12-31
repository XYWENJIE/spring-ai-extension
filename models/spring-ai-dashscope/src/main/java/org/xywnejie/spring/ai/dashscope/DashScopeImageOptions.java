package org.xywnejie.spring.ai.dashscope;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.image.ImageOptions;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeImageOptions implements ImageOptions {

    public static Builder builder(){
        return new Builder();
    }

    @JsonProperty("model")
    private String model;

    @JsonProperty("n")
    private Integer n = 1;

    @JsonProperty("negative_prompt")
    private String negativePrompt;

    @JsonProperty("size")
    private String size;

    @JsonProperty("prompt_extend")
    private Boolean promptExtend;

    @JsonProperty("watermark")
    private Boolean watermark;

    @JsonProperty("seed")
    private Integer seed;

    @JsonProperty("width")
    private Integer width;

    @JsonProperty("height")
    private Integer height;

    @Override
    public Integer getN() {
        return this.n;
    }

    @Override
    public String getModel() {
        return this.model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public Integer getWidth() {
        return this.width;
    }

    public void setWidth(Integer width) {
        this.width = width;
        this.size = width+"*"+height;
    }

    @Override
    public Integer getHeight() {
        return this.height;
    }

    public void setHeight(Integer height) {
        this.height = height;
        this.size = width+"*"+height;
    }

    @Override
    public String getResponseFormat() {
        return "";
    }

    @Override
    public String getStyle() {
        return "";
    }

    public String getNegativePrompt() {
        return negativePrompt;
    }

    public void setNegativePrompt(String negativePrompt) {
        this.negativePrompt = negativePrompt;
    }

    public String getSize() {
        return size;
    }

    public Boolean getPromptExtend() {
        return promptExtend;
    }

    public Boolean getWatermark() {
        return watermark;
    }

    public Integer getSeed() {
        return seed;
    }

    @Override
    public String toString() {
        return "DashScopeImageOptions{" +
                "model='" + model + '\'' +
                ", n=" + n +
                ", negativePrompt='" + negativePrompt + '\'' +
                ", size='" + size + '\'' +
                ", promptExtend=" + promptExtend +
                ", watermark=" + watermark +
                ", seed=" + seed +
                ", width=" + width +
                ", height=" + height +
                '}';
    }

    public static class Builder{

        protected DashScopeImageOptions options;

        public Builder(){
            this.options = new DashScopeImageOptions();
        }

        public Builder model(String model){
            this.options.setModel(model);
            return this;
        }

        public DashScopeImageOptions build(){
            return this.options;
        }

    }
}
