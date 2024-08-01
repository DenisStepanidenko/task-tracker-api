package denis.code.task_tracker_api.api.controllers;


import denis.code.task_tracker_api.api.controllers.helpers.ControllerHelper;
import denis.code.task_tracker_api.api.dto.TaskDto;
import denis.code.task_tracker_api.api.exceptions.BadRequestException;
import denis.code.task_tracker_api.api.factories.TaskDtoFactory;
import denis.code.task_tracker_api.store.entities.ProjectEntity;
import denis.code.task_tracker_api.store.entities.TaskEntity;
import denis.code.task_tracker_api.store.entities.TaskStateEntity;
import denis.code.task_tracker_api.store.repositories.TaskRepository;
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
public class TaskController {
    TaskStateRepository taskStateRepository;
    TaskRepository taskRepository;

    ControllerHelper controllerHelper;
    TaskDtoFactory taskDtoFactory;

    // получить все таски по task-state
    public static final String GET_TASKS = "/api/task-state/{task_state_id}";

    public static final String GET_TASK = "/api/task/{task_id}";

    public static final String CREATE_TASK = "/api/task-state/{task_state_id}/createTask";

    public static final String UPDATE_TASK = "/api/task/{task_id}";

    public static final String CHANGE_TASK = "/api/task/{task_id}/changePosition";

    @GetMapping(GET_TASKS)
    public List<TaskDto> getTasks(@PathVariable(name = "task_state_id") Long taskStateId) {
        TaskStateEntity taskStateEntity = controllerHelper.getTaskStateOrThrowException(taskStateId);

        List<TaskEntity> taskEntities = taskStateEntity.getTasks();

        return taskEntities
                .stream()
                .map(taskDtoFactory::makeTaskDto)
                .collect(Collectors.toList());
    }

    @GetMapping(GET_TASK)
    public TaskDto getTask(@PathVariable("task_id") Long taskId) {

        TaskEntity taskEntity = controllerHelper.getTaskOrThrowException(taskId);

        return taskDtoFactory.makeTaskDto(taskEntity);
    }


    @PostMapping(CREATE_TASK)
    public TaskDto createTask(@PathVariable(name = "task_state_id") Long taskStateId,
                              @RequestParam("name") String name,
                              @RequestParam(value = "description", required = false) Optional<String> optionalDescription) {

        // проверка, что по taskStateId существует состояние
        TaskStateEntity taskStateEntity = controllerHelper.getTaskStateOrThrowException(taskStateId);

        // проверка, что имя непустое
        if (name.trim().isEmpty()) {
            throw new BadRequestException("Task state name can't be empty");
        }

        Optional<TaskEntity> optionalLastTaskEntity = Optional.empty();

        for (TaskEntity task : taskStateEntity.getTasks()) {

            if (task.getName().equals(name)) {
                throw new BadRequestException(String.format("Task with name=%s already exists", name));
            }

            if (task.getRightTaskEntity().isEmpty()) {
                optionalLastTaskEntity = Optional.of(task);
            }
        }

        TaskEntity taskEntity = taskRepository.saveAndFlush(TaskEntity
                .builder()
                .name(name)
                .description(optionalDescription.orElse(null))
                .taskState(taskStateEntity)
                .build()
        );

        optionalLastTaskEntity
                .ifPresent(oldLastTask -> {
                    oldLastTask.setRightTaskEntity(taskEntity);
                    taskEntity.setLeftTaskEntity(oldLastTask);

                    taskRepository.saveAndFlush(oldLastTask);
                });

        TaskEntity task = taskRepository.saveAndFlush(taskEntity);

        return taskDtoFactory.makeTaskDto(task);
    }


