package mz.org.csaude.hl7sync.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger LOG = LoggerFactory.getLogger(ApiController.class);

    @PostMapping("/demographics/generate")
    public void createHL7Request(@RequestParam String locationUUID) {
        LOG.info("locationUUID: {}", locationUUID);
    }
}
