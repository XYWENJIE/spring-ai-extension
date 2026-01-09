package org.xywenjie.spring.ai.dashscope.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.metadata.RateLimit;
import org.springframework.http.ResponseEntity;

public final class DashScopeResponseHeaderExtractor {
	
	private static final Logger logger = LoggerFactory.getLogger(DashScopeResponseHeaderExtractor.class);
	
	public static RateLimit extractAiResponseHeaders(ResponseEntity<?> response) {
		return new DashScopeRateLimit();
	}

}
