package denis.code.task_tracker_api.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TaskDto {
    @NonNull
    Long id;

    @NonNull
    String name;


    String description;

    @JsonProperty("left_task_id")
    Long leftTaskId;

    @JsonProperty("right_task_id")
    Long rightTaskId;

    @NonNull
    @JsonProperty("created_at")
    Instant createdAt;
}
