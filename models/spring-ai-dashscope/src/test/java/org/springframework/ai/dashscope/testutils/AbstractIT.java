package org.springframework.ai.dashscope.testutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.xywenjie.spring.ai.dashscope.DashScopeAudioSpeechModel;
import org.xywenjie.spring.ai.dashscope.DashScopeChatModel;

public abstract class AbstractIT {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractIT.class);
	
	@Autowired
	protected ChatModel chatModel;
	
	@Autowired
	protected StreamingChatModel streamingChatModel;
	
	@Autowired
	protected DashScopeChatModel dashScopeChatModel;

    @Autowired
    protected ImageModel imageModel;
    
    @Autowired
    protected DashScopeAudioSpeechModel speechModel;

}
