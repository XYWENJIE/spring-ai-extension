package org.springframework.ai.dashscope;

import org.springframework.ai.dashscope.api.DashScopeApi;
import org.springframework.ai.dashscope.api.DashScopeApi.ChatModel;
import org.springframework.ai.dashscope.api.DashScopeImageApi;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.springframework.boot.SpringBootConfiguration;

@SpringBootConfiguration
public class DashScopeTestConfiguration {
	
//	private String getApiKey() {
//		String apiKey = System.getenv("DASHSCOPE_API_KEY");
//		if(!StringUtils.hasText(apiKey)) {
//			throw new IllegalArgumentException("你必须提供一个API密钥。请将其放入名为DASHSCOPE_API_KEY的环境变量中。");
//		}
//		return apiKey;
//	}
//	
//	@Bean
//	public QWenDashScopeService dashScopeService() {
//		return new QWenDashScopeService(getApiKey());
//	}
//
//	@Bean
//	public ImageDashScopeService imageDashScopeService() {
//		return new ImageDashScopeService(getApiKey());
//	}
//
//	@Bean
//	public QWenChatModel qWenChatClient(QWenDashScopeService dashScopeService) {
//		return new QWenChatModel(dashScopeService);
//	}
//	
//	@Bean
//	public QWenImageModel qWenImageClient(ImageDashScopeService imageDashScopeService) {
//		return new QWenImageModel(imageDashScopeService);
//	}
	
//	@Bean
//	public EmbeddingModel qwenEmbeddingClient(DashsCopeService dashCopeService) {
//		return new QWenEmbeddingModel(dashCopeService);
//	}
	
	//以上是历史遗留代码
	@Bean
	public DashScopeApi dashScopeApi() {
		return DashScopeApi.builder().apiKey(getApiKey()).build();
	}

    @Bean
    public DashScopeImageApi dashScopeImageApi(){
        return DashScopeImageApi.builder().apiKey(getApiKey()).build();
    }
	
	private ApiKey getApiKey() {
		String apiKey = System.getenv("DASHSCOPE_API_KEY");
		if(!StringUtils.hasText(apiKey)) {
			throw new IllegalArgumentException("");
		}
		return new SimpleApiKey(apiKey);
	}
	
	@Bean
	public DashScopeChatModel dashScopeChatModel(DashScopeApi dashScopeApi) {
		return DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.defaultOptions(DashScopeChatOptions.builder().model(ChatModel.QWEN_MAX.getName()).build())
				.build();
	}

    @Bean
    public DashScopeImageModel dashScopeImageModel(DashScopeImageApi imageApi){
        return new DashScopeImageModel(imageApi);
    }

}
