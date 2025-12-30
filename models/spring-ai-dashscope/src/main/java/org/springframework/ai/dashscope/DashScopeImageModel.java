package org.springframework.ai.dashscope;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.dashscope.api.DashScopeImageApi;
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DashScopeImageModel implements ImageModel {

    private static final Logger logger = LoggerFactory.getLogger(DashScopeImageModel.class);

    private static final String DEFAULT_MODEL = "wanx-v1";

    private final DashScopeImageApi dashScopeImageApi;

    private final DashScopeImageOptions defaultOptions;

    private final RetryTemplate retryTemplate;

    private final ObservationRegistry observationRegistry;

    private final ImageModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultImageModelObservationConvention();
    
    private ImageModelObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

    public DashScopeImageModel(DashScopeImageApi dashScopeImageApi){
        this(dashScopeImageApi,DashScopeImageOptions.builder().build(), RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }

    public DashScopeImageModel(DashScopeImageApi dashScopeImageApi,DashScopeImageOptions options,RetryTemplate retryTemplate){
        this(dashScopeImageApi,options,retryTemplate,ObservationRegistry.NOOP);
    }

    public DashScopeImageModel(DashScopeImageApi dashScopeImageApi,DashScopeImageOptions options,
                               RetryTemplate retryTemplate,ObservationRegistry observationRegistry){
        Assert.notNull(dashScopeImageApi,"dashScopeImageApi must not be null");
        Assert.notNull(options,"options must not be null");
        Assert.notNull(retryTemplate,"retryTemplate must not be null");
        Assert.notNull(observationRegistry,"observationRegistry must not be null");
        this.dashScopeImageApi =dashScopeImageApi;
        this.defaultOptions = options;
        this.retryTemplate = retryTemplate;
        this.observationRegistry = observationRegistry;
    }

    @Override
    public ImageResponse call(ImagePrompt request) {
        Assert.notNull(request, "Prompt must not be null");
        Assert.isTrue(!CollectionUtils.isEmpty(request.getInstructions()), "Prompt messages must not be empty");
        String taskId = submitImageGenTask(request);
        if (taskId == null) {
            return new ImageResponse(List.of(), toMetadataEmpty());
        }
        ImageModelObservationContext observationContext = ImageModelObservationContext.builder()
                .imagePrompt(request)
                .provider("Dash")
                .build();

//        Observation observation = ImageModelObservationDocumentation.IMAGE_MODEL_OPERATION.observation(
//                observationConvention,
//                new DefaultImageModelObservationConvention(),
//                () -> observationContext,
//                this.observationRegistry);

        return ImageModelObservationDocumentation.IMAGE_MODEL_OPERATION
        		.observation(this.observationConvention,DEFAULT_OBSERVATION_CONVENTION,() -> observationContext,
        				this.observationRegistry)
        		.observe(() -> {
        			ResponseEntity<DashScopeImageApi.DashScopeImageResponse> responseEntity = RetryUtils.execute(this.retryTemplate, () -> this.dashScopeImageApi.getImageGenTaskResult(taskId));
        			if(responseEntity.getBody() != null) {
        				DashScopeImageApi.DashScopeImageResponse response = responseEntity.getBody();
        				String status = response.output().taskStatus();
        				switch (status) {
							case "SUCCEEDED" -> {
								return toImageResponse(response);
							}
							case "FAILED","UNKNOWN" -> {
								return new ImageResponse(List.of(),toMetadata(response));
							}
        				}
						throw new TransientAiException("Image generation still pending");
        			}
        			return null;
        		});
    }

    public String submitImageGenTask(ImagePrompt request) {
        DashScopeImageOptions imageOptions = toImageOptions(request.getOptions());
        logger.debug("Image options:{}",imageOptions);

        DashScopeImageApi.ImageRequest imageRequest = constructImageRequest(request,imageOptions);

        HttpEntity<DashScopeImageApi.DashScopeImageResponse> submitResponse = dashScopeImageApi.submitImageGenTask(imageRequest);
        if(submitResponse == null || submitResponse.getBody() == null){
            logger.warn("Submit imageGen error,request:{}",request);
            return null;
        }
        return submitResponse.getBody().output().taskId();
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

    private DashScopeImageApi.DashScopeImageResponse getDashScopeImageGenTask(String taskId){
        HttpEntity<DashScopeImageApi.DashScopeImageResponse> imageResponse = this.dashScopeImageApi.getImageGenTaskResult(taskId);
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

    private ImageResponse toImageResponse(DashScopeImageApi.DashScopeImageResponse asyncResponse){
        DashScopeImageApi.DashScopeImageResponse.Output output = asyncResponse.output();
        var results = output.results();
        ImageResponseMetadata md = toMetadata(asyncResponse);
        List<ImageGeneration> gens = results == null ? List.of() : results.stream().map(r-> new ImageGeneration(new Image(r.url(),null))).toList();
        return new ImageResponse(gens,md);
    }

    private DashScopeImageApi.ImageRequest constructImageRequest(ImagePrompt imagePrompt,DashScopeImageOptions options){
        return new DashScopeImageApi.ImageRequest(
                options.getModel(),
                new DashScopeImageApi.ImageRequest.Input(imagePrompt.getInstructions().get(0).getText(),options.getNegativePrompt()),
                new DashScopeImageApi.ImageRequest.Parameters(options.getSize(),options.getN(),options.getPromptExtend(),options.getWatermark(),options.getSeed()));
    }

    private ImageResponseMetadata toMetadata(DashScopeImageApi.DashScopeImageResponse response){
        var out = response.output();
        var tm = out.taskMetrics();
        var usage = response.usage();
        ImageResponseMetadata imageResponseMetadata = new ImageResponseMetadata();
        Optional.ofNullable(usage).map(DashScopeImageApi.DashScopeImageResponse.Usage::imageCount).ifPresent(count -> imageResponseMetadata.put("imageCount",count));
        Optional.ofNullable(tm).ifPresent(metrics -> {
            imageResponseMetadata.put("taskTotal",metrics.total());
            imageResponseMetadata.put("taskSucceeded",metrics.succeeded());
            imageResponseMetadata.put("taskFailed",metrics.failed());
        });
        imageResponseMetadata.put("requestId",response.requestId());
        imageResponseMetadata.put("taskStatus",out.taskStatus());
        Optional.ofNullable(out.code()).ifPresent(code -> imageResponseMetadata.put("code",code));
        Optional.ofNullable(out.message()).ifPresent(msg -> imageResponseMetadata.put("message",msg));
        return imageResponseMetadata;
    }
}