package denis.code.task_tracker_api.store.repositories;

import denis.code.task_tracker_api.store.entities.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.stream.Stream;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
    Optional<ProjectEntity> findByName(String name);

    @Query(value = "select * from project " , nativeQuery = true)
    Stream<ProjectEntity> streamAll();
    Stream<ProjectEntity> streamAllByNameStartingWithIgnoreCase(String name);


}
