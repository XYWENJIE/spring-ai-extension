package org.xywenjie.spring.ai.model;

import java.time.LocalDateTime;

import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.util.Assert;

/**
 * @author 黄文杰
 */
public class JdbcTaskPersistentService implements TaskPersistentService{

    private static final String DEFAULT_TABLE_NAME = "TAB_ASSISTANT_TASK";

    private final String TABLE_NAME;

    private final JdbcClient jdbcClient;

    private String insertSql;

    private String updateSql;

    private String completeSql;

    public JdbcTaskPersistentService(DataSource dataSource){
        this(dataSource,DEFAULT_TABLE_NAME);
    }

    public JdbcTaskPersistentService(DataSource dataSource,String tableName){
        Assert.notNull(dataSource, "dataSource must not be null");
        Assert.notNull(tableName, "tableName must not be null");
        this.TABLE_NAME = tableName;
        this.jdbcClient = JdbcClient.create(dataSource);
        this.insertSql = String.format("INSERT INTO %s (TASK_ID, PROMPT, STATUS, USER_INFO_ID, PROVIDER, MODEL, CREATE_AT) VALUES (?, ?, ?, ?, ?, ?, ?)", TABLE_NAME);
        this.updateSql = String.format("UPDATE %s SET STATUS = ? WHERE TASK_ID = ?", TABLE_NAME);
        this.completeSql = String.format("UPDATE %s SET STATUS = ?, SUCCESS_COUNT = ?, RESULT_URLS = ?, ERROR_CODE = ?, ERROR_MESSAGE = ?, COMPLETE_AT = ? WHERE TASK_ID = ?", TABLE_NAME);
    }

    @Override
    public void saveTask(TaskContext taskContext){
        jdbcClient.sql(insertSql)
            .param(taskContext.getTaskId())
            .param(taskContext.getPrompt())
            .param("PENDING")
            .param(taskContext.getUsername())
            .param(taskContext.getProvider())
            .param(taskContext.getModel())
            .param(LocalDateTime.now())
            .update();
    }

    @Override
    public void updateTask(TaskContext taskContext){
        jdbcClient.sql(updateSql)
        .param(taskContext.getStatus())
        .param(taskContext.getTaskId())
        .update(); 
    }

    @Override
    public void completeTask(TaskContext taskContext){
        String resultUrlsStr = taskContext.getResultUrls() != null 
            ? String.join(",", taskContext.getResultUrls()) 
            : null;
        jdbcClient.sql(completeSql)
            .param(taskContext.getStatus())
            .param(taskContext.getSuccessCount())
            .param(resultUrlsStr)
            .param(taskContext.getErrorCode())
            .param(taskContext.getErrorMessage())
            .param(LocalDateTime.now())
            .param(taskContext.getTaskId())
            .update();
    }

    public void setInsertSql(String insertSql) {
        this.insertSql = insertSql;
    }

    public void setUpdateSql(String updateSql) {
        this.updateSql = updateSql;
    }

    public void setCompleteSql(String completeSql) {
        this.completeSql = completeSql;
    }
}
