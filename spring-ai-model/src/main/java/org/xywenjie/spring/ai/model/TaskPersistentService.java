package org.xywenjie.spring.ai.model;

public interface TaskPersistentService {
    void saveTask(TaskContext taskContext);

    void updateTask(TaskContext taskContext);

    void completeTask(TaskContext taskContext);
}
