package mz.org.csaude.hl7sync.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HL7File extends HL7FileRequest {

    private LocalDateTime lastModifiedTime;
}
