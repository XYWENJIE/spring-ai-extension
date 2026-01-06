package org.xywenjie.spring.ai.hunyuan.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryImageResponse extends Response {

    @JsonProperty("JobStatusCode")
    private String jobStatusCode;

    @JsonProperty("JobStatusMsg")
    private String jobStatusMsg;

    @JsonProperty("JobErrorCode")
    private String jobErrorCode;

    @JsonProperty("JobErrorMsg")
    private String jobErrorMsg;

    @JsonProperty("ResultImage")
    private List<String> resultImage;

    @JsonProperty("ResultDetails")
    private List<String> resultDetails;

    @JsonProperty("RevisedPrompt")
    private List<String> revisedPrompt;

	public String getJobStatusCode() {
		return jobStatusCode;
	}

	public void setJobStatusCode(String jobStatusCode) {
		this.jobStatusCode = jobStatusCode;
	}

	public String getJobStatusMsg() {
		return jobStatusMsg;
	}

	public void setJobStatusMsg(String jobStatusMsg) {
		this.jobStatusMsg = jobStatusMsg;
	}

	public String getJobErrorCode() {
		return jobErrorCode;
	}

	public void setJobErrorCode(String jobErrorCode) {
		this.jobErrorCode = jobErrorCode;
	}

	public String getJobErrorMsg() {
		return jobErrorMsg;
	}

	public void setJobErrorMsg(String jobErrorMsg) {
		this.jobErrorMsg = jobErrorMsg;
	}

	public List<String> getResultImage() {
		return resultImage;
	}

	public void setResultImage(List<String> resultImage) {
		this.resultImage = resultImage;
	}

	public List<String> getResultDetails() {
		return resultDetails;
	}

	public void setResultDetails(List<String> resultDetails) {
		this.resultDetails = resultDetails;
	}

	public List<String> getRevisedPrompt() {
		return revisedPrompt;
	}

	public void setRevisedPrompt(List<String> revisedPrompt) {
		this.revisedPrompt = revisedPrompt;
	}
}