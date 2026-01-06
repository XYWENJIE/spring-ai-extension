package org.xywenjie.spring.ai.hunyuan.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;
import org.xywenjie.spring.ai.hunyuan.api.dto.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HunyuanImageApi {
	
	private RestClient restClient;
	
	public ResponseEntity<HunyuanImageResponse> createImage(HunyuanImageRequest request){
		Assert.hasText(request.getPrompt(), "prompt must be set");
		return this.restClient.post().body(request).retrieve().toEntity(HunyuanImageResponse.class);
	}
	
	@JsonInclude(Include.NON_NULL)
	public class HunyuanImageRequest{
		
		@JsonProperty("Prompt")
		private String prompt;
		
		@JsonProperty("NegativePrompt")
		private String negativePrompt;
		
		@JsonProperty("Resolution")
		private String resolution;
		
		@JsonProperty("Seed")
		private Integer seed;
		
		@JsonProperty("LogoAdd")
		private Integer logoAdd;
		
		@JsonProperty("logoParam")
		private LogoParam logoParam;
		
		@JsonProperty("RspImgType")
		private String rspImgType;

		public String getPrompt() {
			return prompt;
		}

		public void setPrompt(String prompt) {
			this.prompt = prompt;
		}

		public String getNegativePrompt() {
			return negativePrompt;
		}

		public void setNegativePrompt(String negativePrompt) {
			this.negativePrompt = negativePrompt;
		}

		public String getResolution() {
			return resolution;
		}

		public void setResolution(String resolution) {
			this.resolution = resolution;
		}

		public Integer getSeed() {
			return seed;
		}

		public void setSeed(Integer seed) {
			this.seed = seed;
		}

		public Integer getLogoAdd() {
			return logoAdd;
		}

		public void setLogoAdd(Integer logoAdd) {
			this.logoAdd = logoAdd;
		}

		public LogoParam getLogoParam() {
			return logoParam;
		}

		public void setLogoParam(LogoParam logoParam) {
			this.logoParam = logoParam;
		}

		public String getRspImgType() {
			return rspImgType;
		}

		public void setRspImgType(String rspImgType) {
			this.rspImgType = rspImgType;
		}
		
	}
	
	@JsonInclude(Include.NON_NULL)
	public class LogoParam{
		
		@JsonProperty("LogoUrl")
		private String logoUrl;
		
		@JsonProperty("LogoImage")
		private String logoImage;
		
		@JsonProperty("LogoRect")
		private LogoRect logoRect;

		public String getLogoUrl() {
			return logoUrl;
		}

		public void setLogoUrl(String logoUrl) {
			this.logoUrl = logoUrl;
		}

		public String getLogoImage() {
			return logoImage;
		}

		public void setLogoImage(String logoImage) {
			this.logoImage = logoImage;
		}

		public LogoRect getLogoRect() {
			return logoRect;
		}

		public void setLogoRect(LogoRect logoRect) {
			this.logoRect = logoRect;
		}
		
	}
	
	@JsonInclude(Include.NON_NULL)
	public class LogoRect{
		
		@JsonProperty("X")
		private Integer x;
		
		@JsonProperty("Y")
		private Integer y;
		
		@JsonProperty("Width")
		private Integer width;
		
		@JsonProperty("Height")
		private Integer height;

		public Integer getX() {
			return x;
		}

		public void setX(Integer x) {
			this.x = x;
		}

		public Integer getY() {
			return y;
		}

		public void setY(Integer y) {
			this.y = y;
		}

		public Integer getWidth() {
			return width;
		}

		public void setWidth(Integer width) {
			this.width = width;
		}

		public Integer getHeight() {
			return height;
		}

		public void setHeight(Integer height) {
			this.height = height;
		}
	}
	
	@JsonInclude(Include.NON_NULL)
	public class HunyuanImageResponse{

		@JsonProperty("Response")
		private Response response;

		public Response getResponse() {
			return response;
		}

		public void setResponse(Response response) {
			this.response = response;
		}
	}

}
