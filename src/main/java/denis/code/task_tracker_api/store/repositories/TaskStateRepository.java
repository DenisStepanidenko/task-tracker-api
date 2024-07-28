package denis.code.task_tracker_api.store.repositories;

import denis.code.task_tracker_api.store.entities.TaskStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskStateRepository extends JpaRepository<TaskStateEntity , Long> {

}
