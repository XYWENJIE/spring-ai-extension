package org.springframework.ai.autoconfigure.dashscope;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.xywenjie.spring.ai.dashscope.DashScopeChatOptions;
import org.xywenjie.spring.ai.dashscope.metadata.support.Model;

@ConfigurationProperties(DashscopeProperties.CONFIG_PREFIX)
public class DashscopeProperties extends DashscopeParentProperties {

    public static final String CONFIG_PREFIX = "spring.ai.dashscope.qwen.chat";

    private static final Model DEFAULT_CHAT_MODEL = Model.QWen_TURBO;

    private static final Double DEFAULT_TEMPERATURE = 0.7;

    private boolean enabled = true;

    @NestedConfigurationProperty
    private DashScopeChatOptions options = DashScopeChatOptions.builder()
            .model(DEFAULT_CHAT_MODEL.getModelValue())
            //.withTemperature(DEFAULT_TEMPERATURE.floatValue())
            .build();

    public DashScopeChatOptions getOptions(){
        return options;
    }

    public void setOptions(DashScopeChatOptions options){
        this.options = options;
    }

    public boolean isEnabled(){
        return this.enabled;
    }

    public void setEnabled(boolean enabled){
        this.enabled = enabled;
    }
}
