package mz.org.csaude.hl7sync.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Stores the result of processing a list of patient demographics,
 * the HL7 file itself and processing errors that might have happened.
 */
@Getter
@Setter
public class ProcessingResult {
    private HL7File hl7File;
    private List<String> errorLogs;

}
