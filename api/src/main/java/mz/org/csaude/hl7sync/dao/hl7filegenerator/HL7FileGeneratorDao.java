package mz.org.csaude.hl7sync.dao.hl7filegenerator;

import mz.org.csaude.hl7sync.model.PatientDemographic;

import java.util.List;

public interface HL7FileGeneratorDao {

    public List<PatientDemographic> getPatientDemographicData(List<String> locationsByUuid);
}