    @PatchMapping(UPDATE_TASK)
    public TaskDto updateTask(@PathVariable("task_id") Long taskId,
                              @RequestParam("name") String name,
                              @RequestParam(name = "description", required = false) Optional<String> optionalDescription) {

        TaskEntity taskToUpdate = controllerHelper.getTaskOrThrowException(taskId);

        if (name.trim().isEmpty()) {
            throw new BadRequestException("Name can't be empty");
        }

        TaskStateEntity taskStateEntity = taskToUpdate.getTaskState();

        taskStateEntity
                .getTasks()
                .stream()
                .filter(anotherTaskState -> Objects.equals(anotherTaskState.getName(), name) && !Objects.equals(taskId, anotherTaskState.getId()))
                .findAny()
                .ifPresent(it -> {
                    throw new BadRequestException(String.format("Task with name=%s already exists", name));
                });

        taskToUpdate.setName(name);

        optionalDescription.ifPresent(taskToUpdate::setDescription);

        TaskEntity taskEntity = taskRepository.saveAndFlush(taskToUpdate);

        return taskDtoFactory.makeTaskDto(taskEntity);

    }

    @PatchMapping(CHANGE_TASK)
    public TaskDto changePositionTask(@PathVariable("task_id") Long taskId,
                                      @RequestParam(name = "left_task_id", required = false) Optional<Long> optionalLeftTaskId) {

        TaskEntity changeTask = controllerHelper.getTaskOrThrowException(taskId);
        TaskStateEntity taskState = changeTask.getTaskState();
        ProjectEntity project = changeTask.getTaskState().getProject();

        // ситуация, что указанный левый сосед уже совпадает с существующим
        Optional<Long> optionalOldLeftTaskId = changeTask
                .getLeftTaskEntity()
                .map(TaskEntity::getId);

        if (optionalOldLeftTaskId.equals(optionalLeftTaskId)) {
            return taskDtoFactory.makeTaskDto(changeTask);
        }

        Optional<TaskEntity> optionalNewLeftTask = optionalLeftTaskId
                .map(leftTaskStateId -> {

                    if (leftTaskStateId.equals(taskId)) {
                        throw new BadRequestException("Left task id equals changed task id");
                    }

                    TaskEntity leftTask = controllerHelper.getTaskOrThrowException(leftTaskStateId);

                    if (!Objects.equals(leftTask.getTaskState().getProject().getId(), project.getId())) {
                        throw new BadRequestException("Task position can be changed within the same project");
                    }

                    return leftTask;

                });


        Optional<TaskEntity> optionalNewRightTask;

        if (!optionalNewLeftTask.isPresent()) {
            optionalNewRightTask = taskState
                    .getTasks()
                    .stream()
                    .filter(anotherTask -> !anotherTask.getLeftTaskEntity().isPresent())
                    .findAny();
        } else {
            optionalNewRightTask = optionalNewLeftTask.get().getRightTaskEntity();
        }


        Optional<TaskEntity> optionalOldLeftTask = changeTask.getLeftTaskEntity();
        Optional<TaskEntity> optionalOldRightTask = changeTask.getRightTaskEntity();

        changeTask.setLeftTaskEntity(null);
        changeTask.setRightTaskEntity(null);

        changeTask = taskRepository.saveAndFlush(changeTask);

        optionalOldLeftTask.ifPresent(it -> {
            it.setRightTaskEntity(optionalOldRightTask.orElse(null));

            taskRepository.saveAndFlush(it);
        });

        optionalOldRightTask.ifPresent(it -> {
            it.setLeftTaskEntity(optionalOldLeftTask.orElse(null));

            taskRepository.saveAndFlush(it);
        });


        if (optionalNewLeftTask.isPresent()) {

            optionalNewLeftTask.get().setRightTaskEntity(changeTask);
            changeTask.setLeftTaskEntity(optionalNewLeftTask.get());
        } else {
            changeTask.setLeftTaskEntity(null);
        }

        if (optionalNewRightTask.isPresent()) {

            optionalNewRightTask.get().setLeftTaskEntity(changeTask);
            changeTask.setRightTaskEntity(optionalNewRightTask.get());
        } else {
            changeTask.setRightTaskEntity(null);
        }




        changeTask = taskRepository.saveAndFlush(changeTask);
        optionalNewLeftTask.ifPresent(taskRepository::saveAndFlush);
        optionalNewRightTask.ifPresent(taskRepository::saveAndFlush);





        return taskDtoFactory.makeTaskDto(changeTask);
    }

}
