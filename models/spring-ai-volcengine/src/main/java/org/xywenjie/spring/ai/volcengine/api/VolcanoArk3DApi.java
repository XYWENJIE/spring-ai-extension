package org.xywenjie.spring.ai.volcengine.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author huang Wenjie
 */
public class VolcanoArk3DApi {

    private static final String DEFAULT_BASE_URL = "https://ark.cn-beijing.volces.com";

    private final RestClient restClient;

    public VolcanoArk3DApi(String apiKey){
        this(apiKey,DEFAULT_BASE_URL,RestClient.builder());
    }

    public VolcanoArk3DApi(String apiKey,String baseUrl,RestClient.Builder restClientBuilder){
        Assert.notNull(apiKey,"'apiKey' must not be null");
        Assert.notNull(baseUrl,"'baseUrl' must not be null");
        Assert.notNull(restClientBuilder,"'restClientBuilder' must not be null");
        Consumer<HttpHeaders> jsonContentHeaders = headers -> {
          headers.setBearerAuth(apiKey);
          headers.setAccept(List.of(MediaType.APPLICATION_JSON));
          headers.setContentType(MediaType.APPLICATION_JSON);
        };
        this.restClient = restClientBuilder.baseUrl(baseUrl)
                .defaultHeaders(jsonContentHeaders)
                .defaultStatusHandler(RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER)
                .build();
    }

    public HttpEntity<Response> submit3DModelGenTask(Request request){
        return this.restClient.post().uri("/api/v3/contents/generations/tasks").body(request).retrieve().toEntity(Response.class);
    }

    public HttpEntity<Response> get3DModelGenTaskResult(String id){
        return this.restClient.get().uri("/api/v3/contents/generations/tasks/{id}",id).retrieve().toEntity(Response.class);
    }

    public void list3DModelTask(){
        this.restClient.get().uri("/api/v3/contents/generations/tasks?page_num={page_num}&page_size={page_size}&filter.status={filter.status}&filter.task_ids={filter.task_ids}&filter.model={filter.model}");
    }

    public record Request(
            @JsonProperty("model") String model,
            @JsonProperty("content") Content content){}

    public record Content(
            @JsonProperty("file_url") String fileUrl,
            @JsonProperty("type") String type,
            @JsonProperty("image_url") ImageUrl imageUrl,
            @JsonProperty("text") String text){}

    private record ImageUrl(@JsonProperty("url") String url){}

    public record Response(
            @JsonProperty("id") String id,
            @JsonProperty("model") String model,
            @JsonProperty("status") String status,
            @JsonProperty("error") Object error,
            @JsonProperty("created_at") Integer createdAt,
            @JsonProperty("updated_at") Integer updatedAt,
            @JsonProperty("content") Content content,
            @JsonProperty("subdivisionlevel") String subdivisionLevel,
            @JsonProperty("fileformat") String fileFormat,
            @JsonProperty("usage") Object usage){}
}
