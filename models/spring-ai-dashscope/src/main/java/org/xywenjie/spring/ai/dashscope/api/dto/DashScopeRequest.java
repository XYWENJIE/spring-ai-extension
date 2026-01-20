package org.xywenjie.spring.ai.dashscope.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DashScope request class for handling requests to the DashScope API.
 * This class contains model, input, and parameters for the request.
 * 
 * @author Huang Wenjie(黄文杰)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeRequest {

    @JsonProperty("model")
    private String model;

    @JsonProperty("input")
    private Input input = new Input();

    @JsonProperty("parameters")
    private Parameters parameters = new Parameters();

    /**
     * Get the model for this request
     * @return The model string
     */
    public String getModel() {
        return model;
    }

    /**
     * Set the model for this request
     * @param model The model string
     */
    public void setModel(String model) {
        this.model = model;
    }
    

    /**
     * Get the input for this request
     * @return The Input object
     */
    public Input getInput() {
        if (input == null) {
            input = new Input();
        }
        return input;
    }

    /**
     * Set the input for this request
     * @param input The Input object
     */
    public void setInput(Input input) {
        this.input = input;
    }

    /**
     * Get the parameters for this request
     * @return The Parameters object
     */
    public Parameters getParameters() {
        if (parameters == null) {
            parameters = new Parameters();
        }
        return parameters;
    }

    /**
     * Set the parameters for this request
     * @param parameters The Parameters object
     */
    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    /**
     * Input class containing the input parameters for the request
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Input{
    	
    	@JsonProperty("text")
    	private String text;
    	
    	@JsonProperty("voice")
    	private String voice;
    	
    	@JsonProperty("language_type")
    	private String languageType;

        @JsonProperty("messages")
        private List<Message> messages;

        @JsonProperty("prompt")
        private String prompt;

        @JsonProperty("negative_prompt")
        private String negativePrompt;

        @JsonProperty("ref_images_url")
        private List<String> refImagesUrl;

        @JsonProperty("img_url")
        private String imgUrl;

        @JsonProperty("first_frame_url")
        private String firstFrameUrl;

        @JsonProperty("last_frame_url")
        private String lastFrameUrl;

        @JsonProperty("template")
        private String template;

        @JsonProperty("reference_video_url")
        private List<String> referenceVideoUrl;

        @JsonProperty("audio_url")
        private String audioUrl;
        
        @JsonProperty("texts")
        private List<String> texts;
        
        

        public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public String getVoice() {
			return voice;
		}

		public void setVoice(String voice) {
			this.voice = voice;
		}

		public String getLanguageType() {
			return languageType;
		}

		public void setLanguageType(String languageType) {
			this.languageType = languageType;
		}

		/**
         * Get the list of messages
         * @return List of Message objects
         */
        public List<Message> getMessages() {
            return messages;
        }

        /**
         * Set the list of messages
         * @param messages List of Message objects
         */
        public void setMessages(List<Message> messages) {
            this.messages = messages;
        }

        /**
         * Get the prompt
         * @return The prompt string
         */
        public String getPrompt() {
            return prompt;
        }

        /**
         * Set the prompt
         * @param prompt The prompt string
         */
        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public String getNegativePrompt() {
            return negativePrompt;
        }

        public void setNegativePrompt(String negativePrompt) {
            this.negativePrompt = negativePrompt;
        }

        /**
         * Get the reference images URLs
         * @return List of reference images URLs
         */
        public List<String> getRefImagesUrl() {
            return refImagesUrl;
        }

        /**
         * Set the reference images URLs
         * @param refImagesUrl List of reference images URLs
         */
        public void setRefImagesUrl(List<String> refImagesUrl) {
            this.refImagesUrl = refImagesUrl;
        }

        /**
         * Get the image URL
         * @return The image URL
         */
        public String getImgUrl() {
            return imgUrl;
        }

        /**
         * Set the image URL
         * @param imgUrl The image URL
         */
        public void setImgUrl(String imgUrl) {
            this.imgUrl = imgUrl;
        }

        /**
         * Get the first frame URL
         * @return The first frame URL
         */
        public String getFirstFrameUrl() {
            return firstFrameUrl;
        }

        /**
         * Set the first frame URL
         * @param firstFrameUrl The first frame URL
         */
        public void setFirstFrameUrl(String firstFrameUrl) {
            this.firstFrameUrl = firstFrameUrl;
        }

        /**
         * Get the last frame URL
         * @return The last frame URL
         */
        public String getLastFrameUrl() {
            return lastFrameUrl;
        }

        /**
         * Set the last frame URL
         * @param lastFrameUrl The last frame URL
         */
        public void setLastFrameUrl(String lastFrameUrl) {
            this.lastFrameUrl = lastFrameUrl;
        }

        /**
         * Get the template
         * @return The template string
         */
        public String getTemplate() {
            return template;
        }

        /**
         * Set the template
         * @param template The template string
         */
        public void setTemplate(String template) {
            this.template = template;
        }

        /**
         * Get the reference video URLs
         * @return List of reference video URLs
         */
        public List<String> getReferenceVideoUrl() {
            return referenceVideoUrl;
        }

        /**
         * Set the reference video URLs
         * @param referenceVideoUrl List of reference video URLs
         */
        public void setReferenceVideoUrl(List<String> referenceVideoUrl) {
            this.referenceVideoUrl = referenceVideoUrl;
        }

        /**
         * Get the audio URL
         * @return The audio URL
         */
        public String getAudioUrl() {
            return audioUrl;
        }

        /**
         * Set the audio URL
         * @param audioUrl The audio URL
         */
        public void setAudioUrl(String audioUrl) {
            this.audioUrl = audioUrl;
        }
        
        public List<String> getTexts() {
			return texts;
		}
        
        public void setTexts(List<String> texts) {
			this.texts = texts;
		}

        public static Builder builder(){
            return new Builder();
        }

        public static class Builder{

            private Input input = new Input();

            public Builder prompt(String prompt){
                this.input.setPrompt(prompt);
                return this;
            }

            public Builder negativePrompt(String negativePrompt){
                this.input.setNegativePrompt(negativePrompt);
                return this;
            }

            public Input build(){
                return input;
            }
        }
        
    }

    /**
     * Message class representing a message in the conversation
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message{

        @JsonProperty("role")
        private String role;

        @JsonProperty("content")
        private Object content;

        @JsonProperty("tool_call_id")
        private String toolCallId;

        /**
         * Get the role of the message
         * @return The role string
         */
        public String getRole() {
            return role;
        }

        /**
         * Set the role of the message
         * @param role The role string
         */
        public void setRole(String role) {
            this.role = role;
        }

        /**
         * Get the content of the message
         * @return The content string
         */
        public Object getContent() {
            return content;
        }

        /**
         * Set the content of the message
         * @param content The content string
         */
        public void setContent(Object content) {
            this.content = content;
        }

        public String getToolCallId() {
            return toolCallId;
        }

        public void setToolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
        }

        public static Builder builder(){
            return new Builder();
        }

        public static class Builder{

            private Message messageRequest = new Message();

            public Builder content(Object content){
                messageRequest.setContent(content.toString());
                return this;
            }

            public Builder role(String role){
                messageRequest.setRole(role);
                return this;
            }

            public Message build(){
                return messageRequest;
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MediaContent{

        @JsonProperty("text")
        private String text;

        @JsonProperty("image")
        private String image;

        @JsonProperty("video")
        private String video;

        @JsonProperty("fps")
        private Float fps;

        @JsonProperty("min_pixels")
        private Integer minPixels;

        @JsonProperty("max_pixels")
        private Integer maxPixels;

        @JsonProperty("total_pixels")
        private Integer totalPixels;

        @JsonProperty("audio")
        private String audio;

        @JsonProperty("cache_control")
        private Object cacheControl;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public String getVideo() {
            return video;
        }

        public void setVideo(String video) {
            this.video = video;
        }

        public Float getFps() {
            return fps;
        }

        public void setFps(Float fps) {
            this.fps = fps;
        }

        public Integer getMinPixels() {
            return minPixels;
        }

        public void setMinPixels(Integer minPixels) {
            this.minPixels = minPixels;
        }

        public Integer getMaxPixels() {
            return maxPixels;
        }

        public void setMaxPixels(Integer maxPixels) {
            this.maxPixels = maxPixels;
        }

        public Integer getTotalPixels() {
            return totalPixels;
        }

        public void setTotalPixels(Integer totalPixels) {
            this.totalPixels = totalPixels;
        }

        public String getAudio() {
            return audio;
        }

        public void setAudio(String audio) {
            this.audio = audio;
        }

        public Object getCacheControl() {
            return cacheControl;
        }

        public void setCacheControl(Object cacheControl) {
            this.cacheControl = cacheControl;
        }

        public MediaContent() {
        }

        public MediaContent(String text) {
            this.text = text;
        }

        public static Builder builder(){
            return new Builder();
        }

        public static class Builder{
            private MediaContent mediaContent = new MediaContent();

            public Builder text(String text){
                mediaContent.text = text;
                return this;
            }

            public Builder audio(String audio){
                mediaContent.audio = audio;
                return this;
            }

            public Builder image(String image){
                mediaContent.image = image;
                return this;
            }

            public MediaContent build(){
                return mediaContent;
            }
        }
    }

    /**
     * Resolution enum for video/image generation
     */
    public enum Resolution {
        @JsonProperty("720P")
        P720("720P"),
        
        @JsonProperty("1080P")
        P1080("1080P");

        private final String value;

        Resolution(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public enum ShotType{
        @JsonProperty("single")
        SINGLE("single"),
        @JsonProperty("multi")
        MULTI("multi");

        private final String value;

        ShotType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * Parameters class containing the parameters for the request
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Parameters{

        @JsonProperty("size")
        private String size;

        @JsonProperty("n")
        private Integer n;

        @JsonProperty("watermark")
        private Boolean watermark;

        @JsonProperty("seed")
        private Integer seed;

        @JsonProperty("resolution")
        private Resolution resolution;

        @JsonProperty("duration")
        private Integer duration;

        @JsonProperty("prompt_extend")
        private Boolean promptExtend;

        @JsonProperty("shot_type")
        private ShotType shotType;

        @JsonProperty("audio")
        private Boolean audio;

        @JsonProperty("temperature")
        private Double temperature;

        @JsonProperty("top_p")
        private Integer topP;

        @JsonProperty("top_k")
        private Integer topK;

        @JsonProperty("enable_thinking")
        private Boolean enableThinking;

        @JsonProperty("thinking_budget")
        private String thinkingBudget;

        @JsonProperty("tools")
        private List<FunctionTool> tools;

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public Integer getN() {
            return n;
        }

        public void setN(Integer n) {
            this.n = n;
        }

        public Boolean getWatermark() {
            return watermark;
        }

        public void setWatermark(Boolean watermark) {
            this.watermark = watermark;
        }

        public Integer getSeed() {
            return seed;
        }

        public void setSeed(Integer seed) {
            this.seed = seed;
        }

        /**
         * Get the resolution for this request
         * @return The Resolution enum value
         */
        public Resolution getResolution() {
            return resolution;
        }

        /**
         * Set the resolution for this request
         * @param resolution The Resolution enum value
         */
        public void setResolution(Resolution resolution) {
            this.resolution = resolution;
        }

        /**
         * Get the duration for this request
         * @return The duration integer value
         */
        public Integer getDuration() {
            return duration;
        }

        /**
         * Set the duration for this request
         * @param duration The duration integer value
         */
        public void setDuration(Integer duration) {
            this.duration = duration;
        }

        /**
         * Get the prompt extension setting for this request
         * @return The prompt extension boolean value
         */
        public String getPromptExtend() {
            return promptExtend != null ? promptExtend.toString() : null;
        }

        /**
         * Set the prompt extension setting for this request
         * @param promptExtend The prompt extension boolean value
         */
        public void setPromptExtend(Boolean promptExtend) {
            this.promptExtend = promptExtend;
        }

        /**
         * Get the shot type for this request
         * @return The ShotType enum value
         */
        public ShotType getShotType() {
            return shotType;
        }

        /**
         * Set the shot type for this request
         * @param shotType The ShotType enum value
         */
        public void setShotType(ShotType shotType) {
            this.shotType = shotType;
        }

        /**
         * Get the audio setting for this request
         * @return The audio boolean value
         */
        public Boolean getAudio() {
            return audio;
        }

        /**
         * Set the audio setting for this request
         * @param audio The audio boolean value
         */
        public void setAudio(Boolean audio) {
            this.audio = audio;
        }

        /**
         * Get the temperature for this request
         * @return The temperature double value
         */
        public Double getTemperature() {
            return temperature;
        }

        /**
         * Set the temperature for this request
         * @param temperature The temperature double value
         */
        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        /**
         * Get the topP value for this request
         * @return The topP integer value
         */
        public Integer getTopP() {
            return topP;
        }

        /**
         * Set the topP value for this request
         * @param topP The topP integer value
         */
        public void setTopP(Integer topP) {
            this.topP = topP;
        }

        /**
         * Get the topK value for this request
         * @return The topK integer value
         */
        public Integer getTopK() {
            return topK;
        }

        /**
         * Set the topK value for this request
         * @param topK The topK integer value
         */
        public void setTopK(Integer topK) {
            this.topK = topK;
        }

        /**
         * Get the enable thinking setting for this request
         * @return The enable thinking boolean value
         */
        public Boolean getEnableThinking() {
            return enableThinking;
        }

        /**
         * Set the enable thinking setting for this request
         * @param enableThinking The enable thinking boolean value
         */
        public void setEnableThinking(Boolean enableThinking) {
            this.enableThinking = enableThinking;
        }

        /**
         * Get the thinking budget for this request
         * @return The thinking budget string value
         */
        public String getThinkingBudget() {
            return thinkingBudget;
        }

        /**
         * Set the thinking budget for this request
         * @param thinkingBudget The thinking budget string value
         */
        public void setThinkingBudget(String thinkingBudget) {
            this.thinkingBudget = thinkingBudget;
        }

        public List<FunctionTool> getTools() {
            return tools;
        }

        public void setTools(List<FunctionTool> tools) {
            this.tools = tools;
        }

        public static Builder builder(){
            return new Builder();
        }

        public static class Builder{

            private final Parameters parameters = new Parameters();

            public Builder size(String size){
                parameters.setSize(size);
                return this;
            }

            public Builder n(Integer n){
                this.parameters.setN(n);
                return this;
            }

            public Builder promptExtend(Boolean promptExtend){
                this.parameters.setPromptExtend(promptExtend);
                return this;
            }

            public Builder watermark(Boolean watermark){
                this.parameters.setWatermark(watermark);
                return this;
            }

            public Builder seed(Integer seed){
                this.parameters.setSeed(seed);
                return this;
            }

            public Parameters build(){
                return parameters;
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FunctionTool{

        @JsonProperty("type")
        private String type = "function";

        @JsonProperty("function")
        private Function function;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Function getFunction() {
            return function;
        }

        public void setFunction(Function function) {
            this.function = function;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Function{

        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("parameters")
        private String parameters;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getParameters() {
            return parameters;
        }

        public void setParameters(String parameters) {
            this.parameters = parameters;
        }
    }



    public static Builder builder(){
        return new Builder();
    }

    public static class Builder {

        private final DashScopeRequest instance = new DashScopeRequest();

        public Builder model(String model) {
            instance.setModel(model);
            return this;
        }

        public Builder input(Input input){
            instance.setInput(input);
            return this;
        }

        public Builder parameters(Parameters parameters){
            instance.setParameters(parameters);
            return this;
        }
        
        public Builder text(String text) {
        	instance.getInput().setText(text);
        	return this;
        }
        
        public Builder voice(String voice) {
        	instance.getInput().setVoice(voice);
        	return this;
        }

        public Builder prompt(String prompt){
            instance.getInput().setPrompt(prompt);
            return this;
        }

        public Builder imgUrl(String imgUrl){
            instance.getInput().setImgUrl(imgUrl);
            return this;
        }

        public Builder audioUrl(String audioUrl){
            instance.getInput().setAudioUrl(audioUrl);
            return this;
        }

        public Builder resolution(Resolution resolution){
            instance.getParameters().setResolution(resolution);
            return this;
        }

        public Builder duration(Integer duration){
            instance.getParameters().setDuration(duration);
            return this;
        }

        public Builder promptExtend(Boolean promptExtend){
             instance.getParameters().setPromptExtend(promptExtend);
             return this;
         }

        public Builder shotType(ShotType shotType){
            instance.getParameters().setShotType(shotType);
            return this;
        }

        public Builder audio(Boolean audio) {
            instance.getParameters().setAudio(audio);
            return this;
        }

        public Builder temperature(Double temperature) {
             instance.getParameters().setTemperature(temperature);
             return this;
         }

        public Builder topP(Integer topP) {
             instance.getParameters().setTopP(topP);
             return this;
         }

        public Builder topK(Integer topK) {
            instance.getParameters().setTopK(topK);
            return this;
        }

        public Builder enableThinking(Boolean enableThinking) {
            instance.getParameters().setEnableThinking(enableThinking);
            return this;
        }

        public Builder thinkingBudget(String thinkingBudget) {
             instance.getParameters().setThinkingBudget(thinkingBudget);
             return this;
         }

        public Builder messages(List<Message> messages) {
            instance.getInput().setMessages(messages);
            return this;
        }

        public Builder refImagesUrl(String refImagesUrl) {
             instance.getInput().getRefImagesUrl().add(refImagesUrl);
             return this;
         }

        public Builder firstFrameUrl(String firstFrameUrl) {
            instance.getInput().setFirstFrameUrl(firstFrameUrl);
            return this;
        }

        public Builder lastFrameUrl(String lastFrameUrl) {
            instance.getInput().setLastFrameUrl(lastFrameUrl);
            return this;
        }

        public Builder template(String template) {
            instance.getInput().setTemplate(template);
            return this;
        }

        public Builder referenceVideoUrl(String referenceVideoUrl) {
             instance.getInput().getReferenceVideoUrl().add(referenceVideoUrl);
             return this;
         }
        
        public Builder texts(List<String> texts) {
        	instance.getInput().setTexts(texts);
        	return this;
        }

        public DashScopeRequest build() {
            return instance;
        }

    }

}