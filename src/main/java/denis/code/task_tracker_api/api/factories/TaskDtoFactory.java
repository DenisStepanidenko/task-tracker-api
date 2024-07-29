package denis.code.task_tracker_api.api.factories;


import denis.code.task_tracker_api.api.dto.TaskDto;
import denis.code.task_tracker_api.store.entities.TaskEntity;
import org.springframework.stereotype.Component;

@Component
public class TaskDtoFactory {
    public TaskDto makeTaskDto(TaskEntity taskEntity){
        return TaskDto.builder()
                .id(taskEntity.getId())
                .name(taskEntity.getName())
                .createdAt(taskEntity.getCreatedAt())
                .description(taskEntity.getDescription())
                .build();
    }
}
