package denis.code.task_tracker_api.api.factories;

import denis.code.task_tracker_api.api.dto.TaskStateDto;
import denis.code.task_tracker_api.store.entities.TaskStateEntity;
import org.springframework.stereotype.Component;


@Component
public class TaskStateDtoFactory {
    public TaskStateDto makeTaskStateDto(TaskStateEntity taskStateEntity){
        return TaskStateDto.builder()
                .id(taskStateEntity.getId())
                .name(taskStateEntity.getName())
                .createdAt(taskStateEntity.getCreatedAt())
                .ordinal(taskStateEntity.getOrdinal())
                .build();
    }
}
