package denis.code.task_tracker_api.store.repositories;

import denis.code.task_tracker_api.store.entities.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity , Long> {

}
