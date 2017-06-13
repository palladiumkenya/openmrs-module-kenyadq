/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at http://license.openmrs.org
 * <p/>
 * Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either
 * express or implied. See the License for the specific language governing rights and limitations under the License.
 * <p/>
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.kenyadq.api.impl;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.APIException;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.kenyacore.CoreConstants;
import org.openmrs.module.kenyadq.DqConstants;
import org.openmrs.module.kenyadq.api.KenyaDqService;
import org.openmrs.module.kenyadq.api.db.KenyaDqDao;
import org.openmrs.module.kenyadq.utils.KenyaDqUtils;
import org.openmrs.module.kenyaemr.Metadata;
import org.openmrs.serialization.SerializationException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Module service implementation
 */
public class KenyaDqServiceImpl extends BaseOpenmrsService implements KenyaDqService {

    @Autowired
    private PatientService patientService;

    @Autowired
    private ConceptService conceptService;

    @Autowired
    private DataWarehouseServiceImpl dwPatientExtractService;

    @Autowired
    private LocationService locationService;

    private KenyaDqDao dao;

    private DataWarehouseQueries dwq = new DataWarehouseQueries();

    protected static final Log log = LogFactory.getLog(KenyaDqServiceImpl.class);

    /**
     * @see org.openmrs.module.kenyadq.api.KenyaDqService#mergePatients(org.openmrs.Patient, org.openmrs.Patient)
     */
    public void mergePatients(Patient preferred, Patient notPreferred) throws APIException {
        try {
            Set<PatientIdentifier> preferredPatientIdentifiers = new HashSet<PatientIdentifier>(preferred
                    .getActiveIdentifiers());

            patientService.mergePatients(preferred, notPreferred);

            for (Map.Entry<PatientIdentifierType, List<PatientIdentifier>> entry : getAllPatientIdentifiers
                    (preferred).entrySet()) {
                List<PatientIdentifier> idsForType = entry.getValue();

                if (idsForType.size() > 1) {
                    PatientIdentifier keep = null;

                    // Look for first identifier of this type from the preferred patient
                    for (PatientIdentifier identifier : idsForType) {
                        boolean wasPreferredPatients = preferredPatientIdentifiers.contains(identifier);
                        if (keep == null && wasPreferredPatients) {
                            keep = identifier;
                        }
                    }

                    // If preferred patient didn't have one these, use first one from non-preferred patient
                    if (keep == null) {
                        keep = idsForType.get(0);
                    }

                    for (PatientIdentifier identifier : idsForType) {
                        if (identifier != keep) {
                            // Void if identifier originally belonged to preferred patient
                            if (preferredPatientIdentifiers.contains(identifier)) {
                                patientService.voidPatientIdentifier(identifier, "Removing duplicate after merge");
                            }
                            // Purge otherwise as it was just created by PatientServiceImpl.mergeIdentifiers(...)
                            else {
                                preferred.removeIdentifier(identifier);
                                patientService.purgePatientIdentifier(identifier);
                            }
                        }
                    }
                }
            }
        } catch (SerializationException ex) {
            throw new APIException(ex);
        }
    }

    @Override
    public List<Object> executeSqlQuery(String query, Map<String, Object> substitutions) {
        return dao.executeSqlQuery(query, substitutions);
    }

    @Override
    public List<Object> executeHqlQuery(String query, Map<String, Object> substitutions) {
        return dao.executeHqlQuery(query, substitutions);
    }

    /**
     * Helper method to get all of a patient's identifiers organized by type
     *
     * @param patient the patient
     * @return the map of identifier types to identifiers
     */
    protected Map<PatientIdentifierType, List<PatientIdentifier>> getAllPatientIdentifiers(Patient patient) {
        Map<PatientIdentifierType, List<PatientIdentifier>> ids = new HashMap<PatientIdentifierType,
                List<PatientIdentifier>>();
        for (PatientIdentifier identifier : patient.getActiveIdentifiers()) {
            PatientIdentifierType idType = identifier.getIdentifierType();
            List<PatientIdentifier> idsForType = ids.get(idType);

            if (idsForType == null) {
                idsForType = new ArrayList<PatientIdentifier>();
                ids.put(idType, idsForType);
            }

            idsForType.add(identifier);
        }

        return ids;
    }

    public KenyaDqDao getDao() {
        return dao;
    }

    public void setDao(KenyaDqDao dao) {
        this.dao = dao;
    }

    public byte[] downloadCsvFile(List<Object> data, Object[] headerRow) {
        StringWriter stringWriter = new StringWriter();
        final char csvDelimeter = ',';
        CSVWriter writer = new CSVWriter(stringWriter);
        try {
            if (headerRow != null) {
                data.add(0, headerRow);
            }
            for (Object object : data) {
                Object[] values = (Object[]) object;
                String[] row = new String[values.length];
                int i = 0;
                for (Object value : values) {
                    row[i] = value != null ? value.toString() : null;
                    i++;
                }
                writer.writeNext(row);
            }
            return stringWriter.toString().getBytes();
        } catch (Exception ex) {
            throw new RuntimeException("Could not download data dictionary.");
        }
    }

