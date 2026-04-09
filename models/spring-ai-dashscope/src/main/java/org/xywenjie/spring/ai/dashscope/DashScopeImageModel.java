package org.xywenjie.spring.ai.dashscope;

import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.image.*;
import org.springframework.ai.image.observation.DefaultImageModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationContext;
import org.springframework.ai.image.observation.ImageModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationDocumentation;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.xywenjie.spring.ai.dashscope.api.DashScopeImageApi;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeRequest;
import org.xywenjie.spring.ai.dashscope.api.dto.DashScopeResponse;
import org.xywenjie.spring.ai.model.TaskContext;
import org.xywenjie.spring.ai.model.TaskPersistentService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DashScope 图像生成模型实现类，用于对接阿里云 DashScope AI 服务的图像生成能力。
 *
 * <p>支持功能：</p>
 * <ul>
 *   <li>文生图（Text-to-Image）</li>
 *   <li>异步任务管理</li>
 *   <li>任务状态持久化</li>
 *   <li>可配置的图像参数（尺寸、数量、水印等）</li>
 * </ul>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * DashScopeImageModel imageModel = new DashScopeImageModel(
 *     dashScopeImageApi,
 *     DashScopeImageOptions.builder().model("wanx-v1").size("1024*1024").build(),
 *     retryTemplate,
 *     observationRegistry,
 *     taskPersistentService
 * );
 *
 * ImageResponse response = imageModel.call(new ImagePrompt("一只可爱的猫"));
 * }</pre>
 *
 * @author 黄文杰
 * @since 1.0.0
 * @see ImageModel
 * @see TaskPersistentService
 */
public class DashScopeImageModel implements ImageModel {

    private static final Logger logger = LoggerFactory.getLogger(DashScopeImageModel.class);

    private static final String DEFAULT_MODEL = "wanx-v1";

    private final DashScopeImageApi dashScopeImageApi;

    private final DashScopeImageOptions defaultOptions;

    private final RetryTemplate retryTemplate;

    private final ObservationRegistry observationRegistry;

    private final ImageModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultImageModelObservationConvention();
    
    private final ImageModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

    private final TaskPersistentService taskPersistentService;

    private final Map<String,TaskContext> taskContextMap = new ConcurrentHashMap<>();

