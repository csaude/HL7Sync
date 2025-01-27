package mz.org.csaude.hl7sync.dao;

import mz.org.csaude.hl7sync.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepositoryDao extends JpaRepository<Job, Long> {

    // Find a job by its location UUID and status
    Optional<Job> findByLocationUUIDAndStatusIn(String locationUUID, List<Job.JobStatus> statuses);

    // Add this method to find a job by its jobId
    Optional<Job> findByJobId(String jobId);
}