    public byte[] downloadAnalysisFile() {
        List<Object> headerRow = new ArrayList<Object>();
        initColumnHeaders(headerRow);
        List<Object> data = dao.executeSqlQuery(select() + dynamic(headerRow) + from(),
                new HashMap<String, Object>());
        return downloadCsvFile(data, headerRow.toArray());
    }

    public byte[] downloadDataDictionary() {
        Object[] headerRow = new Object[4];
        headerRow[0] = "concept_id";
        headerRow[1] = "concept_name";
        headerRow[2] = "concept_description";
        headerRow[3] = "concept_type";
        List<Object> data = new ArrayList<Object>();
        List<String> kenyaEmrConceptUuids = getKenyaEmrConceptUuids("answer_concepts_2015-06-08.csv");
        for (Concept concept : conceptService.getAllConcepts()) {
            if (!kenyaEmrConceptUuids.contains(concept.getUuid())) {
                continue;
            }
            Object[] row = new Object[4];
            row[0] = concept.getId().toString();
            row[1] = concept.getPreferredName(CoreConstants.LOCALE).getName();
            ConceptDescription cd = concept.getDescription(CoreConstants.LOCALE);
            String description = cd != null ? cd.getDescription() : "";
            row[2] = description;
            row[3] = concept.getDatatype().getName();
            data.add(row);
        }
        return downloadCsvFile(data, headerRow);
    }

    public byte[] downloadPatientExtract() {
        String query = "select pd.unique_patient_no as PatientID, pd.patient_id as PatientPK, :siteCode, " +
                ":facilityName, pd.Gender, pd.DOB, '' as RegistrationDate, he.date_first_enrolled_in_care as " +
                "RegistrationAtCCC, (select eme.visit_date from kenyaemr_etl.etl_mch_enrollment eme where patient_id " +
                "= pd.patient_id order by eme.visit_date desc limit 1) as RegistrationATPMTCT, (select ete" +
                ".date_first_enrolled_in_tb_care from kenyaemr_etl.etl_tb_enrollment ete where patient_id = pd" +
                ".patient_id order by ete.date_first_enrolled_in_tb_care desc limit 1) as RegistrationAtTBClinic, " +
                "(select cn.name from concept_name cn where cn.concept_id = he.entry_point and cn.locale=:locale " +
                "order by field(cn.concept_name_type, 'SHORT','FULLY_SPECIFIED', 'NULL') LIMIT 1) as PatientSource, " +
                "'' as Region, '' as District, '' as Village, pd.next_of_kin, (select ehf.visit_date " +
                "from kenyaemr_etl.etl_patient_hiv_followup ehf where ehf.patient_id = pd.patient_id order by 1 DESC " +
                "limit 1) as LastVisit, pd.marital_status as MaritalStatus, pd.education_level as EducationLevel, he" +
                ".date_confirmed_hiv_positive, '' as PreviousARTExposure, he" +
                ".date_started_art_at_transferring_facility as PreviousARTStartDate, " +
                ":emr, :project from kenyaemr_etl.etl_patient_demographics pd left join kenyaemr_etl" +
                ".etl_hiv_enrollment he on pd.patient_id = he.patient_id group by pd.patient_id;";

        Map<String, Object> substitutions = new HashMap<String, Object>();
        substitutions.put("siteCode", KenyaDqUtils.getMflCode());
        substitutions.put("facilityName", KenyaDqUtils.getFacilityName());
        substitutions.put("emr", DqConstants.EMR);
        substitutions.put("project", DqConstants.PROJECT);
        substitutions.put("locale", CoreConstants.LOCALE);

        List<Object> patientExtracts = dao.executeSqlQuery(query, substitutions);
        return downloadCsvFile(patientExtracts, dwPatientExtractService.getPatientHeaderRow().toArray());
    }

    public byte[] downloadPatientStatusExtract() {
        String query = "select pd.unique_patient_no as PatientID, pd.patient_id as PatientPK, :siteCode, " +
                ":facilityName, \"\", he.date_of_discontinuation, he.discontinuation_reason from kenyaemr_etl" +
                ".etl_patient_demographics pd left join kenyaemr_etl.etl_hiv_enrollment he on pd.patient_id = he" +
                ".patient_id group by pd.patient_id;";

        Map<String, Object> substitutions = new HashMap<String, Object>();
        substitutions.put("siteCode", KenyaDqUtils.getMflCode());
        substitutions.put("facilityName", KenyaDqUtils.getFacilityName());

        return downloadCsvFile(dao.executeSqlQuery(query, substitutions), dwPatientExtractService
                .getPatientStatusHeaderRow().toArray());
    }

