package mz.org.csaude.hl7sync.dao.jobrepository;

import mz.org.csaude.hl7sync.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepositoryDao extends JpaRepository<Job, Long> {
    Optional<Job> findByLocationUUIDAndStatusIn(String locationUUID, List<Job.JobStatus> statuses);
}
