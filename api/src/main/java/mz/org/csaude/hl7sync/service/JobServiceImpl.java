package mz.org.csaude.hl7sync.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import mz.org.csaude.hl7sync.dao.jobrepository.JobRepositoryDao;
import mz.org.csaude.hl7sync.model.Job;
import mz.org.csaude.hl7sync.model.JobStatus;

@Service
public class JobServiceImpl implements JobService{

    private final JobRepositoryDao jobRepositoryDao;

    @Autowired
    public JobServiceImpl(JobRepositoryDao jobRepositoryDao) {
        this.jobRepositoryDao = jobRepositoryDao;
    }

    @Override
    public Optional<Job> findJobById(String jobId) {
        return jobRepositoryDao.findByJobId(jobId);
    }

    @Override
    public Optional<Job> findByLocationUUIDAndStatuses(String locationUUID, List<JobStatus> statuses) {
        return jobRepositoryDao.findByLocationUUIDAndStatusIn(locationUUID, statuses);
    }

    @Override
    public Job save(Job job) {
        return jobRepositoryDao.save(job);  // Use the repository to save the job
    }
}