    public byte[] downloadPatientVisitExtract() {
        String query = "select pd.unique_patient_no as PatientID, pd.patient_id as PatientPK, :facilityName, " +
                ":siteCode, ehf.visit_id, ehf.visit_date, (select name from encounter_type et join encounter e on " +
                "et.encounter_type_id = e.encounter_type where encounter_id = ehf.encounter_id) as Service, case ehf" +
                ".visit_scheduled when 1 then 'Scheduled' when 0 then 'Unscheduled' end as VisitType, (select cn.name" +
                " from concept_name cn where cn.concept_id = ehf.who_stage and cn.locale=:locale order by field(cn" +
                ".concept_name_type, 'SHORT','FULLY_SPECIFIED') DESC LIMIT 1) as WHOStage, '' as WABStage, (select cn" +
                ".name from concept_name cn where cn.concept_id = ehf.pregnancy_status and cn.locale=:locale order by" +
                " " +
                "field(cn.concept_name_type, 'SHORT','FULLY_SPECIFIED') DESC LIMIT 1) as Pregnant, ehf" +
                ".last_menstrual_period as LMP, ehf.expected_delivery_date as EDD, ehf.height, ehf.weight, concat(ehf" +
                ".systolic_pressure,'/',ehf.diastolic_pressure) as BP, '' as OI, '' as OIDate, ehf" +
                ".substitution_first_line_regimen_date, ehf.substitution_first_line_regimen_reason, ehf" +
                ".substitution_second_line_regimen_date, ehf.substitution_second_line_regimen_reason, ehf" +
                ".second_line_regimen_change_date, ehf.second_line_regimen_change_reason, concat(ifnull((select cn" +
                ".name from concept_name cn where cn.concept_id = ehf.ctx_adherence and cn.locale=:locale order by " +
                "field" +
                "(cn.concept_name_type, 'SHORT','FULLY_SPECIFIED') DESC LIMIT 1),'N/A'),' - ', ifnull((select cn.name" +
                " from concept_name cn where cn.concept_id = ehf.arv_adherence and cn.locale=:locale order by field" +
                "(cn" +
                ".concept_name_type, 'SHORT','FULLY_SPECIFIED') DESC LIMIT 1),'N/A')) as Adherence, concat ('CTX', ' " +
                "- ', 'ART') as AdherenceCategory, (select cn.name from concept_name cn where cn.concept_id = ehf" +
                ".family_planning_method and cn.locale=:locale order by field(cn.concept_name_type, 'SHORT'," +
                "'FULLY_SPECIFIED') DESC LIMIT 1) as FamilyPlanningMethod, trim(trailing ',' from concat(if(ehf" +
                ".condom_provided, 'Condoms,',''), if(ehf.pwp_disclosure, 'Disclosure,',''), if(ehf" +
                ".pwp_partner_tested, 'Partner Testing,',''), if(ehf.screened_for_sti, 'Screened STI',''))) AS PwP, " +
                "concat(timestampdiff(DAY, ehf.last_menstrual_period, ehf.visit_date), \" days\") AS GestationAge, " +
                "ehf.next_appointment_date as NextAppointmentDate from kenyaemr_etl.etl_patient_demographics pd join " +
                "kenyaemr_etl.etl_patient_hiv_followup ehf on pd.patient_id = ehf.patient_id";

        Map<String, Object> substitutions = new HashMap<String, Object>();
        substitutions.put("siteCode", KenyaDqUtils.getMflCode());
        substitutions.put("facilityName", KenyaDqUtils.getFacilityName());
        substitutions.put("locale", CoreConstants.LOCALE);

        return downloadCsvFile(dao.executeSqlQuery(query, substitutions), dwPatientExtractService
                .getPatientVisitHeaderRow().toArray());
    }

    public byte[] downloadPatientLaboratoryExtract() {
        String query = "select pd.unique_patient_no as PatientID, pd.patient_id as PatientPK, :siteCode, " +
                ":facilityName, le.visit_id, \"\" as OrderedByDate, le.date_test_requested as ReportedByDate, " +
                "(select cn.name from concept_name cn where cn.concept_id = le.lab_test and cn.locale=:locale order " +
                "by " +
                "field(cn.concept_name_type, 'SHORT','FULLY_SPECIFIED', 'NULL') LIMIT 1) as TestName, (select cn.name" +
                " from concept_name cn where cn.concept_id = le.test_result and cn.locale=:locale order by field(cn" +
                ".concept_name_type, 'SHORT','FULLY_SPECIFIED', 'NULL') LIMIT 1) as TestResult from kenyaemr_etl" +
                ".etl_patient_demographics pd join kenyaemr_etl.etl_laboratory_extract le on pd.patient_id=le" +
                ".patient_id;";

        Map<String, Object> substitutions = new HashMap<String, Object>();
        substitutions.put("siteCode", KenyaDqUtils.getMflCode());
        substitutions.put("facilityName", KenyaDqUtils.getFacilityName());
        substitutions.put("locale", CoreConstants.LOCALE);

        return downloadCsvFile(dao.executeSqlQuery(query, substitutions), dwPatientExtractService
                .getPatientLaboratoryHeaderRow().toArray());
    }

