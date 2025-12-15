package org.xywenjie.spring.ai.externsion.image;

import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;

public interface AsyncImageModel extends ImageModel {

    String submitImageGenTask(ImagePrompt imagePrompt);

    ImageResponse getImageGenTask(String taskId);
}
