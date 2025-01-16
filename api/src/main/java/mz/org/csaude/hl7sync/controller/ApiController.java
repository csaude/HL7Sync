package mz.org.csaude.hl7sync.controller;

import ca.uhn.hl7v2.HL7Exception;
import mz.org.csaude.hl7sync.dao.hl7filegenerator.HL7FileGeneratorDao;
import mz.org.csaude.hl7sync.dao.jobrepository.JobRepositoryDao;
import mz.org.csaude.hl7sync.model.HL7FileRequest;
import mz.org.csaude.hl7sync.model.Job;
import mz.org.csaude.hl7sync.model.Location;
import mz.org.csaude.hl7sync.service.Hl7Service;
import mz.org.csaude.hl7sync.service.JobService;
import mz.org.csaude.hl7sync.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/demographics/")
public class ApiController {
    private static final Logger LOG = LoggerFactory.getLogger(ApiController.class);
    private static final String HL7_EXTENSION = ".hl7.enc";
    private Hl7Service hl7Service;
    private HL7FileGeneratorDao hl7FileGeneratorDao;
    private LocationService locationService;
    private final JobService jobService;
    private String hl7FolderName;
    private String hl7FileName;

    public ApiController(Hl7Service hl7Service, LocationService locationService, HL7FileGeneratorDao hl7FileGeneratorDao, JobService jobService, @Value("${app.hl7.folder}") String hl7FolderName,
                         @Value("${app.hl7.filename}") String fileName) {
        this.hl7Service = hl7Service;
        this.locationService = locationService;
        this.hl7FileGeneratorDao = hl7FileGeneratorDao;
        this.jobService = jobService;
        this.hl7FolderName = hl7FolderName;
        this.hl7FileName = fileName;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> createHL7Request(@RequestParam String locationUUID) throws HL7Exception, IOException {

        // Check if there's an ongoing job for this location
        Optional<Job> existingJob = jobService.findByLocationUUIDAndStatuses(
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
        jobService.save(newJob);

        LOG.info(jobId);

        HL7FileRequest req = new HL7FileRequest();

        Location province = locationService.findByUuid(locationUUID);
        req.setProvince(province);
        req.setDistrict(province.getChildLocations().get(0));
        req.setHealthFacilities(province.getChildLocations());

        hl7Service.generateHl7File(req, jobId);

        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "Processing");
        response.put("message", "HL7 file is being generated");
        response.put("JobId", jobId);

        // Return a 200 OK status with the response body
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{jobId}")
    public ResponseEntity<?> downloadHl7File(@PathVariable String jobId) {
        // Validate the job
        Optional<Job> jobOptional = jobService.findJobById(jobId);
        if (jobOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Job job = jobOptional.get();
        if (!job.getStatus().equals(Job.JobStatus.COMPLETED)) {
            return ResponseEntity.badRequest().body("The job is still being processed!");
        }

        // Retrieve the file
        Path filePath = Paths.get(hl7FolderName).resolve(hl7FileName + jobId + HL7_EXTENSION);

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            // Serve the file as a downloadable resource
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + resource.getFilename())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE) // Set MIME type
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error retrieving the file: " + e.getMessage());
        }
    }


    @GetMapping("/status/{jobId}")
    public ResponseEntity<String> getJobStatus(@PathVariable String jobId) {
        Optional<Job> job = jobService.findJobById(jobId);
        if (job.isPresent()) {
            return ResponseEntity.ok(job.get().getStatus().toString());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Job not found");
        }
    }


}