    public byte[] downloadPatientPharmacyExtract() {
        String query = "select pd.unique_patient_no as PatientID,  :siteCode, :facilityName, pd.patient_id as " +
                "PatientPK, pe.visit_id, pe.drug, pe.date_created, pe.duration, case pe.duration_units when 'Days' " +
                "then date_add(pe.date_created, interval pe.duration DAY) when 'Weeks' then date_add(pe.date_created," +
                " interval pe.duration WEEK) when 'Months' then date_add(pe.date_created, interval pe.duration MONTH)" +
                " else \"\" end as ExpectedReturn, \"\",\"\",\"\" from kenyaemr_etl.etl_patient_demographics pd join " +
                "kenyaemr_etl.etl_pharmacy_extract pe on pd.patient_id = pe.patient_id;";

        Map<String, Object> substitutions = new HashMap<String, Object>();
        substitutions.put("siteCode", KenyaDqUtils.getMflCode());
        substitutions.put("facilityName", KenyaDqUtils.getFacilityName());

        return downloadCsvFile(dao.executeSqlQuery(query, substitutions), dwPatientExtractService
                .getPatientPharmacyExtractHeaderRow().toArray());
    }

    public byte[] downloadPatientWABWHOCD4Extract() {
        String query = "select epd.unique_patient_no as PatientID, epd.patient_id as PatientPK, :facilityID, " +
                ":siteCode, e_cd4.test_result as eCD4, e_cd4.date_test_requested as eCD4Date, e_who.test_result as" +
                " eWHO, e_who.date_test_requested as eWHODate, b_cd4.test_result as bCD4, b_cd4.date_test_requested " +
                "as bCD4Date, b_who.test_result as bWHO, b_who.date_test_requested as bWHODate, l_who.test_result as " +
                "lastWHO, l_who.date_test_requested as lastWHODate, l_cd4.test_result as lastCD4, " +
                "l_cd4.date_test_requested as lastCD4Date, m_12_cd4.test_result as m12CD4, " +
                "m_12_cd4.date_test_requested as m12CD4Date, m_6_cd4.test_result as m6CD4, " +
                "m_6_cd4.date_test_requested as m6CD4Date from kenyaemr_etl.etl_patient_demographics epd left join " +
                "(select ele.patient_id, ele.test_result, ele.date_test_requested from kenyaemr_etl" +
                ".etl_laboratory_extract ele join kenyaemr_etl.etl_hiv_enrollment ehe on ele.patient_id = ehe" +
                ".patient_id where lab_test = :cd4_count and timestampdiff(DAY, ehe.visit_date, ele" +
                ".date_test_requested) < " +
                "90 group by ele.patient_id order by 1) e_cd4 on e_cd4.patient_id = epd.patient_id left join (select " +
                "ele.patient_id, ele.test_result, ele.date_test_requested from kenyaemr_etl.etl_laboratory_extract " +
                "ele join kenyaemr_etl.etl_hiv_enrollment ehe on ele.patient_id = ehe.patient_id where lab_test = " +
                ":who_stage and timestampdiff(DAY, ehe.visit_date, ele.date_test_requested) < 90 group by ele" +
                ".patient_id " +
                "order by 1) e_who on epd.patient_id = e_who.patient_id left join (select ele.patient_id, ele" +
                ".test_result, ele.date_test_requested from kenyaemr_etl.etl_laboratory_extract ele join kenyaemr_etl" +
                ".etl_pharmacy_extract epe on ele.patient_id = epe.patient_id where lab_test = :cd4_count and " +
                "timestampdiff" +
                "(DAY, epe.date_created, ele.date_test_requested) < 90 group by ele.patient_id order by epe.id) b_cd4" +
                " on b_cd4.patient_id = epd.patient_id left join (select ele.patient_id, ele.test_result, ele" +
                ".date_test_requested from kenyaemr_etl.etl_laboratory_extract ele join kenyaemr_etl" +
                ".etl_pharmacy_extract epe on ele.patient_id = epe.patient_id where lab_test = :who_stage and " +
                "timestampdiff" +
                "(DAY, epe.date_created, ele.date_test_requested) < 90 group by ele.patient_id order by epe.id) b_who" +
                " on epd.patient_id = b_who.patient_id left join (select ele.patient_id, ele.test_result, ele" +
                ".date_test_requested from kenyaemr_etl.etl_laboratory_extract ele where lab_test = :who_stage and " +
                "ele.id " +
                "in (select max(id) from kenyaemr_etl.etl_laboratory_extract group by patient_id)) l_who on l_who" +
                ".patient_id = epd.patient_id left join (select ele.patient_id, ele.test_result, ele" +
                ".date_test_requested from kenyaemr_etl.etl_laboratory_extract ele where lab_test = :cd4_count and " +
                "ele.id " +
                "in (select max(id) from kenyaemr_etl.etl_laboratory_extract group by patient_id)) l_cd4 on " +
                "l_cd4.patient_id = epd.patient_id left join (select ele.patient_id, ele" +
                ".test_result, ele.date_test_requested from kenyaemr_etl.etl_laboratory_extract ele join kenyaemr_etl" +
                ".etl_pharmacy_extract epe on ele.patient_id" +
                " = epe.patient_id where lab_test = :cd4_count and (timestampdiff(DAY, epe.date_created, ele" +
                ".date_test_requested) between 330 and 390) group by ele.patient_id order by epe.id) m_12_cd4 on " +
                "m_12_cd4.patient_id = epd.patient_id left join (select ele.patient_id, ele.test_result, ele" +
                ".date_test_requested from kenyaemr_etl.etl_laboratory_extract ele join kenyaemr_etl" +
                ".etl_pharmacy_extract epe on ele.patient_id = epe.patient_id where lab_test = :cd4_count and " +
                "(timestampdiff(DAY, epe.date_created, ele.date_test_requested) between 150 and 210) group by ele" +
                ".patient_id order by epe.id) m_6_cd4 on m_6_cd4.patient_id = epd.patient_id;";

        Map<String, Object> substitutions = new HashMap<String, Object>();
        substitutions.put("siteCode", KenyaDqUtils.getMflCode());
        substitutions.put("facilityID", KenyaDqUtils.getFacilityId());
        substitutions.put("cd4_count", conceptService.getConceptByUuid(Metadata.Concept.CD4_COUNT).getConceptId());
        substitutions.put("who_stage", conceptService.getConceptByUuid(Metadata.Concept.CURRENT_WHO_STAGE)
                .getConceptId());

        return downloadCsvFile(dao.executeSqlQuery(query, substitutions), dwPatientExtractService
                .getPatientWABWHOCD4ExtractHeaderRow().toArray());
    }

