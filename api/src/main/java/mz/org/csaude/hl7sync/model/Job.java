package mz.org.csaude.hl7sync.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "jobs")
public class Job {
    public enum JobStatus {
        QUEUED, PROCESSING, COMPLETED, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String jobId;

    @Column(nullable = false)
    private String locationUUID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

}

