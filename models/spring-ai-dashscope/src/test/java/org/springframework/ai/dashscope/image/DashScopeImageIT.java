package org.springframework.ai.dashscope.image;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.dashscope.DashScopeTestConfiguration;
import org.springframework.ai.dashscope.testutils.AbstractIT;
import org.springframework.ai.image.*;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DashScopeTestConfiguration.class)
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
public class DashScopeImageIT extends AbstractIT {

    private static final Logger logger = LoggerFactory.getLogger(DashScopeImageIT.class);

    @Test
    void imageUrlTest(){
        var options = ImageOptionsBuilder.builder().height(1024).width(1024).build();
        String instructions = """
                A light cream colored mini golden doodle with a sign that contains the message "I'm on my way to BARCADE!".
                """;
        ImagePrompt imagePrompt = new ImagePrompt(instructions,options);

        ImageResponse imageResponse = this.imageModel.call(imagePrompt);
        assertThat(imageResponse.getResults()).hasSize(1);

        var generation = imageResponse.getResult();
        Image image = generation.getOutput();
        logger.info(image.getUrl());
    }
}
