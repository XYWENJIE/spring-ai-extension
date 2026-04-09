package org.xywenjie.spring.ai.dashscope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.xywenjie.spring.ai.dashscope.api.DashScopeApi;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeDefinition;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeRequest;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * DashScope 音频转录模型实现类，用于对接阿里云 DashScope AI 服务的语音识别能力。
 *
 * <p>支持功能：</p>
 * <ul>
 *   <li>语音转文字（ASR）</li>
 *   <li>支持多种音频格式（FLAC、MP3、WAV 等）</li>
 *   <li>支持提示词引导识别</li>
 *   <li>支持语言配置</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * DashScopeAudioTranscriptionModel transcriptionModel = new DashScopeAudioTranscriptionModel(
 *     dashScopeApi,
 *     DashScopeAudioTranscriptionOptions.builder().model("qwen3-asr-flash").build()
 * );
 *
 * AudioTranscriptionResponse response = transcriptionModel.call(
 *     new AudioTranscriptionPrompt(audioResource)
 * );
 * String text = response.getResult().getOutput();
 * }</pre>
 *
 * @author 黄文杰
 * @since 1.0.0
 * @see TranscriptionModel
 * @see DashScopeApi
 */
public class DashScopeAudioTranscriptionModel implements TranscriptionModel {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final DashScopeApi dashScopeApi;

    private final DashScopeAudioTranscriptionOptions defaultOptions;

    private final RetryTemplate retryTemplate;

    private final Map<String,String> AUDIO_MIME_TYPES = Map.of(
            ".flac","audio/flac"
    );

    /**
     * 使用默认模型构造音频转录模型。
     *
     * @param dashScopeApi DashScope API 客户端
     */
    public DashScopeAudioTranscriptionModel(DashScopeApi dashScopeApi){
        this(dashScopeApi,DashScopeAudioTranscriptionOptions.builder().model("qwen3-asr-flash").build());
    }

    /**
     * 使用自定义选项构造音频转录模型。
     *
     * @param dashScopeApi DashScope API 客户端
     * @param options 转录选项
     */
    public DashScopeAudioTranscriptionModel(DashScopeApi dashScopeApi,DashScopeAudioTranscriptionOptions options){
        this(dashScopeApi,options, RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }

    /**
     * 使用自定义选项和重试策略构造音频转录模型。
     *
     * @param dashScopeApi DashScope API 客户端
     * @param options 转录选项
     * @param retryTemplate 重试模板
     */
    public DashScopeAudioTranscriptionModel(DashScopeApi dashScopeApi,DashScopeAudioTranscriptionOptions options,
                                            RetryTemplate retryTemplate){
        Assert.notNull(dashScopeApi,"DashScopeApi must not be null");
        Assert.notNull(options,"DashScopeAudioTranscriptionOptions must not be null");
        Assert.notNull(retryTemplate,"RetryTemplate must not be null");
        this.dashScopeApi = dashScopeApi;
        this.defaultOptions = options;
        this.retryTemplate = retryTemplate;
    }

    /**
     * 识别音频内容并返回文本。
     *
     * @param audioResource 音频资源
     * @return 识别出的文本内容
     */
    public String call(Resource audioResource){
        AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(audioResource);
        return call(transcriptionRequest).getResult().getOutput();
    }

    /**
     * 执行音频转录。
     *
     * @param transcriptionPrompt 转录请求
     * @return 转录响应
     */
    @Override
    public AudioTranscriptionResponse call(AudioTranscriptionPrompt transcriptionPrompt) {
        Resource audioResource = transcriptionPrompt.getInstructions();
        DashScopeRequest dashScopeRequest = createRequest(transcriptionPrompt);
        ResponseEntity<DashScopeResponse> transcriptionEntity = this.dashScopeApi.multimodel(dashScopeRequest);
        var transcription = transcriptionEntity.getBody();
        List<DashScopeResponse.MediaContent> mediaContent = (List<DashScopeResponse.MediaContent>) transcription.getOutput().getChoices().get(0).getMessage().getContent();
        AudioTranscription transcript = new AudioTranscription(mediaContent.get(0).getText());
        return new AudioTranscriptionResponse(transcript);
    }

    DashScopeRequest createRequest(AudioTranscriptionPrompt transcriptionPrompt){
        DashScopeAudioTranscriptionOptions options = this.defaultOptions;
        if(transcriptionPrompt.getOptions() instanceof DashScopeAudioTranscriptionOptions runtimeOptions){
            options = this.merge(runtimeOptions,options);
        }
        Resource instructions = transcriptionPrompt.getInstructions();
        String audio = toBase64OrLocation(instructions, options.getModel());
        List<DashScopeRequest.Message> messages = new ArrayList<>();
        if(options.getPrompt() != null){
            messages.add(DashScopeRequest.Message.builder().role(DashScopeDefinition.Role.SYSTEM).content(List.of(DashScopeRequest.MediaContent.builder().text(options.getPrompt()).build())).build());
        }
        messages.add(DashScopeRequest.Message.builder().role(DashScopeDefinition.Role.USER).content(List.of(DashScopeRequest.MediaContent.builder().audio(audio).build())).build());
        DashScopeRequest.AsrOptions asrOptions = new DashScopeRequest.AsrOptions();
        if(options.getLanguage() != null){
            asrOptions.setLanguage(options.getLanguage());
        }
        return DashScopeRequest.builder()
                .model(options.getModel())
                .messages(messages)
                .parameters(DashScopeRequest.Parameters.builder().asrOptions(asrOptions).build())
                .build();
    }

    private DashScopeAudioTranscriptionOptions merge(DashScopeAudioTranscriptionOptions source,DashScopeAudioTranscriptionOptions target){
        if(source == null){
            source = new DashScopeAudioTranscriptionOptions();
        }
        DashScopeAudioTranscriptionOptions merged = new DashScopeAudioTranscriptionOptions();
        merged.setModel(source.getModel() != null ? source.getModel() : target.getModel());
        merged.setLanguage(source.getLanguage() != null ? source.getLanguage() : target.getLanguage());
        merged.setPrompt(source.getPrompt() != null ? source.getPrompt() : target.getPrompt());
        return merged;
    }

    private String toBase64OrLocation(Resource audioResource,String model){
        if(StringUtils.hasText(model) && model.equals("qwen-audio-asr")){
            logger.debug("Uploading audio file to OSS for model: {}", model);
            return fileUpdate(audioResource,model);
        }
        try(InputStream inputStream = audioResource.getInputStream()){
            byte[] types = inputStream.readAllBytes();
            String base64 = Base64.getEncoder().encodeToString(types);
            String filename = audioResource.getFilename();
            String extension = "";
            int dotIndex = filename.lastIndexOf(".");
            extension = filename.substring(dotIndex).toLowerCase();
            String mimeType  = AUDIO_MIME_TYPES.get(extension);
            return String.format("data:%s;base64,%s",mimeType,base64);
        }catch (IOException e){
            logger.error(e.getMessage(),e);
            throw new IllegalArgumentException("Failed to encode audio resource to base64");
        }

    }


    private String fileUpdate(Resource resource,String modelName){
        return this.dashScopeApi.uploadsTemplateUrl(resource,modelName);
    }


}
