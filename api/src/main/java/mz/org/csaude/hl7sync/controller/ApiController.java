package mz.org.csaude.hl7sync.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.uhn.hl7v2.HL7Exception;
import mz.org.csaude.hl7sync.dao.hl7filegenerator.HL7FileGeneratorDao;
import mz.org.csaude.hl7sync.model.HL7FileRequest;
import mz.org.csaude.hl7sync.model.Hl7FileForm;
import mz.org.csaude.hl7sync.model.Job;
import mz.org.csaude.hl7sync.model.Location;
import mz.org.csaude.hl7sync.service.Hl7Service;
import mz.org.csaude.hl7sync.service.JobService;
import mz.org.csaude.hl7sync.service.LocationService;

@RestController
@RequestMapping("/api/demographics/")
public class ApiController {
    private static final Logger log = LoggerFactory.getLogger(ApiController.class);
    private static final String HL7_EXTENSION = ".hl7.enc";
    private static final String METADATA_JSON = ".metadata.json";
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
    public ResponseEntity<?> createHL7Request(@RequestBody Hl7FileForm hl7FileForm) throws HL7Exception, IOException {

        // Check if there's an ongoing job for this location
        List<Job> existingJob = jobService.findByLocationUUIDAndStatuses(
                hl7FileForm.getDistrict().getUuid(), List.of(Job.JobStatus.QUEUED, Job.JobStatus.PROCESSING)
        );

        //Return existing job ID if a job is in progress
        if (!existingJob.isEmpty()) {
            return buildErrorResponse("Processing", "Job already in progress. JobID: " + existingJob.get(0).getJobId());
        }

        log.info("HERE IS THE UUID");
        log.info(hl7FileForm.getProvince().getUuid());
        
        // Check if the locationUUID provided exists
        Location province = locationService.findByUuid(hl7FileForm.getProvince().getUuid());
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
        req.setProvince(hl7FileForm.getProvince());
        req.setDistrict(hl7FileForm.getDistrict());
        req.setHealthFacilities(hl7FileForm.getHealthFacilities());

        // Create a new job
        String jobId = UUID.randomUUID().toString();
        Job newJob = new Job();
        newJob.setJobId(jobId);
        newJob.setLocationUUID(hl7FileForm.getDistrict().getUuid());
        newJob.setStatus(Job.JobStatus.QUEUED);
        newJob.setCreatedAt(LocalDateTime.now());
        newJob.setUpdatedAt(LocalDateTime.now());
        newJob.setHealthFacilities(hl7FileForm.getHealthFacilities().stream()
                .map(Location::getName)
                .collect(Collectors.joining(", ")));

        // Generate timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        String timestamp = newJob.getCreatedAt().format(formatter);

        newJob.setDownloadURL(hl7FolderName + hl7FileName+ "_" +hl7FileForm.getDistrict().getName()+ "_"+ timestamp + HL7_EXTENSION);
        jobService.save(newJob);

        hl7Service.generateHl7File(req, newJob);

        log.info("Job Created: {}", jobId);

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

        // Regex pattern to extract district and full timestamp
        String districtWithTimestamp = "";
        Pattern pattern = Pattern.compile("Patient_Demographic_Data_([A-Za-z]+_\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2})");
        Matcher matcher = pattern.matcher(downloadUrl);

        if (matcher.find()) {
            districtWithTimestamp = matcher.group(1);  // Extracts, for example, "Milange_2025_03_09_11_40_53"
        } else {
            log.info("District with timestamp not found");
        }

        log.info("Extracted: " + districtWithTimestamp);

        Path metadataPath = Paths.get(hl7FolderName, districtWithTimestamp + METADATA_JSON);

        try {
            // Create a temporary ZIP file
            Path zipPath = Files.createTempFile("patient-data-", ".zip");

            try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                // Add HL7 file to the ZIP
                Resource hl7Resource = new UrlResource(filePath.toUri());
                if (!hl7Resource.exists()) {
                    return ResponseEntity.notFound().build();
                }

                // Add HL7 file entry
                ZipEntry hl7Entry = new ZipEntry("Patient_Demographic_Data.hl7.enc");
                zipOut.putNextEntry(hl7Entry);
                try (InputStream in = hl7Resource.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        zipOut.write(buffer, 0, len);
                    }
                }
                zipOut.closeEntry();

                // Add metadata file to the ZIP if it exists
                if (Files.exists(metadataPath)) {
                    ZipEntry metadataEntry = new ZipEntry(".metadata.json");
                    zipOut.putNextEntry(metadataEntry);
                    try (InputStream in = Files.newInputStream(metadataPath)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            zipOut.write(buffer, 0, len);
                        }
                    }
                    zipOut.closeEntry();
                } else {
                	log.warn("Metadata file not found at: {}", metadataPath);
                }
            }

            // Serve the ZIP file
            Resource zipResource = new FileSystemResource(zipPath);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=patient_data_package.zip")
                    .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                    .body(zipResource);

        } catch (Exception e) {
            log.error("Error creating ZIP package for JobID {}: {}", jobId, e.getMessage());
            return ResponseEntity.internalServerError().body("Error retrieving the files: " + e.getMessage());
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

    @GetMapping("/getGeneratedHL7Files/{locationUUID}")
    public ResponseEntity<?> getHl7Files(@PathVariable String locationUUID) {

        // Validate the job
        List<Job> jobs = jobService.findByLocationUUIDAndStatuses(locationUUID, List.of(Job.JobStatus.COMPLETED));

        if (jobs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Get only the last 5 jobs
        int startIndex = Math.max(0, jobs.size() - 5);
        List<Job> lastFiveJobs = jobs.subList(startIndex, jobs.size());

        return ResponseEntity.ok(lastFiveJobs);
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