    public byte[] downloadARTPatientExtract() {
        String query = "select epd.patient_id as PatientPK, epd.unique_patient_no as PatientID, if(" +
                "(@ageEnrollment/*'*/:=/*'*/timestampdiff(YEAR, epd.dob, ehe.date_first_enrolled_in_care)) > 0, " +
                "concat(@ageEnrollment, ' Years'), concat(@ageEnrollment/*'*/:=/*'*/timestampdiff(MONTH, epd.dob, ehe" +
                ".date_first_enrolled_in_care), ' months')) as AgeEnrollment, if(" +
                "(@ageARTStart/*'*/:=/*'*/timestampdiff(YEAR," +
                " epd.dob, @startARTDate/*'*/:=/*'*/if(ehe.date_started_art_at_transferring_facility is not null, ehe" +
                ".date_started_art_at_transferring_facility, (select visit_date from kenyaemr_etl" +
                ".etl_pharmacy_extract where patient_id=ede.patient_id and is_arv=1 order by 1 asc limit 1)))) > 0, " +
                "concat(@ageARTStart, ' years'), concat(@ageARTStart/*'*/:=/*'*/timestampdiff(MONTH, epd.dob, " +
                "@startARTDate/*'*/:=/*'*/if(ehe.date_started_art_at_transferring_facility is not null, ehe" +
                ".date_started_art_at_transferring_facility, (select visit_date from kenyaemr_etl" +
                ".etl_pharmacy_extract where patient_id=ede.patient_id and is_arv=1 order by 1 asc limit 1))), ' " +
                "months')) as AgeARTStart, if((@ageLastVisit/*'*/:=/*'*/timestampdiff(YEAR, epd.dob, ehf.visit_date))" +
                " > 0, concat(@ageLastVisit, ' years'), concat(@ageLastVisit/*'*/:=/*'*/timestampdiff(MONTH, epd.dob," +
                " ehf.visit_date), ' months')) as AgeLastVisit, :siteCode, :facilityName, \"RegistrationDate\", " +
                "(select cn.name from concept_name cn where cn.concept_id = ehe.entry_point and cn.locale=:locale " +
                "order by field(cn.concept_name_type, 'SHORT','FULLY_SPECIFIED', 'NULL') LIMIT 1) as PatientSource,  " +
                "epd.gender, @startARTDate as StartARTDate, ehe.date_started_art_at_transferring_facility as " +
                "PreviousARTStartDate, '' as PreviousARTRegimen, (select visit_date from kenyaemr_etl" +
                ".etl_pharmacy_extract where patient_id=ede.patient_id and is_arv=1 order by 1 asc limit 1) as " +
                "StartARTAtThisFacility, ede.regimen, ede.regimen_line, (select epe.date_created from kenyaemr_etl" +
                ".etl_pharmacy_extract epe where epe.patient_id=ede.patient_id and epe.is_arv=1 order by 1 desc limit" +
                " 1) as LastARTDate, (select epe.regimen from kenyaemr_etl.etl_pharmacy_extract epe where epe" +
                ".patient_id=ede.patient_id and epe.is_arv=1 order by 1 desc limit 1) as LastRegimen, \"\" as " +
                "LastRegimenLine, \"\" as Duration, \"\" as ExpectedReturn,  ehf.visit_date, ehe" +
                ".discontinuation_reason as ExitReason, ehe.date_of_discontinuation as ExitDate from kenyaemr_etl" +
                ".etl_drug_event ede join kenyaemr_etl.etl_patient_demographics epd on " +
                "ede.patient_id = epd.patient_id left join kenyaemr_etl.etl_hiv_enrollment ehe on ede.patient_id = " +
                "ehe.patient_id left join kenyaemr_etl.etl_patient_hiv_followup ehf on ede.patient_id=ehf.patient_id " +
                "where ede.voided=0 group by ede.id";

        Map<String, Object> substitutions = new HashMap<String, Object>();
        substitutions.put("siteCode", KenyaDqUtils.getMflCode());
        substitutions.put("facilityName", KenyaDqUtils.getFacilityName());
        substitutions.put("locale", CoreConstants.LOCALE);

        return downloadCsvFile(dao.executeSqlQuery(query, substitutions), dwPatientExtractService
                .getARTPatientExtractHeaderRow().toArray());
    }

