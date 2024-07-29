package denis.code.task_tracker_api.api.controllers;

import denis.code.task_tracker_api.api.dto.ProjectDto;
import denis.code.task_tracker_api.api.exceptions.BadRequestException;
import denis.code.task_tracker_api.api.factories.ProjectDtoFactory;
import denis.code.task_tracker_api.store.entities.ProjectEntity;
import denis.code.task_tracker_api.store.repositories.ProjectRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectController {
    ProjectRepository projectRepository;
    ProjectDtoFactory projectDtoFactory;

    public static final String CREATE_PROJECT = "/api/projects";


    @PostMapping(CREATE_PROJECT)
    public ProjectDto createProject(@RequestParam String name) {
        projectRepository
                .findByName(name)
                .ifPresent(project -> {
                    throw new BadRequestException(String.format("Project %s already exists", name));
                });


        //throw new BadRequestException(String.format("Project %s already exists", name));
        //ProjectEntity project = new ;

//        return projectDtoFactory.makeProjectDto();
        return null;
    }
}
