package mz.org.csaude.hl7sync.dao.hl7filegenerator;

import java.util.List;

import mz.org.csaude.hl7sync.model.PatientDemographic;
import mz.org.csaude.hl7sync.util.Hl7Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Qualifier;



@Repository
public class HL7FileGeneratorDaoImpl implements HL7FileGeneratorDao {

    @Autowired
    @Qualifier("openmrsJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private String sql;

    public List<PatientDemographic> getPatientDemographicData(List<String> locationsByUuid) {

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
                + "                TRIM(IFNULL(pa.address3, '')),"
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
                + "        pat2.value"
                + "        WHEN 1057 THEN 'S'"
                + "        WHEN 5555 THEN 'M'"
                + "        WHEN 1060 THEN 'P'"
                + "        WHEN 1059 THEN 'W'"
                + "        WHEN 1056 THEN 'D'"
                + "        ELSE 'T'"
                + "    END marital_status,"
                + "    pid.location_id,"
                + "    pid.lName locationName,"
                + "    enc2.encounter_datetime lastConsultationDate"
                + " FROM"
                + "    person pe"
                + "    INNER JOIN patient p on pe.person_id = p.patient_id"
                + "    LEFT JOIN ("
                + "        SELECT"
                + "            pid1.*,"
                + "            pid2.lUuid lUuid,"
                + "            pid2.lName lName"
                + "        FROM"
                + "            patient_identifier pid1"
                + "            INNER JOIN ("
                + "                SELECT"
                + "                    patient_id,"
                + "                    MIN(patient_identifier_id) id,"
                + "                    l.uuid lUuid,"
                + "                    l.name lName"
                + "                FROM"
                + "                    patient_identifier pi"
                + "                    INNER JOIN location l on l.location_id = pi.location_id"
                + "                WHERE"
                + "                    pi.voided = 0"
                + "                    AND pi.identifier_type = 2"
                + "                    AND l.retired = 0"
                + "                GROUP BY"
                + "                    patient_id"
                + "            ) pid2"
                + "        WHERE"
                + "            pid1.patient_id = pid2.patient_id"
                + "            AND pid1.patient_identifier_id = pid2.id"
                + "    ) pid on pid.patient_id = p.patient_id"
                + "    LEFT JOIN ("
                + "        SELECT"
                + "            pn1.*"
                + "        FROM"
                + "            person_name pn1"
                + "            INNER JOIN ("
                + "                SELECT"
                + "                    person_id,"
                + "                    MIN(person_name_id) id"
                + "                FROM"
                + "                    person_name"
                + "                WHERE"
                + "                    voided = 0"
                + "                GROUP BY"
                + "                    person_id"
                + "            ) pn2"
                + "        WHERE"
                + "            pn1.person_id = pn2.person_id"
                + "            AND pn1.person_name_id = pn2.id"
                + "    ) pn on pn.person_id = p.patient_id"
                + "    LEFT JOIN ("
                + "        SELECT"
                + "            pa1.*"
                + "        FROM"
                + "            person_address pa1"
                + "            INNER JOIN ("
                + "                SELECT"
                + "                    person_id,"
                + "                    MIN(person_address_id) id"
                + "                FROM"
                + "                    person_address"
                + "                WHERE"
                + "                    voided = 0"
                + "                GROUP BY"
                + "                    person_id"
                + "            ) pa2"
                + "        WHERE"
                + "            pa1.person_id = pa2.person_id"
                + "            AND pa1.person_address_id = pa2.id"
                + "    ) pa on pa.person_id = p.patient_id"
                + "    LEFT JOIN ("
                + "        SELECT"
                + "            pat1.*"
                + "        FROM"
                + "            person_attribute pat1"
                + "            INNER JOIN ("
                + "                SELECT"
                + "                    person_id,"
                + "                    MIN(person_attribute_id) id"
                + "                FROM"
                + "                    person_attribute"
                + "                WHERE"
                + "                    voided = 0"
                + "                    AND person_attribute_type_id = 9"
                + "                GROUP BY"
                + "                    person_id"
                + "            ) pat2"
                + "        WHERE"
                + "            pat1.person_id = pat2.person_id"
                + "            AND pat1.person_attribute_id = pat2.id"
                + "    ) pat on pat.person_id = p.patient_id"
                + "    LEFT JOIN ("
                + "        SELECT"
                + "            pat12.*"
                + "        FROM"
                + "            person_attribute pat12"
                + "            INNER JOIN ("
                + "                SELECT"
                + "                    person_id,"
                + "                    MIN(person_attribute_id) id"
                + "                FROM"
                + "                    person_attribute"
                + "                WHERE"
                + "                    voided = 0"
                + "                    AND person_attribute_type_id = 14"
                + "                GROUP BY"
                + "                    person_id"
                + "            ) pat22"
                + "        WHERE"
                + "            pat12.person_id = pat22.person_id"
                + "            AND pat12.person_attribute_id = pat22.id"
                + "    ) pat1 on pat1.person_id = p.patient_id"
                + "    LEFT JOIN ("
                + "        SELECT"
                + "            pat121.*"
                + "        FROM"
                + "            person_attribute pat121"
                + "            INNER JOIN ("
                + "                SELECT"
                + "                    person_id,"
                + "                    MIN(person_attribute_id) id"
                + "                FROM"
                + "                    person_attribute"
                + "                WHERE"
                + "                    voided = 0"
                + "                    AND person_attribute_type_id = 5"
                + "                GROUP BY"
                + "                    person_id"
                + "            ) pat222"
                + "        WHERE"
                + "            pat121.person_id = pat222.person_id"
                + "            AND pat121.person_attribute_id = pat222.id"
                + "    ) pat2 on pat2.person_id = p.patient_id"
                + "    LEFT JOIN ("
                + "        SELECT"
                + "            e.*"
                + "        FROM"
                + "            encounter e"
                + "            INNER JOIN ("
                + "                SELECT"
                + "                    patient_id,"
                + "                    MAX(encounter_datetime) encounterDate"
                + "                FROM"
                + "                    encounter"
                + "                WHERE"
                + "                    voided = 0"
                + "                    AND encounter_type = 6"
                + "                GROUP BY"
                + "                    patient_id"
                + "            ) e1"
                + "        WHERE"
                + "            e.patient_id = e1.patient_id"
                + "            AND e.encounter_datetime = e1.encounterDate"
                + "    ) enc2 ON enc2.patient_id = p.patient_id"
                + " WHERE"
                + "    p.voided = 0"
                + "    AND pe.voided = 0"
                + "    AND LENGTH(pid.identifier) = 21"
                + "    AND pid.lUuid IN (" + Hl7Util.listToString(locationsByUuid) + ")"
                + " GROUP BY"
                + "    pid.identifier;";

        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(PatientDemographic.class));
    }
}