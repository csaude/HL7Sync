package mz.org.csaude.hl7sync.controller;

import ca.uhn.hl7v2.HL7Exception;
import mz.org.csaude.hl7sync.dao.HL7FileGeneratorDao;
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
import java.time.format.DateTimeFormatter;

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

    @PostMapping("/generate/{locationUUID}")
    public ResponseEntity<?> createHL7Request(@PathVariable String locationUUID) throws HL7Exception, IOException {
        // Check if there's an ongoing job for this location
        Optional<Job> existingJob = jobService.findByLocationUUIDAndStatuses(
                locationUUID, List.of(Job.JobStatus.QUEUED, Job.JobStatus.PROCESSING)
        );
        // Return existing job ID if a job is in progress
        if (existingJob.isPresent()) {
            return buildErrorResponse("Processing", "Job already in progress. JobID: " + existingJob.get().getJobId());
        }

        // Check if the locationUUID provided exists
        Location province = locationService.findByUuid(locationUUID);
        if (province == null) {
            return buildErrorResponse("Location not found", "Unable to find the provided locationUUID");
        }

        // Check if the province has child locations (districts)
        List<Location> childLocations = province.getChildLocations();
        if (childLocations == null || childLocations.isEmpty()) {
            return buildErrorResponse("District not found", "No child locations (districts) found for the provided locationUUID");
        }

        // Retrieve the first district
        Location district = childLocations.get(0);
        if (district == null) {
            return buildErrorResponse("District not found", "Unable to find a valid district in the child locations");
        }

        // Check if there are health facilities
        List<Location> healthFacilities = province.getChildLocations(); // Assuming same child locations
        if (healthFacilities == null || healthFacilities.isEmpty()) {
            return buildErrorResponse("Health facilities not found", "Unable to find any health facility for the provided locationUUID");
        }

        // Create a new HL7 File Request
        HL7FileRequest req = new HL7FileRequest();
        req.setProvince(province);
        req.setDistrict(district);
        req.setHealthFacilities(healthFacilities);

        // Create a new job
        String jobId = UUID.randomUUID().toString();
        Job newJob = new Job();
        newJob.setJobId(jobId);
        newJob.setLocationUUID(locationUUID);
        newJob.setStatus(Job.JobStatus.QUEUED);
        newJob.setCreatedAt(LocalDateTime.now());
        newJob.setUpdatedAt(LocalDateTime.now());

        // Generate timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        String timestamp = LocalDateTime.now().format(formatter);
        newJob.setDownloadURL(hl7FolderName + hl7FileName+ "_" +district.getName()+ "_"+ timestamp + HL7_EXTENSION);
        jobService.save(newJob);
        hl7Service.generateHl7File(req, newJob);

        LOG.info("Job Created: {}", jobId);

        return buildSuccessResponse("Processing", "HL7 file is being generated", Map.of("JobId", jobId));
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

        // Use the stored download URL
        String downloadUrl = job.getDownloadURL();
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            return ResponseEntity.internalServerError().body("Download URL is not available for this job.");
        }

        // Convert the stored path string to a Path object
        Path filePath = Paths.get(downloadUrl);
        System.out.println(filePath);

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
            LOG.error("Error retrieving file for JobID {}: {}", jobId, e.getMessage());
            return ResponseEntity.internalServerError().body("Error retrieving the file: " + e.getMessage());
        }
    }


    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId) {
        Optional<Job> job = jobService.findJobById(jobId);
        if (job.isPresent()) {
            Job foundJob = job.get();
            Map<String, Object> response = new HashMap<>();
            response.put("jobId", foundJob.getJobId());
            response.put("status", foundJob.getStatus());
            response.put("updatedAt", foundJob.getUpdatedAt());

            // Explicitly handle JobStatus in the switch statement and return the corresponding message
            String message = switch (foundJob.getStatus()) {
                case QUEUED -> "Job is queued for processing.";
                case PROCESSING -> "Job is currently being processed.";
                case COMPLETED -> "Job completed successfully.";
                case FAILED -> "Job failed. " + foundJob.getErrorDetails();
                default -> "Unknown job status";  // Fallback for unhandled status
            };
            response.put("message", message);
            return ResponseEntity.ok(response);

        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Job not found");
        }
    }

    // Helper method to create response maps
    private ResponseEntity<Map<String, Object>> buildErrorResponse(String status, String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", status, "message", message));
    }

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(String status, String message, Map<String, Object> additionalData) {
        Map<String, Object> response = new HashMap<>(additionalData);
        response.put("status", status);
        response.put("message", message);
        return ResponseEntity.ok(response);
    }


}
