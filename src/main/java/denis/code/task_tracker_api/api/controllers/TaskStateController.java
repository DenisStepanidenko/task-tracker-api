package denis.code.task_tracker_api.api.controllers;


import denis.code.task_tracker_api.api.controllers.helpers.ControllerHelper;
import denis.code.task_tracker_api.api.dto.TaskStateDto;
import denis.code.task_tracker_api.api.exceptions.BadRequestException;
import denis.code.task_tracker_api.api.exceptions.NotFoundException;
import denis.code.task_tracker_api.api.factories.TaskStateDtoFactory;
import denis.code.task_tracker_api.store.entities.ProjectEntity;
import denis.code.task_tracker_api.store.entities.TaskStateEntity;
import denis.code.task_tracker_api.store.repositories.TaskStateRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TaskStateController {
    TaskStateRepository taskStateRepository;
    TaskStateDtoFactory taskStateDtoFactory;

    ControllerHelper controllerHelper;


    public static final String GET_TASK_STATES = "/api/projects/{project_id}/task-states";
    public static final String CREATE_TASK_STATE = "/api/projects/{project_id}/task-states";
    public static final String UPDATE_TASK_STATE = "/api/task-state/{task_state_id}";
    public static final String CHANGE_TASK_STATE_POSITION = "/api/task-state/{task_state_id}/position/change";

    @GetMapping(GET_TASK_STATES)
    public List<TaskStateDto> getTaskStates(@PathVariable(name = "project_id") Long projectId) {
        ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);

        return project
                .getTaskStates()
                .stream()
                .map(taskStateDtoFactory::makeTaskStateDto)
                .collect(Collectors.toList());
    }

    @PostMapping(CREATE_TASK_STATE)
    public TaskStateDto createTaskState(@PathVariable(name = "project_id") Long projectId,
                                        @RequestParam(name = "name") String taskStateName) {

        if (taskStateName.trim().isEmpty()) {
            throw new BadRequestException("Task state name can't be empty");
        }

        ProjectEntity project = controllerHelper.getProjectOrThrowException(projectId);


        Optional<TaskStateEntity> optionalAnotherTaskStateEntity = Optional.empty();


        for (TaskStateEntity taskState : project.getTaskStates()) {
            if (taskState.getName().equalsIgnoreCase(taskStateName)) {
                throw new BadRequestException(String.format("Task state with name=%s already exists", taskStateName));
            }

            if (!taskState.getRightTaskState().isPresent()) {
                optionalAnotherTaskStateEntity = Optional.of(taskState);
            }
        }


        TaskStateEntity taskStateEntity = taskStateRepository.saveAndFlush(TaskStateEntity.builder()
                .name(taskStateName)
                .project(project)
                .build());


        optionalAnotherTaskStateEntity
                .ifPresent(anotherTaskState -> {
                    taskStateEntity.setLeftTaskState(anotherTaskState);
                    anotherTaskState.setRightTaskState(taskStateEntity);

                    taskStateRepository.saveAndFlush(anotherTaskState);
                });


        TaskStateEntity savedTaskStateEntity = taskStateRepository.saveAndFlush(taskStateEntity);

        return taskStateDtoFactory.makeTaskStateDto(savedTaskStateEntity);
    }


    @PatchMapping(UPDATE_TASK_STATE)
    public TaskStateDto updateTaskState(@PathVariable(name = "task_state_id") Long taskStateId,
                                        @RequestParam(name = "name") String taskStateName) {


        if (taskStateName.trim().isEmpty()) {
            throw new BadRequestException("Task state name can't be empty");
        }

        TaskStateEntity taskStateEntity = getTaskStateOrThrowException(taskStateId);

        taskStateRepository.findTaskStateEntityByProjectIdAndNameContainsIgnoreCase(
                        taskStateEntity.getProject().getId(),
                        taskStateName)
                .filter(anotherTaskState -> !anotherTaskState.getId().equals(taskStateId))
                .ifPresent(anotherTaskState -> {
                    throw new BadRequestException(String.format("Task state with name=%s already exists", taskStateName));
                });


        taskStateEntity.setName(taskStateName);
        TaskStateEntity taskState = taskStateRepository.saveAndFlush(taskStateEntity);

        return taskStateDtoFactory.makeTaskStateDto(taskState);


    }

    @PatchMapping(CHANGE_TASK_STATE_POSITION)
    public TaskStateDto changeTaskPosition(@PathVariable(name = "task_state_id") Long taskStateId,
                                           @RequestParam(name = "left_task_state_id", required = false) Optional<Long> optionalLeftTaskStateId) {


        TaskStateEntity changeTaskState = getTaskStateOrThrowException(taskStateId);

        ProjectEntity project = changeTaskState.getProject();

        Optional<Long> optionalOldLeftTaskState = changeTaskState
                .getLeftTaskState()
                .map(TaskStateEntity::getId);

        if (optionalOldLeftTaskState.equals(optionalLeftTaskStateId)) {
            return taskStateDtoFactory.makeTaskStateDto(changeTaskState);
        }


        Optional<TaskStateEntity> optionalNewLeftTaskState = optionalLeftTaskStateId
                .map(leftTaskStateId -> {

                    if (taskStateId.equals(leftTaskStateId)) {
                        throw new BadRequestException("Left task state id equals changed task state id");
                    }

                    TaskStateEntity leftTaskStateEntity = getTaskStateOrThrowException(leftTaskStateId);

                    if (!project.getId().equals(leftTaskStateEntity.getProject().getId())) {
                        throw new BadRequestException("Task state position can be changed within the same project");
                    }
                    return leftTaskStateEntity;
                });


        Optional<TaskStateEntity> optionalNewRightTaskState;


        if (!optionalNewLeftTaskState.isPresent()) {
            optionalNewRightTaskState = project
                    .getTaskStates()
                    .stream()
                    .filter(anotherTaskState -> !anotherTaskState.getLeftTaskState().isPresent())
                    .findAny();

        } else {
            optionalNewRightTaskState = optionalNewLeftTaskState.get()
                    .getRightTaskState();
        }

        Optional<TaskStateEntity> optionalOldLeftTaskStateEntity = changeTaskState.getLeftTaskState();
        Optional<TaskStateEntity> optionalOldRightTaskStateEntity = changeTaskState.getRightTaskState();

        optionalOldLeftTaskStateEntity.ifPresent(it -> {
            it.setRightTaskState(optionalOldRightTaskStateEntity.orElse(null));

            taskStateRepository.saveAndFlush(it);
        });

        optionalOldRightTaskStateEntity.ifPresent(it -> {
            it.setLeftTaskState(optionalOldLeftTaskStateEntity.orElse(null));
            taskStateRepository.saveAndFlush(it);
        });


        if (optionalNewLeftTaskState.isPresent()) {
            TaskStateEntity newLeftTaskState = optionalNewLeftTaskState.get();

            newLeftTaskState.setRightTaskState(changeTaskState);

            changeTaskState.setLeftTaskState(newLeftTaskState);
        } else {
            changeTaskState.setLeftTaskState(null);
        }

        if (optionalNewRightTaskState.isPresent()) {
            TaskStateEntity newRightTaskState = optionalNewRightTaskState.get();

            newRightTaskState.setLeftTaskState(changeTaskState);

            changeTaskState.setRightTaskState(newRightTaskState);
        } else {
            changeTaskState.setRightTaskState(null);
        }

        changeTaskState = taskStateRepository.saveAndFlush(changeTaskState);

        optionalNewRightTaskState.ifPresent(taskStateRepository::saveAndFlush);
        optionalNewLeftTaskState.ifPresent(taskStateRepository::saveAndFlush);

        return taskStateDtoFactory.makeTaskStateDto(changeTaskState);
    }






    private TaskStateEntity getTaskStateOrThrowException(Long taskStateId) {
        return taskStateRepository.findById(taskStateId)
                .orElseThrow(() -> new NotFoundException(String.format("Task state with id=%d doesn't exists", taskStateId)));
    }
}
