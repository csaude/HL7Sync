package mz.org.csaude.hl7sync.service;


import mz.org.csaude.hl7sync.model.*;

import ca.uhn.hl7v2.HL7Exception;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Hl7Service {
    /**
     * @param locations The locations to be used to generate the HL7 file.
     * @return The generated HL7 file.
     * @throws HL7Exception
     */
    public CompletableFuture<ProcessingResult> generateHl7File(HL7FileRequest fileRequest, Job newJob);

    /**
     * @return The generated HL7 file, could be processing.
     */
    public CompletableFuture<ProcessingResult> getProcessingResult();

    /**
     * @return The HL7 file that was succesfully generated or null if none.
     */
    public HL7File getHl7File();

    /**
     * @return True if the search is available, false otherwise. Due to asynchronous
     *         nature of the file generation, clients should call this method
     *         before calling search.
     */
    public boolean isSearchAvailable();

    /**
     * @param partialNID The partial NID to be used to search for patients.
     * @return The list of patients matching the partial NID.
     */
    public List<PatientDemographic> search(String partialNID);
}
