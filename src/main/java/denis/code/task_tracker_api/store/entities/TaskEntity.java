package denis.code.task_tracker_api.store.entities;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String name;

    private String description;

    @ManyToOne
    private TaskStateEntity taskState;

    @OneToOne
    private TaskEntity leftTaskEntity;

    @OneToOne
    private TaskEntity rightTaskEntity;

    public Optional<TaskEntity> getLeftTaskEntity(){
        return Optional.ofNullable(leftTaskEntity);
    }

    public Optional<TaskEntity> getRightTaskEntity(){
        return Optional.ofNullable(rightTaskEntity);
    }
    @Builder.Default
    private Instant createdAt = Instant.now();

}
