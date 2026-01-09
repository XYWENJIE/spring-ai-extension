package org.xywenjie.spring.ai.dashscope;

import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.metadata.RateLimit;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeRequest;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeResponse;
import org.xywenjie.spring.ai.dashscope.metadata.DashScopeAudioSpeechResponseMetadata;
import org.xywenjie.spring.ai.dashscope.metadata.DashScopeResponseHeaderExtractor;
import org.xywenjie.spring.ai.dashscope.api.DashScopeAudioApi;

import reactor.core.publisher.Flux;

/**
 * DashScope音频语音模型实现
 * 实现TextToSpeechModel接口，提供文本转语音功能
 * 
 * @author Huang Wenjie(黄文杰)
 */
public class DashScopeAudioSpeechModel implements TextToSpeechModel {

	/**
	 * 日志记录器
	 */
	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * DashScope音频API客户端
	 */
	private final DashScopeAudioApi audioApi;

	/**
	 * 默认音频语音选项
	 */
	private final DashScopeAudioSpeechOptions defaultOptions;

	/**
	 * 重试模板
	 */
	private final RetryTemplate retryTemplate;
	
	public DashScopeAudioSpeechModel(DashScopeAudioApi audioApi) {
		this(audioApi,DashScopeAudioSpeechOptions.builder().model("qwen3-tts-flash").voice("Cherry").build());
	}
	
	public DashScopeAudioSpeechModel(DashScopeAudioApi audioApi,DashScopeAudioSpeechOptions options) {
		this(audioApi,options,RetryUtils.DEFAULT_RETRY_TEMPLATE);
	}

	/**
	 * 构造函数
	 * @param audioApi DashScope音频API客户端
	 * @param options 音频语音选项
	 * @param retryTemplate 重试模板
	 */
	public DashScopeAudioSpeechModel(DashScopeAudioApi audioApi, DashScopeAudioSpeechOptions options,
			RetryTemplate retryTemplate) {
		Assert.notNull(audioApi, "");
		this.audioApi = audioApi;
		this.defaultOptions = options;
		this.retryTemplate = retryTemplate;
	}

	/**
	 * 将文本转换为语音（同步调用）
	 * @param text 要转换的文本
	 * @return 语音字节数组
	 */
	@Override
	public byte[] call(String text) {
		TextToSpeechPrompt prompt = new TextToSpeechPrompt(text);
		return call(prompt).getResult().getOutput();
	}

	/**
	 * 将文本转换为语音（同步调用，使用提示）
	 * @param prompt 文本转语音提示
	 * @return 文本转语音响应
	 */
	@Override
	public TextToSpeechResponse call(TextToSpeechPrompt prompt) {
		DashScopeRequest dashScopeRequest = createRequest(prompt);
		ResponseEntity<DashScopeResponse> speechEntity = RetryUtils.execute(this.retryTemplate,
				() -> this.audioApi.createSpeech(dashScopeRequest));
		
		var speechResponse = speechEntity.getBody();
		if(speechResponse == null) {
			logger.warn("No speech response returned for speechRequest: {}",speechResponse);
			return new TextToSpeechResponse(List.of(new Speech(new byte[0])));
		}
		//if(speechResponse.get)
		RateLimit rateLimit = DashScopeResponseHeaderExtractor.extractAiResponseHeaders(speechEntity);
		try(InputStream in = new URL(speechResponse.getOutput().getAudio().getUrl()).openStream()){
			return new TextToSpeechResponse(List.of(new Speech(in.readAllBytes())),new DashScopeAudioSpeechResponseMetadata(rateLimit));
		}catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return new TextToSpeechResponse(List.of(new Speech(new byte[0])));
	}

	/**
	 * 流式将文本转换为语音
	 * @param prompt 文本转语音提示
	 * @return 流式文本转语音响应
	 */
	@Override
	public Flux<TextToSpeechResponse> stream(TextToSpeechPrompt prompt) {
		DashScopeRequest speechRequest = createRequest(prompt);

		Flux<ResponseEntity<DashScopeResponse>> speechEntity = RetryUtils.execute(this.retryTemplate,
				() -> this.audioApi.stream(speechRequest));
		return speechEntity.map(entity -> {
			String base64Data = entity.getBody().getOutput().getAudio().getData();
			byte[] audioBytes = Base64.getDecoder().decode(base64Data);
			return new TextToSpeechResponse(List.of(new Speech(audioBytes)),
					new DashScopeAudioSpeechResponseMetadata(DashScopeResponseHeaderExtractor.extractAiResponseHeaders(entity)));
		});
	}

	/**
	 * 创建DashScope请求
	 * @param prompt 文本转语音提示
	 * @return DashScope请求对象
	 */
	private DashScopeRequest createRequest(TextToSpeechPrompt prompt) {
		DashScopeAudioSpeechOptions runtimeOptions = (prompt
				.getOptions() instanceof DashScopeAudioSpeechOptions dashScopeAudioSpeechOptions)
						? dashScopeAudioSpeechOptions
						: null;
		DashScopeAudioSpeechOptions options = (runtimeOptions != null) ? this.merge(runtimeOptions, this.defaultOptions)
				: this.defaultOptions;
		String text = StringUtils.hasText(options.getText()) ? options.getText() : prompt.getInstructions().getText();
		
		DashScopeRequest.Builder responseBuilder = DashScopeRequest.builder();
		responseBuilder.model(options.getModel());
		responseBuilder.text(text);
		responseBuilder.voice(options.getVoice());
		return responseBuilder.build();

	}
	
	/**
	 * 合并两个音频语音选项
	 * @param source 源选项（优先级高）
	 * @param target 目标选项（优先级低）
	 * @return 合并后的选项
	 */
	private DashScopeAudioSpeechOptions merge(DashScopeAudioSpeechOptions source,DashScopeAudioSpeechOptions target) {
		DashScopeAudioSpeechOptions.Builder mergedBuilder = DashScopeAudioSpeechOptions.builder();
		
		mergedBuilder.model(source.getModel() != null ? source.getModel() : target.getModel());
		mergedBuilder.text(source.getText() != null ? source.getText(): target.getText());
		return mergedBuilder.build();
	}
}
