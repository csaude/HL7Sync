package mz.org.csaude.hl7sync.dao.hl7filegenerator;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import mz.org.csaude.hl7sync.model.PatientDemographic;
import mz.org.csaude.hl7sync.util.Hl7Util;


@Repository
public class HL7FileGeneratorDaoImpl implements HL7FileGeneratorDao {
	
	private static final Logger log = LoggerFactory.getLogger(HL7FileGeneratorDaoImpl.class.getName());

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String sql;

    public List<PatientDemographic> getPatientDemographicData(List<String> locationsByUuid) {
    	log.info("getPatientDemographicData called...");

    	sql = "SELECT"
    	        + "    REPLACE(REPLACE(pid.identifier, '\r', ''), '\n', ' ') pid," 
    	        + "    pe.gender," 
    	        + "    pe.birthdate," 
    	        + "    REPLACE(REPLACE(pn.given_name, '\r', ''), '\n', ' ') given_name," 
    	        + "    REPLACE(REPLACE(pn.middle_name, '\r', ''), '\n', ' ') middle_name," 
    	        + "    REPLACE(REPLACE(pn.family_name, '\r', ''), '\n', ' ') family_name," 
    	        + "    REPLACE(" 
    	        + "        REPLACE(" 
    	        + "            CONCAT(" 
    	        + "                TRIM(IFNULL(pa.address1, ''))," 
    	        + "                ' '," 
    	        + "                TRIM(IFNULL(pa.address2, ''))," 
    	        + "                ' '," 
    	        + "                TRIM(IFNULL(pa.address6, ''))," 
    	        + "                ' '," 
    	        + "                TRIM(IFNULL(pa.address5, ''))" 
    	        + "            )," 
    	        + "            '\r'," 
    	        + "            ''" 
    	        + "        )," 
    	        + "        '\n'," 
    	        + "        ' '" 
    	        + "    ) address," 
    	        + "    REPLACE(REPLACE(pa.state_province, '\r', ''), '\n', ' ') state_province," 
    	        + "    REPLACE(REPLACE(pa.country, '\r', ''), '\n', ' ') country," 
    	        + "    REPLACE(REPLACE(pa.county_district, '\r', ''), '\n', ' ') county_district," 
    	        + "    REPLACE(REPLACE(pat.value, '\r', ''), '\n', ' ') telefone1," 
    	        + "    REPLACE(REPLACE(pat1.value, '\r', ''), '\n', ' ') telefone2," 
    	        + "    CASE" 
    	        + "        WHEN pat2.value = 1057 THEN 'S'" 
    	        + "        WHEN pat2.value = 5555 THEN 'M'" 
    	        + "        WHEN pat2.value = 1060 THEN 'P'" 
    	        + "        WHEN pat2.value = 1059 THEN 'W'" 
    	        + "        WHEN pat2.value = 1056 THEN 'D'" 
    	        + "        ELSE 'T'" 
    	        + "    END AS marital_status," 
    	        + "    l.location_id," 
    	        + "    l.name AS locationName," 
    	        + "    enc2.encounterDate AS lastConsultationDate" 
    	        + " FROM person pe" 
    	        + " INNER JOIN patient p ON pe.person_id = p.patient_id" 
    	        + " INNER JOIN patient_identifier pid on pid.patient_id=p.patient_id and pid.voided = 0 AND pid.identifier_type = 2" 
    	        + " INNER JOIN location l on l.location_id=pid.location_id and  l.retired = 0" 
    	        + " LEFT JOIN  person_attribute pat ON pat.person_id=p.patient_id and pat.voided = 0 AND pat.person_attribute_type_id = 9" 
    	        + " LEFT JOIN  person_attribute pat1 ON pat1.person_id=p.patient_id and pat1.voided = 0 AND pat1.person_attribute_type_id = 14" 
    	        + " LEFT JOIN  person_attribute pat2 ON pat2.person_id=p.patient_id and pat2.voided = 0 AND pat2.person_attribute_type_id = 5" 
    	        + " LEFT JOIN (" 
    	        + "     SELECT pn1.*" 
    	        + "     FROM person_name pn1" 
    	        + "     INNER JOIN (" 
    	        + "         SELECT person_id, MIN(person_name_id) AS id" 
    	        + "         FROM person_name" 
    	        + "         WHERE voided = 0" 
    	        + "         GROUP BY person_id" 
    	        + "     ) pn2 ON pn1.person_id = pn2.person_id AND pn1.person_name_id = pn2.id" 
    	        + " ) pn ON pn.person_id = p.patient_id" 
    	        + " LEFT JOIN (" 
    	        + "     SELECT pa1.*" 
    	        + "     FROM person_address pa1" 
    	        + "     INNER JOIN (" 
    	        + "         SELECT person_id, MIN(person_address_id) AS id" 
    	        + "         FROM person_address" 
    	        + "         WHERE voided = 0" 
    	        + "         GROUP BY person_id" 
    	        + "     ) pa2 ON pa1.person_id = pa2.person_id AND pa1.person_address_id = pa2.id" 
    	        + " ) pa ON pa.person_id = p.patient_id" 
    	        + " LEFT JOIN (" 
    	        + "     SELECT  patient_id, MAX(encounter_datetime) AS encounterDate" 
    	        + "     FROM  encounter e " 
    	        + "         inner join location l on l.location_id=e.location_id" 
    	        + "     WHERE voided = 0 AND encounter_type = 6 and l.uuid IN (" + Hl7Util.listToString(locationsByUuid) + ") " 
    	        + "     and l.retired=0" 
    	        + "     GROUP BY patient_id" 
    	        + " ) enc2 on enc2.patient_id=p.patient_id" 
    	        + " WHERE p.voided = 0" 
    	        + " AND pe.voided = 0" 
    	        + " AND l.uuid IN (" + Hl7Util.listToString(locationsByUuid) + ") " 
    	        + " GROUP BY pid.identifier;";

        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(PatientDemographic.class));

    }
}