    public byte[] downloadAll() {
        String mfl = KenyaDqUtils.getMflCode();
        Map<String, byte[]> contents = new HashMap<String, byte[]>();
        contents.put("ARTPatientExtract" + "-" + location() + "-" + timeStamp() + mfl + ".csv",
                downloadARTPatientExtract());
        contents.put("PatientExtract" + "-" + location() + "-" + timeStamp() + mfl + ".csv", downloadPatientExtract());
        contents.put("PatientLaboratoryExtract" + "-" + location() + "-" + timeStamp() + mfl + ".csv",
                downloadPatientLaboratoryExtract());
        contents.put("PatientPharmacyExtract" + "-" + location() + "-" + timeStamp() + mfl + ".csv",
                downloadPatientPharmacyExtract());
        contents.put("PatientStatusExtract" + "-" + location() + "-" + timeStamp() + mfl + ".csv",
                downloadPatientStatusExtract());
        contents.put("PatientVisitExtract" + "-" + location() + "-" + timeStamp() + mfl + ".csv",
                downloadPatientVisitExtract());
        contents.put("PatientWABWHOCD4Extract" + "-" + location() + "-" + timeStamp() + mfl + ".csv",
                downloadPatientWABWHOCD4Extract());
        byte[] ret = null;
        try {
            ret = dwPatientExtractService.zipBytes(contents);
        } catch (Exception ex) {

        }
        return ret;
    }

    public String timeStamp() {
        return dwPatientExtractService.timeStamp();
    }

    public String location() {
        return dwPatientExtractService.location();
    }

    private String getUpn() {
        PatientService patientService = null;
        PatientIdentifier patientIdentifier = patientService
                .getPatientIdentifierByUuid(Metadata.IdentifierType.UNIQUE_PATIENT_NUMBER);
        return patientIdentifier != null ? patientIdentifier.getIdentifier() : "";
    }

    private void initColumnHeaders(List<Object> columnHeaders) {
        columnHeaders.add("encounter_uuid");
        columnHeaders.add("encounter_type");
        columnHeaders.add("encounter_location");
        columnHeaders.add("encounter_datetime");
        columnHeaders.add("encounter_voided");
        columnHeaders.add("person_uuid");
        columnHeaders.add("given_name");
        columnHeaders.add("middle_name");
        columnHeaders.add("family_name");
        columnHeaders.add("gender");
        columnHeaders.add("birthdate");
        columnHeaders.add("birthdate_estimated");
        columnHeaders.add("dead");
        columnHeaders.add("death_date");
        columnHeaders.add("cause_of_death");
//		headerRow.add("telephone");
        columnHeaders.add("postal_address");
        columnHeaders.add("school_employer_address");
        columnHeaders.add("location");
        columnHeaders.add("village_estate");
        columnHeaders.add("county");
        columnHeaders.add("district");
        columnHeaders.add("sub_location");
        columnHeaders.add("landmark");
//		headerRow.add("subchief");
        columnHeaders.add("province");
        columnHeaders.add("division");
        columnHeaders.add("house_no");
        columnHeaders.add("patient_voided");
        columnHeaders.add("visit_uuid");
        columnHeaders.add("visit_type");
        columnHeaders.add("visit_start_date");
        columnHeaders.add("visit_end_date");
        columnHeaders.add("obs_uuid");
    }

