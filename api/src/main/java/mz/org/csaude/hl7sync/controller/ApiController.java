package mz.org.csaude.hl7sync.controller;

import ca.uhn.hl7v2.HL7Exception;
import mz.org.csaude.hl7sync.dao.HL7FileGeneratorDao;
import mz.org.csaude.hl7sync.model.HL7FileRequest;
import mz.org.csaude.hl7sync.model.Location;
import mz.org.csaude.hl7sync.model.PatientDemographic;
import mz.org.csaude.hl7sync.service.Hl7Service;
import mz.org.csaude.hl7sync.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import mz.org.csaude.hl7sync.dao.HL7FileGeneratorDao;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {
    private static final Logger LOG = LoggerFactory.getLogger(ApiController.class);

    private Hl7Service hl7Service;
    private HL7FileGeneratorDao hl7FileGeneratorDao;
    private LocationService locationService;

    public ApiController(Hl7Service hl7Service, LocationService locationService, HL7FileGeneratorDao hl7FileGeneratorDao) {
        this.hl7Service = hl7Service;
        this.locationService = locationService;
        this.hl7FileGeneratorDao = hl7FileGeneratorDao;
    }

    @PostMapping("/demographics/generate")
    public ResponseEntity<?> createHL7Request(@RequestParam String locationUUID) throws HL7Exception, IOException {
        // Generate a unique job ID
        String jobId = UUID.randomUUID().toString();

        HL7FileRequest req = new HL7FileRequest();

        Location province = locationService.findByUuid(locationUUID);
        req.setProvince(province);
        req.setDistrict(province.getChildLocations().get(0));
        req.setHealthFacilities(province.getChildLocations());

        hl7Service.generateHl7File(req);

        // Prepare the response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "Processing");
        response.put("message", "HL7 file is being generated. JobID:" + jobId);

        // Return a 200 OK status with the response body
        return ResponseEntity.ok(response);
    }
}
