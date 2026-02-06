package org.xywenjie.spring.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.*;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import org.xywenjie.spring.ai.dashscope.DashScopeAudioSpeechModel;
import org.xywenjie.spring.ai.dashscope.DashScopeChatModel;
import org.xywenjie.spring.ai.dashscope.DashScopeChatOptions;
import org.xywenjie.spring.ai.dashscope.DashScopeEmbeddingModel;
import org.xywenjie.spring.ai.dashscope.DashScopeImageModel;
import org.xywenjie.spring.ai.dashscope.api.DashScopeApi;
import org.xywenjie.spring.ai.dashscope.api.DashScopeAudioApi;
import org.xywenjie.spring.ai.dashscope.api.DashScopeImageApi;
import org.xywenjie.spring.ai.dashscope.api.DashScopeApi.ChatModel;
import org.springframework.boot.SpringBootConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@SpringBootConfiguration
public class DashScopeTestConfiguration {

	private static final Logger log = LoggerFactory.getLogger(DashScopeTestConfiguration.class);

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
		return DashScopeApi.builder()
				.apiKey(getApiKey())
				.restClientBuilder(RestClient.builder().requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory())).requestInterceptor(new LoggerRequestInterceptor()))
				.build();
	}

    @Bean
    public DashScopeImageApi dashScopeImageApi(){
        return DashScopeImageApi.builder().apiKey(getApiKey()).build();
    }
    
    @Bean
    DashScopeAudioApi dashScopeAudioApi() {
    	return DashScopeAudioApi.builder().apiKey(getApiKey()).build();
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
	public DashScopeAudioSpeechModel dashScopeAudioSpeechModel(DashScopeAudioApi audioApi) {
		return new DashScopeAudioSpeechModel(audioApi);
	}

    @Bean
    public DashScopeImageModel dashScopeImageModel(DashScopeImageApi imageApi){
        return new DashScopeImageModel(imageApi);
    }
    
    @Bean
    public DashScopeEmbeddingModel dashScopeEmbeddingModel(DashScopeApi api) {
    	return new DashScopeEmbeddingModel(api);
    }

	public class LoggerRequestInterceptor implements ClientHttpRequestInterceptor{

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
			String bodyString  = new String(body);
			//log.info(">>> Request Body: {}",bodyString);
			ClientHttpResponse response = execution.execute(request,body);
			//log.info("<<< Response Status: {}",response.getStatusCode());
			//BufferingClientHttp
			//String responseBody = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8)).lines().reduce("",(a,b) -> a + "\n" + b).strip();
			//log.info("<<< Response Body: {}",responseBody);
			return response;
		}
	}

//	public class LoggerExchangeFilterFunction implements ExchangeFilterFunction{
//
//		@Override
//		public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
//			logStartRequest(request);
//			log.info(">>> Request Headers: {}",request.headers());
//			log.info(">>> Request Body: {}",request.body());
//			return next.exchange(request).flatMap(response -> {
//				log.info("<<< Response Status: {}",response.statusCode());
//				log.info("<<< Response Headers: {}",response.headers());
//
//				if(response.statusCode().value() == HttpStatus.UNAUTHORIZED.value()){
//					log.warn("<<< Received UNAUTHORIZED Response (401)");
//					return Mono.just(response);
//				}else{
//
//					return Mono.just(response);
//				}
//			});
//		}
//
//		private void logStartRequest(ClientRequest request){
//			log.info(">>> [HTTP Request] - Method: {}", request.method());
//			log.info(">>> [HTTP Request] - URI: {}", request.url());
//			//logBodyIfAvailable(request.body(BodyInserterContent.of));
//		}
//
//		private void logBodyIfAvailable(Mono<?> bodyMono,String type){
//			bodyMono.subscribe(body -> log.info("{}:{}",type,body));
//		}
//	}


}