    /**
     * 使用默认选项构造图像生成模型。
     *
     * @param dashScopeImageApi DashScope 图像 API 客户端
     */
    public DashScopeImageModel(DashScopeImageApi dashScopeImageApi){
        this(dashScopeImageApi,DashScopeImageOptions.builder().build(), RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }

    /**
     * 使用自定义选项构造图像生成模型。
     *
     * @param dashScopeImageApi DashScope 图像 API 客户端
     * @param options 图像生成选项
     * @param retryTemplate 重试模板
     */
    public DashScopeImageModel(DashScopeImageApi dashScopeImageApi,DashScopeImageOptions options,RetryTemplate retryTemplate){
        this(dashScopeImageApi,options,retryTemplate,ObservationRegistry.NOOP,null);
    }

    /**
     * 使用完整参数构造图像生成模型。
     *
     * @param dashScopeImageApi DashScope 图像 API 客户端
     * @param options 图像生成选项
     * @param retryTemplate 重试模板
     * @param observationRegistry 观测注册表
     * @param taskPersistentService 任务持久化服务（可选）
     */
    public DashScopeImageModel(DashScopeImageApi dashScopeImageApi, DashScopeImageOptions options,
                               RetryTemplate retryTemplate, ObservationRegistry observationRegistry, TaskPersistentService taskPersistentService) {
        Assert.notNull(dashScopeImageApi, "dashScopeImageApi must not be null");
        Assert.notNull(options, "options must not be null");
        Assert.notNull(retryTemplate, "retryTemplate must not be null");
        Assert.notNull(observationRegistry, "observationRegistry must not be null");
        this.dashScopeImageApi = dashScopeImageApi;
        this.defaultOptions = options;
        this.retryTemplate = retryTemplate;
        this.observationRegistry = observationRegistry;
        this.taskPersistentService = taskPersistentService;
    }

    /**
     * 执行图像生成请求。
     *
     * @param request 图像生成提示
     * @return 图像生成响应
     */
    @Override
    public @Nullable ImageResponse call(@NonNull ImagePrompt request) {
        Assert.notNull(request, "Prompt must not be null");
        Assert.isTrue(!CollectionUtils.isEmpty(request.getInstructions()), "Prompt messages must not be empty");
        String taskId = submitImageGenTask(request);
        if (taskId == null) {
            return new ImageResponse(List.of(), toMetadataEmpty());
        }
        ImageModelObservationContext observationContext = ImageModelObservationContext.builder()
                .imagePrompt(request)
                .provider("DashScope")
                .build();

        return ImageModelObservationDocumentation.IMAGE_MODEL_OPERATION
        		.observation(this.observationConvention,DEFAULT_OBSERVATION_CONVENTION,() -> observationContext,
        				this.observationRegistry)
        		.observe(() -> {
        			ResponseEntity<DashScopeResponse> responseEntity = RetryUtils.execute(this.retryTemplate, () -> this.dashScopeImageApi.getImageGenTaskResult(taskId));
        			if(responseEntity.getBody() != null) {
        				DashScopeResponse response = responseEntity.getBody();
        				String status = response.getOutput().getTaskStatus();
                        TaskContext taskContext = taskContextMap.get(taskId);
                        if(taskContext != null && !status.equals(taskContext.getStatus())){
                            taskContext.setStatus(status);
                            this.taskPersistentService.updateTask(taskContext);
                        }
        				switch (status) {
							case "SUCCEEDED" -> {
                                taskContextMap.remove(taskId);
                                int imageCount = response.getUsage().getImageCount();
                                taskContext.setSuccessCount(Optional.of(imageCount).get());
                                taskContext.setStatus("SUCCEEDED");
                                List<String> resultList = new ArrayList<>();
                                for(DashScopeResponse.Result result : response.getOutput().getResults()){
                                    if(StringUtils.hasText(result.getCode())){
                                        resultList.add(result.getMessage());
                                    }else{
                                        resultList.add(result.getUrl());
                                    }
                                }
                                this.taskPersistentService.completeTask(taskContext);
								return toImageResponse(response);
							}
							case "FAILED","UNKNOWN" -> {
                                taskContextMap.remove(taskId);
                                taskContext.setStatus(status);
                                taskContext.setErrorCode(response.getOutput().getCode());
                                taskContext.setErrorMessage(response.getOutput().getMessage());
                                this.taskPersistentService.completeTask(taskContext);
								return new ImageResponse(List.of(),toMetadata(response));
							}
        				}
						throw new TransientAiException("Image generation still pending");
        			}
        			return null;
        		});
    }

    /**
     * 提交图像生成任务。
     *
     * @param request 图像生成提示
     * @return 任务 ID
     */
    public String submitImageGenTask(ImagePrompt request) {
        DashScopeImageOptions imageOptions = toImageOptions(request.getOptions());
        logger.debug("Image options:{}",imageOptions);

        DashScopeRequest imageRequest = constructImageRequest(request,imageOptions);

        HttpEntity<DashScopeResponse> submitResponse = this.dashScopeImageApi.submitImageGenTask(imageRequest);
        if(submitResponse == null || submitResponse.getBody() == null){
            logger.warn("Submit imageGen error,request:{}",request);
            return null;
        }
        DashScopeResponse dashScopeResponse = submitResponse.getBody();
        if(taskPersistentService != null){
            String taskId = dashScopeResponse.getOutput().getTaskId();
            TaskContext taskContext = TaskContext.builder()
                .taskId(taskId)
                .provider("DashScope")
                .model(imageRequest.getModel())
                .prompt(imageRequest.getInput().getPrompt())
                .username(imageOptions.getUsername())
                .build();
            taskContextMap.put(taskId, taskContext);
            taskPersistentService.saveTask(taskContext);
            //记录AI任务
        }
        return dashScopeResponse.getOutput().getTaskId();
    }

    private DashScopeImageOptions toImageOptions(ImageOptions runtimeOptions){

        var currentOptions = DashScopeImageOptions.builder().model(DEFAULT_MODEL).build();//TODO

        if(Objects.nonNull(runtimeOptions)){
            currentOptions = ModelOptionsUtils.copyToTarget(runtimeOptions,ImageOptions.class,DashScopeImageOptions.class);
        }
        if(currentOptions.getModel() == null){
            currentOptions.setModel(DEFAULT_MODEL);
        }
        currentOptions = ModelOptionsUtils.merge(currentOptions,this.defaultOptions,DashScopeImageOptions.class);
        return currentOptions;
    }

    private DashScopeResponse getDashScopeImageGenTask(String taskId){
        HttpEntity<DashScopeResponse> imageResponse = this.dashScopeImageApi.getImageGenTaskResult(taskId);
        if(imageResponse == null || imageResponse.getBody() == null){
            logger.warn("No image response returned for taskId: {}",taskId);
            return null;
        }
        return imageResponse.getBody();
    }

    public ImageResponse getImageGenTask(String taskId) {
        return toImageResponse(Objects.requireNonNull(getDashScopeImageGenTask(taskId)));
    }

    private ImageResponseMetadata toMetadataEmpty(){
        ImageResponseMetadata md = new ImageResponseMetadata();
        md.put("taskStatus","NO_TASK_ID");
        return md;
    }

    private ImageResponseMetadata toMetadataTimeout(String taskId){
        ImageResponseMetadata md = new ImageResponseMetadata();
        md.put("taskId",taskId);
        md.put("taskStatus","TIMED_OUT");
        return md;
    }

    private ImageResponse toImageResponse(DashScopeResponse asyncResponse){
        DashScopeResponse.Output output = asyncResponse.getOutput();
        var results = output.getResults();
        ImageResponseMetadata md = toMetadata(asyncResponse);
        List<ImageGeneration> gens = results == null ? List.of() : results.stream().map(r-> new ImageGeneration(new Image(r.getUrl(),null))).toList();
        return new ImageResponse(gens,md);
    }

    private DashScopeRequest constructImageRequest(ImagePrompt imagePrompt,DashScopeImageOptions options){
        DashScopeRequest.Input input = DashScopeRequest.Input.builder()
                .prompt(imagePrompt.getInstructions().get(0).getText())
                .negativePrompt(options.getNegativePrompt())
                .build();
        DashScopeRequest.Parameters parameters = DashScopeRequest.Parameters.builder()
                .size(options.getSize())
                .n(options.getN())
                .promptExtend(options.getPromptExtend())
                .watermark(options.getWatermark())
                .seed(options.getSeed())
                .build();
        return DashScopeRequest.builder().model(options.getModel()).input(input).parameters(parameters).build();
    }

    private ImageResponseMetadata toMetadata(DashScopeResponse response){
        var out = response.getOutput();
        var tm = out.getTaskMetrics();
        var usage = response.getUsage();
        ImageResponseMetadata imageResponseMetadata = new ImageResponseMetadata();
        Optional.ofNullable(usage).map(DashScopeResponse.Usage::getImageCount).ifPresent(count -> imageResponseMetadata.put("imageCount",count));
        Optional.ofNullable(tm).ifPresent(metrics -> {
            imageResponseMetadata.put("taskTotal",metrics.getTotal());
            imageResponseMetadata.put("taskSucceeded",metrics.getSucceeded());
            imageResponseMetadata.put("taskFailed",metrics.getFailed());
        });
        imageResponseMetadata.put("requestId",response.getRequestId());
        imageResponseMetadata.put("taskStatus",out.getTaskStatus());
        Optional.ofNullable(out.getCode()).ifPresent(code -> imageResponseMetadata.put("code",code));
        Optional.ofNullable(out.getMessage()).ifPresent(msg -> imageResponseMetadata.put("message",msg));
        return imageResponseMetadata;
    }
}