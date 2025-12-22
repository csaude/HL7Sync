package mz.org.csaude.hl7sync.service;
import mz.org.csaude.hl7sync.dao.jobrepository.JobRepositoryDao;
import mz.org.csaude.hl7sync.model.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class JobServiceImpl implements JobService{

    private static final Logger log = LoggerFactory.getLogger(JobServiceImpl.class.getName());

    private final JobRepositoryDao jobRepositoryDao;

    @PostConstruct
    public void cleanupStaleJobsOnStartup() {
        log.info("Checking for stale jobs on startup...");
        List<Job.JobStatus> staleStatuses = List.of(Job.JobStatus.QUEUED, Job.JobStatus.PROCESSING);
        List<Job> staleJobs = findByStatuses(staleStatuses);

        if (staleJobs.isEmpty()) {
            log.info("No stale jobs found.");
            return;
        }

        log.warn("Found {} stale jobs. Marking as FAILED...", staleJobs.size());
        for (Job job : staleJobs) {
            job.setStatus(Job.JobStatus.FAILED);
            job.setUpdatedAt(LocalDateTime.now());
            job.setErrorDetails("Job failed due to an unexpected server restart or shutdown.");
            save(job);
        }
        log.info("Stale job cleanup complete.");
    }

    @Override
    public List<Job> findByStatuses(List<Job.JobStatus> statuses) {
        return jobRepositoryDao.findByStatusIn(statuses);
    }

    @Autowired
    public JobServiceImpl(JobRepositoryDao jobRepositoryDao) {
        this.jobRepositoryDao = jobRepositoryDao;
    }

    @Override
    public Optional<Job> findJobById(String jobId) {
        return jobRepositoryDao.findByJobId(jobId);
    }

    @Override
    public List <Job> findByLocationUUIDAndStatuses(String locationUUID, List<Job.JobStatus> statuses) {
        return jobRepositoryDao.findByLocationUUIDAndStatusIn(locationUUID, statuses);
    }

    @Override
    public Job save(Job job) {
        return jobRepositoryDao.save(job);  // Use the repository to save the job
    }
}
