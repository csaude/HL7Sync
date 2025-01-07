package mz.org.csaude.hl7sync.controller;


import ca.uhn.hl7v2.HL7Exception;
import mz.org.csaude.hl7sync.model.HL7FileRequest;
import mz.org.csaude.hl7sync.service.Hl7Service;
import mz.org.csaude.hl7sync.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger LOG = LoggerFactory.getLogger(ApiController.class);

    private Hl7Service hl7Service;
    private LocationService locationService;

    public ApiController(Hl7Service hl7Service, LocationService locationService){
        this.hl7Service = hl7Service;
        this.locationService = locationService;
    }
    @GetMapping("/demographics/generate")
    public void createHL7Request() throws HL7Exception, IOException {
        HL7FileRequest req = new HL7FileRequest();
        req.setProvince(locationService.findByUuid("b637a1f8-21a6-4b75-8a66-73626903deab"));
        System.out.println(req.getProvince());
        req.setDistrict(locationService.findByUuid("b637a1f8-21a6-4b75-8a66-73626903deab").getChildLocations().get(0));
       System.out.println(req.getDistrict());
        req.setHealthFacilities(locationService.findByUuid("b637a1f8-21a6-4b75-8a66-73626903deab").getChildLocations());
        System.out.println(req.getHealthFacilities());
        hl7Service.generateHl7File(req);
       System.out.println("Done");
        // ConfigController.previousHl7FileForm = hl7FileForm;

        LOG.info("File generated");
    }
}
