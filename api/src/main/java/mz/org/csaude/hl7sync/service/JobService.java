package mz.org.csaude.hl7sync.service;

import mz.org.csaude.hl7sync.model.Job;

import java.util.List;
import java.util.Optional;

public interface JobService {
    Optional<Job> findJobById(String jobId);

    Optional<Job> findByLocationUUIDAndStatuses(String locationUUID, List<Job.JobStatus> statuses);

    Job save(Job job);
}
