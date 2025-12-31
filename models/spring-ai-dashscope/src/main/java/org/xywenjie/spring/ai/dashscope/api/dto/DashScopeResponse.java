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
        
        @JsonProperty("embeddings")
        private List<Embedding> embeddings;

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
        
        public List<Embedding> getEmbeddings() {
			return embeddings;
		}
        
        public void setEmbeddings(List<Embedding> embeddings) {
			this.embeddings = embeddings;
		}

    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public class Usage {

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
    public class SparseEmbedding{
    	
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
}