package org.xywenjie.spring.ai.model;

import org.springframework.ai.model.ApiKey;

public class AppKeySecret implements ApiKey {
	
	private final String appKey;
	private final String secret;
	
	public AppKeySecret(String appkey,String secret) {
		this.appKey = appkey;
		this.secret = secret;
	}

    @Override
    public String getValue() {
    	return appKey;
    }
    
    public String getSecret() {
    	return secret;
    }
    
    public String toString() {
    	return "AppKeySecret(appKey="+appKey+",Secret=*******)";
    }

}
