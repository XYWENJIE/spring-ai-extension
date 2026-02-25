package org.xywenjie.spring.ai.model;

import java.util.List;

public class TaskContext {

    private String taskId;

    private String model;

    private String username;

    private String prompt;

    private String status;

    private String provider;

    private Integer successCount;

    private List<String> resultUrls;

    private String errorCode;

    private String errorMessage;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public List<String> getResultUrls() {
        return resultUrls;
    }

    public void setResultUrls(List<String> resultUrls) {
        this.resultUrls = resultUrls;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private TaskContext taskContext = new TaskContext();

        public Builder taskId(String taskId) {
            taskContext.setTaskId(taskId);
            return this;
        }

        public Builder model(String model) {
            taskContext.setModel(model);
            return this;
        }

        public Builder prompt(String prompt) {
            taskContext.setPrompt(prompt);
            return this;
        }

        public Builder username(String username) {
            taskContext.setUsername(username);
            return this;
        }

        public Builder provider(String provider) {
            taskContext.setProvider(provider);
            return this;
        }

        public Builder successCount(Integer successCount) {
            taskContext.setSuccessCount(successCount);
            return this;
        }

        public Builder resultUrls(List<String> resultUrls) {
            taskContext.setResultUrls(resultUrls);
            return this;
        }

        public Builder errorCode(String errorCode) {
            taskContext.setErrorCode(errorCode);
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            taskContext.setErrorMessage(errorMessage);
            return this;
        }

        public TaskContext build() {
            return taskContext;
        }
    }
}
