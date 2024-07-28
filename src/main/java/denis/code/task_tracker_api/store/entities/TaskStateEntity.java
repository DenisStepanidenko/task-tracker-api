package denis.code.task_tracker_api.store.entities;


import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;


    @Column(unique = true)
    private String name;

    private Long ordinal;


    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    @OneToMany
    private List<TaskEntity> tasks = new ArrayList<>();
}
