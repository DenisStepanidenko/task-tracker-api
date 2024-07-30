package denis.code.task_tracker_api.api.controllers;

import denis.code.task_tracker_api.api.dto.AckDto;
import denis.code.task_tracker_api.api.dto.ProjectDto;
import denis.code.task_tracker_api.api.exceptions.BadRequestException;
import denis.code.task_tracker_api.api.exceptions.NotFoundException;
import denis.code.task_tracker_api.api.factories.ProjectDtoFactory;
import denis.code.task_tracker_api.store.entities.ProjectEntity;
import denis.code.task_tracker_api.store.repositories.ProjectRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProjectController {
    ProjectRepository projectRepository;
    ProjectDtoFactory projectDtoFactory;

    public static final String CREATE_PROJECT = "/api/projects";
    public static final String EDIT_PROJECT = "/api/projects/{project_id}";
    public static final String FETCH_PROJECTS = "/api/projects";
    public static final String DELETE_PROJECT = "/api/projects/{project_id}";
    public static final String FIND_PROJECT = "/api/projects/{project_id}";

    @GetMapping(FIND_PROJECT)
    public ProjectDto findProject(@PathVariable("project_id") Long projectId) {

        ProjectEntity project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException(String.format("Project with id=%d doesn't exists", projectId)));

        return projectDtoFactory.makeProjectDto(project);
    }


    @GetMapping(FETCH_PROJECTS)
    public List<ProjectDto> fetchProjects(@RequestParam(value = "prefix_name", required = false) Optional<String> optionalPrefixName) {

        optionalPrefixName = optionalPrefixName.filter(prefixName -> !prefixName.trim().isEmpty());

        Stream<ProjectEntity> projectStream;

        if (optionalPrefixName.isPresent()) {
            projectStream = projectRepository.streamAllByNameStartingWithIgnoreCase(optionalPrefixName.get());
        } else {
            projectStream = projectRepository.streamAll();
        }

        return projectStream.map(projectDtoFactory::makeProjectDto)
                .collect(Collectors.toList());
    }


    @PostMapping(CREATE_PROJECT)
    public ProjectDto createProject(@RequestParam String name) {

        if (name.trim().isEmpty()) {
            throw new BadRequestException("Name can't be empty");
        }


        projectRepository
                .findByName(name)
                .ifPresent(project -> {
                    throw new BadRequestException(String.format("Project %s already exists", name));
                });

        ProjectEntity project = projectRepository.saveAndFlush(
                ProjectEntity.builder()
                        .name(name)
                        .build()
        );

        return projectDtoFactory.makeProjectDto(project);
    }

    @PatchMapping(EDIT_PROJECT)
    public ProjectDto editProject(@RequestParam String name, @PathVariable("project_id") Long projectId) {

        if (name.trim().isEmpty()) {
            throw new BadRequestException("Name can't be empty");
        }


        ProjectEntity project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException(String.format("Project with id=%d doesn't exists", projectId)));


        projectRepository
                .findByName(name)
                .filter(anotherProject -> !Objects.equals(anotherProject.getId(), projectId))
                .ifPresent(anotherProject -> {
                    throw new BadRequestException(String.format("Project %s already exists", name));
                });

        project.setName(name);

        project = projectRepository.saveAndFlush(project);

        return projectDtoFactory.makeProjectDto(project);
    }

    @DeleteMapping(DELETE_PROJECT)
    public AckDto deleteProject(@PathVariable("project_id") Long projectId) {

        projectRepository
                .findById(projectId)
                .orElseThrow(() -> new NotFoundException(String.format("Project with id=%d doesn't exists", projectId)));


        projectRepository.deleteById(projectId);

        return AckDto.makeDefault(true);
    }
}