    private String select() {
        String select =
                "SELECT\n" +
                        "\tCAST(e.uuid AS CHAR) AS encounter_uuid,\n" +
                        "\tt.`name` AS encounter_type,\n" +
                        "\tl.`name` AS encounter_location,\n" +
                        "\te.encounter_datetime,\n" +
                        "\te.voided AS encounter_voided,\n" +
                        "\tCAST(p.uuid AS CHAR) AS person_uuid,\n" +
                        "\tn.given_name,\n" +
                        "\tn.middle_name,\n" +
                        "\tn.family_name,\n" +
                        "\tp.gender,\n" +
                        "\tp.birthdate,\n" +
                        "\tp.birthdate_estimated,\n" +
                        "\tp.dead,\n" +
                        "\tp.death_date,\n" +
                        "\tp.cause_of_death,\n" +
//						"\ta.address1 AS telephone,\n" +
                        "\ta.address1 AS postal_address,\n" +
                        "\ta.address3 AS school_employer_address,\n" +
                        "\ta.address6 AS location,\n" +
                        "\ta.city_village AS village_estate,\n" +
                        "\ta.country AS county,\n" +
                        "\ta.county_district AS district,\n" +
                        "\ta.address5 AS sub_location,\n" +
                        "\ta.address2 AS landmark,\n" +
//						"\ta.address5 AS subchief,\n" +
                        "\ta.state_province AS province,\n" +
                        "\ta.address4 AS division,\n" +
                        "\ta.postal_code AS house_no,\n" +
                        "\tp.voided AS patient_voided,\n" +
                        "\tCAST(v.uuid AS CHAR) AS visit_uuid,\n" +
                        "\tvt.`name` AS visit_type,\n" +
                        "\tv.date_started AS visit_start_date,\n" +
                        "\tv.date_stopped AS visit_end_date,\n" +
                        "\tCAST(o.uuid AS CHAR) AS obs_uuid,\n";
        return select;
    }

    private String dynamic(List<Object> columnHeaders) {
        List<Concept> concepts = conceptService.getAllConcepts();
        List<String> kenyaEmrConceptUuids = getKenyaEmrConceptUuids("question_concepts_2015-05-27.csv");
        String dynamic = "";
        for (Concept concept : concepts) {
            if (!kenyaEmrConceptUuids.contains(concept.getUuid())) {
                continue;
            }
            ConceptDatatype cd = concept.getDatatype();
            String valueColumn = "";
            if ("Boolean".equalsIgnoreCase(cd.getName())) {
                valueColumn = "value_boolean";
            } else if ("Coded".equalsIgnoreCase(cd.getName())) {
                valueColumn = "value_coded";
            } else if ("".equalsIgnoreCase(cd.getName())) {
                valueColumn = "value_drug";
            } else if ("Date".equalsIgnoreCase(cd.getName())
                    || "Time".equalsIgnoreCase(cd.getName())
                    || "Datetime".equalsIgnoreCase(cd.getName())) {
                valueColumn = "value_datetime";
            } else if ("Numeric".equalsIgnoreCase(cd.getName())) {
                valueColumn = "value_numeric";
            } else if ("Text".equalsIgnoreCase(cd.getName())
                    || "Structured Numeric".equalsIgnoreCase(cd.getName())) {
                valueColumn = "value_text";
            } else if ("Document".equalsIgnoreCase(cd.getName())) {
                valueColumn = "value_complex";
            }
            if (!"".equalsIgnoreCase(valueColumn)) {
                String conceptName = escape(concept.getPreferredName(CoreConstants.LOCALE).getName());
                dynamic += "\tCASE WHEN o.concept_id = " + concept.getId() + " THEN o." + valueColumn
                        + " END '" + conceptName + "',\n";
                columnHeaders.add(conceptName);
            }
        }
        return trimTraillingComma(dynamic);
    }

    private String from() {
        String from =
                "\n\tFROM\n" +
                        "\tencounter e\n" +
                        "INNER JOIN\n" +
                        "\tencounter_type t ON e.encounter_type = t.encounter_type_id\n" +
                        "INNER JOIN\n" +
                        "\tlocation l ON e.location_id = l.location_id\n" +
                        "INNER JOIN\n" +
                        "\tperson p ON e.patient_id = p.person_id\n" +
                        "INNER JOIN\n" +
                        "\tperson_name n ON p.person_id = n.person_id\n" +
                        "INNER JOIN\n" +
                        "\tperson_address a ON p.person_id = a.person_id\n" +
                        "INNER JOIN\n" +
                        "\tvisit v ON e.visit_id = v.visit_id\n" +
                        "INNER JOIN\n" +
                        "\tvisit_type vt ON v.visit_type_id = vt.visit_type_id\n" +
                        "INNER JOIN\n" +
                        "\tobs o ON e.encounter_id = o.encounter_id\n" +
                        "WHERE\n" +
                        "\tn.voided = 0\n" +
                        "GROUP BY\n" +
                        "\te.encounter_id\n" +
                        "ORDER BY\n" +
                        "\te.encounter_id;";
        return from;
    }

