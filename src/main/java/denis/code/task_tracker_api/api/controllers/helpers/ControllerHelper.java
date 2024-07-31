package denis.code.task_tracker_api.api.controllers.helpers;


import denis.code.task_tracker_api.api.exceptions.NotFoundException;
import denis.code.task_tracker_api.store.entities.ProjectEntity;
import denis.code.task_tracker_api.store.entities.TaskStateEntity;
import denis.code.task_tracker_api.store.repositories.ProjectRepository;
import denis.code.task_tracker_api.store.repositories.TaskStateRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
@Transactional
public class ControllerHelper {
    ProjectRepository projectRepository;

    TaskStateRepository taskStateRepository;



    public ProjectEntity getProjectOrThrowException(Long projectId) {
        ProjectEntity project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException(String.format("Project with id=%d doesn't exists", projectId)));
        return project;
    }

    public TaskStateEntity getTaskStateOrThrowException(Long taskStateId) {
        return taskStateRepository.findById(taskStateId)
                .orElseThrow(() -> new NotFoundException(String.format("Task state with id=%d doesn't exists", taskStateId)));
    }
}
