package org.xywenjie.spring.ai.hunyuan.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ImageResponse extends Response {
	
	@JsonProperty("ResultImage")
	private String resultImage;
	
	@JsonProperty("Seed")
	private Integer seed;
	
	public String getResultImage() {
		return resultImage;
	}
	
	public void setResultImage(String resultImage) {
		this.resultImage = resultImage;
	}
	
	public Integer getSeed() {
		return seed;
	}
	
	public void setSeed(Integer seed) {
		this.seed = seed;
	}

}
