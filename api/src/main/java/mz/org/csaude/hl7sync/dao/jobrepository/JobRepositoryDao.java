package mz.org.csaude.hl7sync.dao.jobrepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import mz.org.csaude.hl7sync.model.Job;
import mz.org.csaude.hl7sync.model.JobStatus;

@Repository
public interface JobRepositoryDao extends JpaRepository<Job, Long> {

    // Find a job by its location UUID and status
    Optional<Job> findByLocationUUIDAndStatusIn(String locationUUID, List<JobStatus> statuses);

    // Add this method to find a job by its jobId
    Optional<Job> findByJobId(String jobId);
}
