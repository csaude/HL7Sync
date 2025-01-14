package mz.org.csaude.hl7sync.controller;

import ca.uhn.hl7v2.HL7Exception;
import mz.org.csaude.hl7sync.dao.hl7filegenerator.HL7FileGeneratorDao;
import mz.org.csaude.hl7sync.dao.jobrepository.JobRepositoryDao;
import mz.org.csaude.hl7sync.model.HL7FileRequest;
import mz.org.csaude.hl7sync.model.Job;
import mz.org.csaude.hl7sync.model.Location;
import mz.org.csaude.hl7sync.service.Hl7Service;
import mz.org.csaude.hl7sync.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private static final Logger LOG = LoggerFactory.getLogger(ApiController.class);

    private Hl7Service hl7Service;
    private HL7FileGeneratorDao hl7FileGeneratorDao;
    private LocationService locationService;

    private JobRepositoryDao jobRepositoryDao;

    public ApiController(Hl7Service hl7Service, LocationService locationService, HL7FileGeneratorDao hl7FileGeneratorDao, JobRepositoryDao jobRepositoryDao) {
        this.hl7Service = hl7Service;
        this.locationService = locationService;
        this.hl7FileGeneratorDao = hl7FileGeneratorDao;
        this.jobRepositoryDao = jobRepositoryDao;
    }

    @PostMapping("/demographics/generate")
    public ResponseEntity<?> createHL7Request(@RequestParam String locationUUID) throws HL7Exception, IOException {

        // Check if there's an ongoing job for this location
        Optional<Job> existingJob = jobRepositoryDao.findByLocationUUIDAndStatusIn(
                locationUUID, List.of(Job.JobStatus.QUEUED, Job.JobStatus.PROCESSING)
        );

        if (existingJob.isPresent()) {
            // Return existing job ID if a job is in progress
            Map<String, Object> response = new HashMap<>();
            response.put("status", "Processing");
            response.put("message", "Job already in progress. JobID: " + existingJob.get().getJobId());
            return ResponseEntity.ok(response);
        }

        // Create a new job
        String jobId = UUID.randomUUID().toString();
        Job newJob = new Job();
        newJob.setJobId(jobId);
        newJob.setLocationUUID(locationUUID);
        newJob.setStatus(Job.JobStatus.QUEUED);
        newJob.setCreatedAt(LocalDateTime.now());
        newJob.setUpdatedAt(LocalDateTime.now());
        jobRepositoryDao.save(newJob);

        HL7FileRequest req = new HL7FileRequest();

        Location province = locationService.findByUuid(locationUUID);
        req.setProvince(province);
        req.setDistrict(province.getChildLocations().get(0));
        req.setHealthFacilities(province.getChildLocations());

        hl7Service.generateHl7File(req, jobId);

        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "Processing");
        response.put("message", "HL7 file is being generated. JobID:" + jobId);

        // Return a 200 OK status with the response body
        return ResponseEntity.ok(response);
    }
}
