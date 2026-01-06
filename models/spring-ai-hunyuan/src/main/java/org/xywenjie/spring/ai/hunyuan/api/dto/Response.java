package org.xywenjie.spring.ai.hunyuan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Response {

    @JsonProperty("RequestId")
    private String requestId;

    @JsonProperty("JobId")
    private String jobId;
    
    public String getRequestId() {
		return requestId;
	}
    
    public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
    
    public String getJobId() {
		return jobId;
	}
    
    public void setJobId(String jobId) {
		this.jobId = jobId;
	}
    
}
