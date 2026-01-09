package org.xywenjie.spring.ai.dashscope.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Huang Wenjie
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeResponse {
	
	@JsonProperty("status_code")
	private Integer statusCode;

    @JsonProperty("output")
    private Output output;

    @JsonProperty("usage")
    private Usage usage;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;
    
    public Integer getStatusCode() {
		return statusCode;
	}
    
    public void setStatusCode(Integer statusCode) {
		this.statusCode = statusCode;
	}
    

    public Output getOutput() {
		return output;
	}

	public void setOutput(Output output) {
		this.output = output;
	}
	
	public Usage getUsage() {
		return usage;
	}
	
	public void setUsage(Usage usage) {
		this.usage = usage;
	}
	
	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

    @JsonInclude(JsonInclude.Include.NON_NULL)
	public class Output {
    	
    	@JsonProperty("text")
    	private String text;
    	
    	@JsonProperty("finish_reason")
    	private String finishReason;
    	
    	@JsonProperty("choices")
    	private String choices;

        @JsonProperty("task_id")
        private String taskId;

        @JsonProperty("task_status")
        private String taskStatus;

        @JsonProperty("submit_time")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT")
        private LocalDateTime submitTime;

        @JsonProperty("scheduled_time")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT")
        private LocalDateTime scheduledTime;

        @JsonProperty("end_time")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT")
        private LocalDateTime endTime;

        @JsonProperty("orig_prompt")
        private String origPrompt;

        @JsonProperty("video_url")
        private String videoUrl;
        
        @JsonProperty("audio")
        private Audio audio;
        
        @JsonProperty("embeddings")
        private List<Embedding> embeddings;

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public String getFinishReason() {
			return finishReason;
		}

		public void setFinishReason(String finishReason) {
			this.finishReason = finishReason;
		}

		public String getChoices() {
			return choices;
		}

		public void setChoices(String choices) {
			this.choices = choices;
		}

		public String getTaskId() {
			return taskId;
		}

		public void setTaskId(String taskId) {
			this.taskId = taskId;
		}

		public String getTaskStatus() {
			return taskStatus;
		}

		public void setTaskStatus(String taskStatus) {
			this.taskStatus = taskStatus;
		}

		public LocalDateTime getSubmitTime() {
			return submitTime;
		}

		public void setSubmitTime(LocalDateTime submitTime) {
			this.submitTime = submitTime;
		}

		public LocalDateTime getScheduledTime() {
			return scheduledTime;
		}

		public void setScheduledTime(LocalDateTime scheduledTime) {
			this.scheduledTime = scheduledTime;
		}

		public LocalDateTime getEndTime() {
			return endTime;
		}

		public void setEndTime(LocalDateTime endTime) {
			this.endTime = endTime;
		}

		public String getOrigPrompt() {
			return origPrompt;
		}

		public void setOrigPrompt(String origPrompt) {
			this.origPrompt = origPrompt;
		}

		public String getVideoUrl() {
			return videoUrl;
		}

		/**
         * 设置视频URL
         * 
         * @param videoUrl 视频的URL地址
         */
        public void setVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }
        
        public Audio getAudio() {
			return audio;
		}

		public void setAudio(Audio audio) {
			this.audio = audio;
		}

		public List<Embedding> getEmbeddings() {
			return embeddings;
		}
        
        public void setEmbeddings(List<Embedding> embeddings) {
			this.embeddings = embeddings;
		}

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class Usage {
    	
    	//输入文本的 Token 消耗量。对于通义千问3-TTS-Flash模型，该字段固定为0
    	@JsonProperty("input_tokens")
    	private Integer inputTokens;
    	
    	@JsonProperty("input_tokens_details")
    	private InputTokensDetails inputTokensDetails;
    	
    	//输出音频的 Token 消耗量。对于通义千问3-TTS-Flash模型，该字段固定为0。
    	@JsonProperty("output_tokens")
    	private Integer outputTokens;
    	
    	@JsonProperty("output_token_details")
    	private OutputTokenDetails outputTokenDetails;
    	
    	//输入文本的字符数。仅通义千问3-TTS-Flash模型返回该字段。
    	@JsonProperty("characters")
    	private Integer characters;

        @JsonProperty("duration")
        private Integer duration;

        @JsonProperty("input_video_duration")
        private Integer inputVideoDuration;

        @JsonProperty("output_video_duration")
        private Integer outputVideoDuration;

        @JsonProperty("video_count")
        private Integer videoCount;

        @JsonProperty("SR")
        private Integer sr;
        
        @JsonProperty("total_tokens")
        private Integer totalTokens;

		public Integer getInputTokens() {
			return inputTokens;
		}

		public void setInputTokens(Integer inputTokens) {
			this.inputTokens = inputTokens;
		}

		public InputTokensDetails getInputTokensDetails() {
			return inputTokensDetails;
		}

		public void setInputTokensDetails(InputTokensDetails inputTokensDetails) {
			this.inputTokensDetails = inputTokensDetails;
		}

		public Integer getOutputTokens() {
			return outputTokens;
		}

		public void setOutputTokens(Integer outputTokens) {
			this.outputTokens = outputTokens;
		}

		public OutputTokenDetails getOutputTokenDetails() {
			return outputTokenDetails;
		}

		public void setOutputTokenDetails(OutputTokenDetails outputTokenDetails) {
			this.outputTokenDetails = outputTokenDetails;
		}

		public Integer getCharacters() {
			return characters;
		}

		public void setCharacters(Integer characters) {
			this.characters = characters;
		}

		public Integer getDuration() {
			return duration;
		}

		public void setDuration(Integer duration) {
			this.duration = duration;
		}

		public Integer getInputVideoDuration() {
			return inputVideoDuration;
		}

		public void setInputVideoDuration(Integer inputVideoDuration) {
			this.inputVideoDuration = inputVideoDuration;
		}

		public Integer getOutputVideoDuration() {
			return outputVideoDuration;
		}

		public void setOutputVideoDuration(Integer outputVideoDuration) {
			this.outputVideoDuration = outputVideoDuration;
		}

		public Integer getVideoCount() {
			return videoCount;
		}

		public void setVideoCount(Integer videoCount) {
			this.videoCount = videoCount;
		}

		public Integer getSr() {
			return sr;
		}

		public void setSr(Integer sr) {
			this.sr = sr;
		}
		
		public Integer getTotalTokens() {
			return totalTokens;
		}
		
		public void setTotalTokens(Integer totalTokens) {
			this.totalTokens = totalTokens;
		}
        
    }
    
    @JsonInclude(Include.NON_NULL)
    public static class InputTokensDetails{
    	
    	@JsonProperty("text_tokens")
    	private Integer textTokens;
    	
    	public Integer getTextTokens() {
			return textTokens;
		}
    	
    	public void setTextTokens(Integer textTokens) {
			this.textTokens = textTokens;
		}
    }
    
    @JsonInclude(Include.NON_NULL)
    public static class OutputTokensDetails{
    	
    	@JsonProperty("audio_tokens")
    	private Integer audioTokens;
    	
    	@JsonProperty("text_tokens")
    	private Integer textTokens;
    	
    	public Integer getAudioTokens() {
			return audioTokens;
		}
    	
    	public void setAudioTokens(Integer audioTokens) {
			this.audioTokens = audioTokens;
		}
    	
    	public Integer getTextTokens() {
			return textTokens;
		}
    	
    	public void setTextTokens(Integer textTokens) {
			this.textTokens = textTokens;
		}
    }
    
    @JsonInclude(Include.NON_NULL)
    public static class Embedding{
    	
    	@JsonProperty("sparse_embedding")
    	private List<SparseEmbedding> sparseEmbedding;
    	
    	@JsonProperty("embedding")
    	private float[] embedding;
    	
    	@JsonProperty("text_index")
    	private Integer textIndex;
    	
    	public float[] getEmbedding() {
			return embedding;
		}
    	
    	public void setEmbedding(float[] embedding) {
			this.embedding = embedding;
		}
    	
    	public Integer getTextIndex() {
			return textIndex;
		}
    	
    	public void setTextIndex(Integer textIndex) {
			this.textIndex = textIndex;
		}
    }
    
    @JsonInclude(Include.NON_NULL)
    public static class SparseEmbedding{
    	
    	@JsonProperty("index")
    	private Integer index;
    	
    	@JsonProperty("value")
    	private Float value;
    	
    	@JsonProperty("token")
    	private String token;

		public Integer getIndex() {
			return index;
		}

		public void setIndex(Integer index) {
			this.index = index;
		}

		public Float getValue() {
			return value;
		}

		public void setValue(Float value) {
			this.value = value;
		}

		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}
    }
    
    @JsonInclude(Include.NON_NULL)
    public static class Audio{
    	
    	@JsonProperty("url")
    	private String url;
    	
    	@JsonProperty("data")
    	private String data;
    	
    	@JsonProperty("id")
    	private String id;
    	
    	@JsonProperty("expires_at")
    	private Integer expiresAt;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public Integer getExpiresAt() {
			return expiresAt;
		}

		public void setExpiresAt(Integer expiresAt) {
			this.expiresAt = expiresAt;
		}
    }
    
    public static class OutputTokenDetails{
    	
    }
}