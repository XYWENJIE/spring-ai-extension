package org.xywenjie.spring.ai.dashscope.metadata;

import java.time.Duration;

import org.springframework.ai.chat.metadata.RateLimit;

/**
 * 19:36:04.174 [main] INFO org.xywenjie.spring.ai.audio.api.DashScopeAudioApiIT -- Heeader:vary:[Origin,Access-Control-Request-Method,Access-Control-Request-Headers, Accept-Encoding]
19:36:04.179 [main] INFO org.xywenjie.spring.ai.audio.api.DashScopeAudioApiIT -- Heeader:content-type:[application/json]
19:36:04.179 [main] INFO org.xywenjie.spring.ai.audio.api.DashScopeAudioApiIT -- Heeader:x-request-id:[4bd0d057-0837-4283-bd61-d3890482d44d]
19:36:04.179 [main] INFO org.xywenjie.spring.ai.audio.api.DashScopeAudioApiIT -- Heeader:x-dashscope-call-gateway:[true]
19:36:04.179 [main] INFO org.xywenjie.spring.ai.audio.api.DashScopeAudioApiIT -- Heeader:x-dashscope-finished:[true]
19:36:04.179 [main] INFO org.xywenjie.spring.ai.audio.api.DashScopeAudioApiIT -- Heeader:x-dashscope-timeout:[298]
19:36:04.179 [main] INFO org.xywenjie.spring.ai.audio.api.DashScopeAudioApiIT -- Heeader:req-cost-time:[1117]
19:36:04.179 [main] INFO org.xywenjie.spring.ai.audio.api.DashScopeAudioApiIT -- Heeader:req-arrive-time:[1767785762648]
19:36:04.179 [main] INFO org.xywenjie.spring.ai.audio.api.DashScopeAudioApiIT -- Heeader:resp-start-time:[1767785763766]
19:36:04.179 [main] INFO org.xywenjie.spring.ai.audio.api.DashScopeAudioApiIT -- Heeader:x-envoy-upstream-service-time:[1044]
19:36:04.179 [main] INFO org.xywenjie.spring.ai.audio.api.DashScopeAudioApiIT -- Heeader:date:[Wed, 07 Jan 2026 11:36:03 GMT]
19:36:04.179 [main] INFO org.xywenjie.spring.ai.audio.api.DashScopeAudioApiIT -- Heeader:server:[istio-envoy]
19:36:04.179 [main] INFO org.xywenjie.spring.ai.audio.api.DashScopeAudioApiIT -- Heeader:transfer-encoding:[chunked]
 */
public class DashScopeRateLimit implements RateLimit{

	@Override
	public Long getRequestsLimit() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getRequestsRemaining() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Duration getRequestsReset() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getTokensLimit() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getTokensRemaining() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Duration getTokensReset() {
		// TODO Auto-generated method stub
		return null;
	}

}
