package mz.org.csaude.hl7sync.service;

import java.util.List;
import java.util.Optional;

import mz.org.csaude.hl7sync.model.Job;
import mz.org.csaude.hl7sync.model.JobStatus;

public interface JobService {
    Optional<Job> findJobById(String jobId);

    Optional<Job> findByLocationUUIDAndStatuses(String locationUUID, List<JobStatus> statuses);

    Job save(Job job);
}