    private List<String> getKenyaEmrConceptUuids(String fileName) {
        List<String> conceptUuids = new ArrayList<String>();
        CSVReader reader = null;
        InputStream in = null;
        try {
            in = getClass().getClassLoader().getResourceAsStream("metadata/" + fileName);
            reader = new CSVReader(new InputStreamReader(in));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length > 0) {
                    String conceptUuid = nextLine[0];
                    if (conceptUuid != null && !conceptUuids.contains(conceptUuid)) {
                        conceptUuids.add(conceptUuid);
                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Could not read KenyaEMR concepts metadata file.");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            }
        }
        Collections.sort(conceptUuids);
        return conceptUuids;
    }

    private String trimTraillingComma(String untrimmed) {
        String trimmed = untrimmed.substring(0, untrimmed.length() - 2);
        return trimmed;
    }

    private String escape(String string) {
        return string.replace("'", "''");
    }

    //testDW using flat table
    public byte[] downloadFlatPatientLabExtract() {
        List<Object> headerRow = dwq.getLabExtractHeaderRow();
        List<Object> data = dao.executeSqlQuery(dwq.labExtractQuery(),
                new HashMap<String, Object>());
        return downloadCsvFile(data, headerRow.toArray());
    }

    public byte[] downloadFlatPatientVisitExtract() {
        List<Object> headerRow = dwq.getPatientVisitHeaderRow();
        List<Object> data = dao.executeSqlQuery(dwq.getPatientVisitQuery(),
                new HashMap<String, Object>());
        return downloadCsvFile(data, headerRow.toArray());
    }

    public byte[] downloadFlatPatientExtract() {
        List<Object> headerRow = dwq.getPatientHeaderRow();
        List<Object> data = dao.executeSqlQuery(dwq.getPatientExtractQuery(),
                new HashMap<String, Object>());
        return downloadCsvFile(data, headerRow.toArray());
    }

    public byte[] downloadFlatPatientStatusExtract() {
        List<Object> headerRow = dwq.getPatientStatusHeaderRow();
        List<Object> data = dao.executeSqlQuery(dwq.PatientStatusExtractQuery(),
                new HashMap<String, Object>());
        return downloadCsvFile(data, headerRow.toArray());
    }

    public byte[] downloadFlatARTPatientExtract() {
        List<Object> headerRow = dwq.getARTPatientExtractHeaderRow();
        List<Object> data = dao.executeSqlQuery(dwq.ARTPatientExtracQuery(),
                new HashMap<String, Object>());
        return downloadCsvFile(data, headerRow.toArray());
    }

    public byte[] downloadFlatPatientPharmacyExtract() {
        List<Object> headerRow = dwq.getPatientPharmacyExtractHeaderRow();
        List<Object> data = dao.executeSqlQuery(dwq.pharmacyExtractQuery(),
                new HashMap<String, Object>());
        return downloadCsvFile(data, headerRow.toArray());
    }

    public byte[] downloadFlatPatientWABWHOCD4Extract() {
        List<Object> headerRow = dwq.getPatientWABWHOCD4ExtractHeaderRow();
        List<Object> data = dao.executeSqlQuery(dwq.getPatientWABWHOCD4ExtractQuery(),
                new HashMap<String, Object>());
        return downloadCsvFile(data, headerRow.toArray());
    }



    @Override
    public byte[] downloadFlatAll() {
        Map<String, byte[]> contents = new HashMap<String, byte[]>();
        contents.put("ARTPatientExtract" + "-" + location() + "-" + timeStamp()  + ".csv", downloadFlatARTPatientExtract());
        contents.put("PatientExtract" + "-" + location() + "-" + timeStamp() + ".csv", downloadFlatPatientExtract());
        contents.put("PatientLaboratoryExtract" + "-" + location() + "-" + timeStamp() + ".csv", downloadFlatPatientLabExtract());
        contents.put("PatientPharmacyExtract" + "-" + location() + "-" + timeStamp() + ".csv", downloadFlatPatientPharmacyExtract());
        contents.put("PatientStatusExtract" + "-" + location() + "-" + timeStamp()  + ".csv", downloadFlatPatientStatusExtract());
        contents.put("PatientVisitExtract" + "-" + location() + "-" + timeStamp() + ".csv", downloadFlatPatientVisitExtract());
        contents.put("PatientWABWHOCD4Extract" + "-" + location() + "-" + timeStamp() + ".csv", downloadFlatPatientWABWHOCD4Extract());
        byte[] ret = null;
        try {
            ret = dwPatientExtractService.zipBytes(contents);
        } catch (Exception ex) {

        }
        return ret;
    }

    private void labExtractColumnHeaders(List<Object> columnHeaders) {

        columnHeaders.add("PatientID");
        columnHeaders.add("Gender");
        columnHeaders.add("DOB");
        columnHeaders.add("unique_patient_no");
        columnHeaders.add("national_id_no");
        columnHeaders.add("patient_clinic_number");
        columnHeaders.add("visit_date");
        columnHeaders.add("siteCode");
        columnHeaders.add("siteName");
        columnHeaders.add("TestName ");
        columnHeaders.add("test_result");
        columnHeaders.add("TestResult ");

    }

}