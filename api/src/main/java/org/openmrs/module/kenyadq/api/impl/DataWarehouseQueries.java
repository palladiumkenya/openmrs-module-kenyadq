package org.openmrs.module.kenyadq.api.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dev on 1/22/17.
 */
public class DataWarehouseQueries {

    public String getPatientVisitQuery() {
        String sqlQuery="select d.unique_patient_no as PatientID,\n" +
                "d.patient_id as PatientPK,\n" +
                "(select name from location\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation')) as FacilityName,\n" +
                "\n" +
                "(select value_reference from location_attribute\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation') and attribute_type_id=1) as siteCode,\n" +
                "fup.visit_id as VisitID,\n" +
                "fup.visit_date as VisitDate,\n" +
                "'Out Patient' as Service,\n" +
                "fup.visit_scheduled as VisitType,\n" +
                "case fup.who_stage \n" +
                "\twhen 1220 then 'WHO_STAGE_1'\n" +
                "\twhen 1221 then 'WHO_STAGE_2'\n" +
                "\twhen 1222 then 'WHO_STAGE_3'\n" +
                "\twhen 1223 then 'WHO_STAGE_4'\n" +
                "\twhen 1204 then 'WHO_STAGE_1'\n" +
                "\twhen 1205 then 'WHO_STAGE_2'\n" +
                "\twhen 1206 then 'WHO_STAGE_3'\n" +
                "\twhen 1207 then 'WHO_STAGE_4'\n" +
                "    else ''\n" +
                "end as WHOStage,\n" +
                "'' as WABStage,\n" +
                "case fup.pregnancy_status \n" +
                "\twhen 1065 then 'Yes'\n" +
                "\twhen 1066 then 'No'\n" +
                "end as Pregnant,\n" +
                "fup.last_menstrual_period as LMP,\n" +
                "fup.expected_delivery_date as EDD,\n" +
                "fup.height as Height,\n" +
                "fup.weight as Weight,\n" +
                "concat(fup.systolic_pressure,'/',fup.diastolic_pressure) as BP,\n" +
                "'ART|CTX' as AdherenceCategory, \n" +
                "concat( \n" +
                    "IF(fup.arv_adherence=159405, 'Good', IF(fup.arv_adherence=159406, 'Fair', IF(fup.arv_adherence=159407, 'Poor', ''))), '|', \n" +
                    "IF(fup.ctx_adherence=159405, 'Good', IF(fup.ctx_adherence=159406, 'Fair', IF(fup.ctx_adherence=159407, 'Poor', ''))) \n" +
                ") AS Adherence, \n" +
                "'' as OI, \n" +
                "'' as OIDate, \n" +
                "case fup.family_planning_status \n" +
                "when 695 then 'Currently using FP'\n" +
                "when 160652 then 'Not using FP'\n" +
                "when 1360 then 'Wants FP'\n" +
                "else ''\n" +
                "end as FamilyPlanningMethod,\n" +
                "concat(\n" +
                "case fup.condom_provided \n" +
                "when 1065 then 'Condoms,'\n" +
                "else ''\n" +
                "end,\n" +
                "case fup.pwp_disclosure\n" +
                "when 1065 then 'Disclosure,'\n" +
                "else ''\n" +
                "end,\n" +
                "case fup.pwp_partner_tested\n" +
                "when 1065 then 'Partner Testing,'\n" +
                "else ''\n" +
                "end,\n" +
                "case fup.screened_for_sti\n" +
                "when 1065 then 'Screened for STI'\n" +
                "else ''\n" +
                "end )as PWP,\n" +
                "if(fup.last_menstrual_period is not null, timestampdiff(week,fup.last_menstrual_period,fup.visit_date),'') as GestationAge,\n" +
                "fup.next_appointment_date\n" +
                "from kenyaemr_etl.etl_patient_demographics d\n" +
                "join kenyaemr_etl.etl_patient_hiv_followup fup on fup.patient_id=d.patient_id\n" +
                "where d.unique_patient_no is not null\n" +
                "order by d.patient_id,fup.visit_date;";

        return sqlQuery;
    }

    public String getPatientExtractQuery () {
        String sqlQuery="\n" +
                "select d.unique_patient_no as PatientID,\n" +
                "d.patient_id as PatientPK,\n" +
                "(select value_reference from location_attribute\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation') and attribute_type_id=1) as siteCode,\n" +
                "(select name from location\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation')) as FacilityName,\n" +
                "d.gender as Gender,\n" +
                "d.dob as DOB,\n" +
                "min(hiv.visit_date) as RegistrationDate,\n" +
                "min(hiv.visit_date) as RegistrationAtCCC,\n" +
                "min(mch.visit_date) as RegistrationATPMTCT,\n" +
                "min(tb.visit_date) as RegistrationAtTBClinic,\n" +
                "case  max(hiv.entry_point) \n" +
                "when 160542 then 'OPD' \n" +
                "when 160563 then 'Other'\n" +
                "when 160539 then 'VCT'\n" +
                "when 160538 then 'PMTCT'\n" +
                "when 160541 then 'TB'\n" +
                "when 160536 then 'IPD - Adult'\n" +
                "else cn.name\n" +
                "end as PatientSource," +
                "(select state_province from location\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation')) as Region,\n" +
                "(select county_district from location\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation'))as District,\n" +
                "(select address6 from location\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation')) as Village,\n" +
                "d.next_of_kin as ContactRelation,\n" +
                "max(hiv.visit_date) as LastVisit,\n" +
                "d.marital_status as MaritalStatus,\n" +
                "d.education_level as EducationLevel,\n" +
                "min(hiv.date_confirmed_hiv_positive) as DateConfirmedHIVPositive,\n" +
                "max(hiv.arv_status) as PreviousARTExposure,\n" +
                "'' as PreviousARTStartDate,\n" +
                "'KenyaEMR' as Emr,\n" +
                "'I-TECH' as Project,'' as eWAB,'' as eWABDate,'' as bWAB,'' as bWABDate \n" +
                "from kenyaemr_etl.etl_patient_demographics d\n" +
                "left outer join kenyaemr_etl.etl_hiv_enrollment hiv on hiv.patient_id=d.patient_id\n" +
                "left outer join kenyaemr_etl.etl_mch_enrollment mch on mch.patient_id=d.patient_id\n" +
                "left outer join kenyaemr_etl.etl_tb_enrollment tb on tb.patient_id=d.patient_id\n" +
                "left outer join concept_name cn on cn.concept_id=hiv.entry_point  and cn.concept_name_type='FULLY_SPECIFIED'\n" +
                "and cn.locale='en'\n" +
                "where unique_patient_no is not null\n"+
                "group by d.patient_id\n" +
                "order by d.patient_id;";



        return  sqlQuery;
    }

    public  String PatientStatusExtractQuery()
    {
        String sqlQuery="select d.unique_patient_no as PatientID,\n" +
                "d.patient_id as PatientPK,\n" +
                "(select value_reference from location_attribute\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation') and attribute_type_id=1) as siteCode,\n" +
                "(select name from location\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation')) as FacilityName,\n" +
                "'' as ExitDescription,\n" +
                "disc.visit_date as ExitDate,\n" +
                "case\n" +
                "when disc.discontinuation_reason is not null then cn.name\n" +
                "else '' end as ExitReason\n" +
                "from kenyaemr_etl.etl_patient_program_discontinuation disc\n" +
                "join kenyaemr_etl.etl_patient_demographics d on d.patient_id=disc.patient_id\n" +
                "left outer join concept_name cn on cn.concept_id=disc.discontinuation_reason  and cn.concept_name_type='FULLY_SPECIFIED'\n" +
                "and cn.locale='en'\n" +
                "where d.unique_patient_no is not null;\n";

        return  sqlQuery;
    }

    public  String ARTPatientExtracQuery(){
        String sqlQuery="select d.unique_patient_no as PatientID,\n" +
                "d.patient_id as PatientPK,\n" +
                "timestampdiff(year,d.DOB, hiv.visit_date) as AgeEnrollment,\n" +
                "timestampdiff(year,d.DOB, reg.art_start_date) as AgeARTStart,\n" +
                "timestampdiff(year,d.DOB, reg.latest_vis_date) as AgeLastVisit,\n" +
                "(select value_reference from location_attribute\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation') and attribute_type_id=1) as siteCode,\n" +
                "(select name from location\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation')) as FacilityName,\n" +
                "min(hiv.visit_date) as RegistrationDate,\n" +
                "case  max(hiv.entry_point) \n" +
                "when 160542 then 'OPD' \n" +
                "when 160563 then 'Other'\n" +
                "when 160539 then 'VCT'\n" +
                "when 160538 then 'PMTCT'\n" +
                "when 160541 then 'TB'\n" +
                "when 160536 then 'IPD - Adult'\n" +
                "else cn.name\n" +
                "end as PatientSource,\n" +
                "reg.art_start_date as StartARTDate,\n" +
                "'' as PreviousARTStartDate,\n" +
                "'' as PreviousARTRegimen,\n" +
                "'' as StartARTAtThisFacility,\n" +
                "reg.regimen as StartRegimen,\n" +
                "reg.regimen_line as StartRegimenLine,\n" +
                "reg.last_art_date as LastARTDate,\n" +
                "reg.last_regimen as LastRegimen,\n" +
                "reg.last_regimen_line as LastRegimenLine,\n" +
                "reg.latest_tca as ExpectedReturn,\n" +
                "reg.latest_vis_date as LastVisit,\n" +
                "timestampdiff(month,reg.art_start_date, reg.latest_vis_date) as duration,\n" +
                "d.Gender,\n" +
                "disc.visit_date as ExitDate,\n" +
                "case\n" +
                "when disc.discontinuation_reason is not null then dis_rsn.name\n" +
                "else '' end as ExitReason\n" +
                "from kenyaemr_etl.etl_hiv_enrollment hiv \n" +
                "join kenyaemr_etl.etl_patient_demographics d on d.patient_id=hiv.patient_id\n" +
                "left outer join  kenyaemr_etl.etl_patient_program_discontinuation disc on disc.patient_id=hiv.patient_id\n" +
                "left outer join (select e.patient_id,\n" +
                "if(enr.date_started_art_at_transferring_facility is not null,enr.date_started_art_at_transferring_facility,\n" +
                "e.date_started)as art_start_date, e.date_started, e.gender,e.dob,d.visit_date as dis_date, if(d.visit_date is not null, 1, 0) as TOut,\n" +
                "e.regimen, e.regimen_line, e.alternative_regimen, max(fup.next_appointment_date) as latest_tca,\n" +
                "last_art_date,last_regimen,last_regimen_line,\n" +
                "if(enr.transfer_in_date is not null, 1, 0) as TIn, max(fup.visit_date) as latest_vis_date\n" +
                "from (select e.patient_id,p.dob,p.Gender,min(e.date_started) as date_started,\n" +
                "max(e.date_started) as last_art_date,\n" +
                "mid(min(concat(e.date_started,e.regimen_name)),11) as regimen,\n" +
                "mid(min(concat(e.date_started,e.regimen_line)),11) as regimen_line, \n" +
                "mid(max(concat(e.date_started,e.regimen_name)),11) as last_regimen,\n" +
                "mid(max(concat(e.date_started,e.regimen_line)),11) as last_regimen_line,\n" +
                "max(if(discontinued,1,0))as alternative_regimen\n" +
                "from kenyaemr_etl.etl_drug_event e\n" +
                "join kenyaemr_etl.etl_patient_demographics p on p.patient_id=e.patient_id\n" +
                "group by e.patient_id) e\n" +
                "left outer join kenyaemr_etl.etl_patient_program_discontinuation d on d.patient_id=e.patient_id\n" +
                "left outer join kenyaemr_etl.etl_hiv_enrollment enr on enr.patient_id=e.patient_id\n" +
                "left outer join kenyaemr_etl.etl_patient_hiv_followup fup on fup.patient_id=e.patient_id\n" +
                "group by e.patient_id)reg on reg.patient_id=hiv.patient_id\n" +
                "left outer join concept_name dis_rsn on dis_rsn.concept_id=disc.discontinuation_reason  and dis_rsn.concept_name_type='FULLY_SPECIFIED'\n" +
                "and dis_rsn.locale='en'\n" +
                "left outer join concept_name cn on cn.concept_id=hiv.entry_point  and cn.concept_name_type='FULLY_SPECIFIED'\n" +
                "and cn.locale='en'\n" +
                "where d.unique_patient_no is not null\n" +
                "group by d.patient_id;\n";

        return  sqlQuery;
    }

    public String labExtractQuery () {

        String sqlQuery ="select d.unique_patient_no as patientID, d.patient_id as patientPK, l.encounter_id as visitID, \n" +
                "l.visit_date as orderedByDate,l.visit_date as reportedByDate, " +
                "(select value_reference from location_attribute\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation') and attribute_type_id=1) as facilityID,\n" +
                "(select value_reference from location_attribute\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation') and attribute_type_id=1) as siteCode,\n" +
                "(select name from location\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation')) as facilityName,\n" +
                "cn.name as testName,\n" +

                //"l.lab_test,\n" +
                //"l.test_result,\n" +
                "case \n" +
                "when c.datatype_id=2 then cn2.name\n" +
                "else\n" +
                "\tl.test_result\n" +
                "end as lab_test_results, '' as enrollmentTest \n" +
                "from kenyaemr_etl.etl_laboratory_extract l\n" +
                "join kenyaemr_etl.etl_patient_demographics d on d.patient_id=l.patient_id\n" +
                "join concept_name cn on cn.concept_id=l.lab_test and cn.concept_name_type='FULLY_SPECIFIED'\n" +
                "and cn.locale='en'\n" +
                "join concept c on c.concept_id = l.lab_test\n" +
                "left outer join concept_name cn2 on cn2.concept_id=l.test_result and cn2.concept_name_type='FULLY_SPECIFIED'\n" +
                "and cn2.locale='en';";


        return  sqlQuery;

    }

    public String pharmacyExtractQuery () {
        String sqlQuery="select d.unique_patient_no as PatientID,\n" +
                "d.patient_id as PatientPK,\n" +
                "(select name from location\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation')) as FacilityName,\n" +
                "\n" +
                "(select value_reference from location_attribute\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation') and attribute_type_id=1) as siteCode,\n" +
                "ph.visit_id as VisitID,\n" +
                "if(cn2.name is not null, cn2.name,cn.name) as Drug,\n" +
                "ph.visit_date as DispenseDate,\n" +
                "duration,\n" +
                "fup.next_appointment_date as ExpectedReturn\n" +
                "from kenyaemr_etl.etl_pharmacy_extract ph\n" +
                "join kenyaemr_etl.etl_patient_demographics d on d.patient_id=ph.patient_id\n" +
                "left outer join concept_name cn on cn.concept_id=ph.drug  and cn.concept_name_type='FULLY_SPECIFIED'\n" +
                "and cn.locale='en'\n" +
                "left outer join concept_name cn2 on cn2.concept_id=ph.drug  and cn2.concept_name_type='SHORT'\n" +
                "and cn.locale='en'\n" +
                "left outer join kenyaemr_etl.etl_patient_hiv_followup fup on fup.encounter_id=ph.encounter_id\n" +
                "and fup.patient_id=ph.patient_id\n" +
                "where unique_patient_no is not null\n" +
                "order by ph.patient_id,ph.visit_date";

        return  sqlQuery;
    }

    public String getPatientWABWHOCD4ExtractQuery(){
        String sqlQuery="select \n" +
                "d.unique_patient_no as PatientID,\n" +
                "d.patient_id as PatientPK,\n" +
                "(select value_reference from location_attribute\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation') and attribute_type_id=1) as facilityId,\n" +
                "(select value_reference from location_attribute\n" +
                "where location_id in (select property_value\n" +
                "from global_property\n" +
                "where property='kenyaemr.defaultLocation') and attribute_type_id=1) as siteCode,\n" +
                " mid(max(if(l.visit_date<=p_dates.enrollment_date,concat(l.visit_date,test_result),null)),11) as eCd4,\n" +
                " left(max(if(l.visit_date<=p_dates.enrollment_date,concat(l.visit_date,test_result),null)),10) as eCd4Date,\n" +
                " if(fup.visit_date<=p_dates.enrollment_date,\n" +
                " case who_stage\n" +
                "\twhen 1220 then 'WHO_STAGE_1'\n" +
                "\twhen 1221 then 'WHO_STAGE_2'\n" +
                "    when 1222 then 'WHO_STAGE_3'\n" +
                "    when 1223 then 'WHO_STAGE_4'\n" +
                "    when 1204 then 'WHO_STAGE_1'\n" +
                "    when 1205 then 'WHO_STAGE_2'\n" +
                "    when 1206 then 'WHO_STAGE_3'\n" +
                "    when 1207 then 'WHO_STAGE_4'\n" +
                " end,null) as ewhostage,\n" +
                " if(fup.visit_date<=p_dates.enrollment_date and who_stage is not null,\n" +
                "fup.visit_date,null) as ewhodate,\n" +
                "'' as bCD4,\n" +
                "'' as bCD4Date,\n" +
                "'' as bWHO,\n" +
                "'' as bWHODate,\n" +
                " mid(max(concat(fup.visit_date,case who_stage\n" +
                "\twhen 1220 then 'WHO_STAGE_1'\n" +
                "\twhen 1221 then 'WHO_STAGE_2'\n" +
                "    when 1222 then 'WHO_STAGE_3'\n" +
                "    when 1223 then 'WHO_STAGE_4'\n" +
                "    when 1204 then 'WHO_STAGE_1'\n" +
                "    when 1205 then 'WHO_STAGE_2'\n" +
                "    when 1206 then 'WHO_STAGE_3'\n" +
                "    when 1207 then 'WHO_STAGE_4'\n" +
                " end)),11) as lastwho,\n" +
                " left(max(concat(fup.visit_date,case who_stage\n" +
                "\twhen 1220 then 'WHO_STAGE_1'\n" +
                "\twhen 1221 then 'WHO_STAGE_2'\n" +
                "    when 1222 then 'WHO_STAGE_3'\n" +
                "    when 1223 then 'WHO_STAGE_4'\n" +
                "    when 1204 then 'WHO_STAGE_1'\n" +
                "    when 1205 then 'WHO_STAGE_2'\n" +
                "    when 1206 then 'WHO_STAGE_3'\n" +
                "    when 1207 then 'WHO_STAGE_4'\n" +
                " end)),10) as lastwhodate,\n" +
                "  mid(max(concat(l.visit_date,l.test_result)),11) as lastcd4,\n" +
                " left(max(concat(l.visit_date,l.test_result)),10) as lastcd4date,\n" +
                " mid(max(if(l.visit_date>p_dates.six_month_date and l.visit_date<p_dates.twelve_month_date ,concat(l.visit_date,test_result),null)),11) as m6Cd4,\n" +
                " left(max(if(l.visit_date>=p_dates.six_month_date and l.visit_date<p_dates.twelve_month_date ,concat(l.visit_date,test_result),null)),10) as m6Cd4Date,\n" +
                " mid(max(if(l.visit_date>=p_dates.twelve_month_date,concat(l.visit_date,test_result),null)),11) as m6Cd4,\n" +
                " left(max(if(l.visit_date>p_dates.twelve_month_date,concat(l.visit_date,test_result),null)),10) as m6Cd4Date,'' as eWAB,'' as eWABDate,'' as bWAB,'' as bWABDAte\n" +
                "from kenyaemr_etl.etl_patient_hiv_followup fup\n" +
                "join kenyaemr_etl.etl_patient_demographics d on d.patient_id=fup.patient_id\n" +
                "join (select e.patient_id,date_add(date_add(min(e.visit_date),interval 3 month), interval 1 day) as enrollment_date,\n" +
                "date_add(date_add(min(e.visit_date), interval 6 month),interval 1 day) as six_month_date,\n" +
                "date_add(date_add(min(e.visit_date), interval 12 month),interval 1 day) as twelve_month_date\n" +
                "from kenyaemr_etl.etl_hiv_enrollment e\n" +
                "group by e.patient_id) p_dates on p_dates.patient_id=fup.patient_id\n" +
                "left outer join kenyaemr_etl.etl_laboratory_extract l on l.patient_id=fup.patient_id and l.lab_test in (5497,730)\n" +
                "group by fup.patient_id";

        return  sqlQuery;
    }

    public List<Object> getPatientVisitHeaderRow() {
        List<Object> headerRow = new ArrayList<Object>();
        headerRow.add("PatientID");
        headerRow.add("PatientPK");
        headerRow.add("FacilityName");
        headerRow.add("SiteCode");
        headerRow.add("VisitID");
        headerRow.add("VisitDate");
        headerRow.add("Service");
        headerRow.add("VisitType");
        headerRow.add("WHOStage");
        headerRow.add("WABStage");
        headerRow.add("Pregnant");
        headerRow.add("LMP");
        headerRow.add("EDD");
        headerRow.add("Height");
        headerRow.add("Weight");
        headerRow.add("BP");
        headerRow.add("AdherenceCategory");
        headerRow.add("Adherence");
        headerRow.add("OI");
        headerRow.add("OIDate");
        headerRow.add("FamilyPlanningMethod");
        headerRow.add("PwP");
        headerRow.add("GestationAge");
        headerRow.add("NextAppointmentDate");
        return headerRow;


    }

    public List<Object> getPatientHeaderRow() {
        List<Object> headerRow = new ArrayList<Object>();
            headerRow.add("PatientID");
            headerRow.add("PatientPK");
            headerRow.add("SiteCode");
            headerRow.add("FacilityName");
            headerRow.add("Gender");
            headerRow.add("DOB");
            headerRow.add("RegistrationDate");
            headerRow.add("RegistrationAtCCC");
            headerRow.add("RegistrationATPMTCT");
            headerRow.add("RegistrationAtTBClinic");
            headerRow.add("PatientSource");
            headerRow.add("Region");
            headerRow.add("District");
            headerRow.add("Village");
            headerRow.add("ContactRelation");
            headerRow.add("LastVisit");
            headerRow.add("MaritalStatus");
            headerRow.add("EducationLevel");
            headerRow.add("DateConfirmedHIVPositive");
            headerRow.add("PreviousARTExposure");
            headerRow.add("PreviousARTStartDate");
            headerRow.add("Emr");
            headerRow.add("Project");
        return headerRow;
    }

    public List<Object> getPatientStatusHeaderRow() {
        List<Object> headerRow = new ArrayList<Object>();
        headerRow.add("PatientID");
        headerRow.add("PatientPK");
        headerRow.add("SiteCode");
        headerRow.add("FacilityName");
        headerRow.add("ExitDescription");
        headerRow.add("ExitDate");
        headerRow.add("ExitReason");
        return headerRow;
    }

    public List<Object> getARTPatientExtractHeaderRow() {
        List<Object> headerRow = new ArrayList<Object>();
        headerRow.add("PatientID");
        headerRow.add("PatientPK");
        headerRow.add("AgeEnrollment");
        headerRow.add("AgeARTStart");
        headerRow.add("AgeLastVisit");
        headerRow.add("SiteCode");
        headerRow.add("FacilityName");
        headerRow.add("RegistrationDate");
        headerRow.add("PatientSource");
        headerRow.add("Gender");
        headerRow.add("StartARTDate");
        headerRow.add("PreviousARTStartDate");
        headerRow.add("PreviousARTRegimen");
        headerRow.add("StartARTAtThisFacility");
        headerRow.add("StartRegimen");
        headerRow.add("StartRegimenLine");
        headerRow.add("LastARTDate");
        headerRow.add("LastRegimen");
        headerRow.add("LastRegimenLine");
        headerRow.add("ExpectedReturn");
        headerRow.add("Duration");
        headerRow.add("LastVisit");
        headerRow.add("ExitReason");
        headerRow.add("ExitDate");
        return headerRow;
    }

    public List<Object> getLabExtractHeaderRow() {
        List<Object> headerRow = new ArrayList<Object>();
        headerRow.add("PatientID");
        headerRow.add("PatientPK");
        headerRow.add("VisitID");
        headerRow.add("OrderedByDate");
        headerRow.add("ReportedByDate");
        headerRow.add("FacilityID");
        headerRow.add("SiteCode");
        headerRow.add("FacilityName");
        headerRow.add("TestName");
        headerRow.add("TestResult");
        headerRow.add("EnrollmentTest");


        return  headerRow;

    }

    public List<Object> getPatientPharmacyExtractHeaderRow() {
        List<Object> headerRow = new ArrayList<Object>();
        headerRow.add("PatientID");
        headerRow.add("PatientPK");
        headerRow.add("FacilityName");
        headerRow.add("SiteCode");
        headerRow.add("VisitID");
        headerRow.add("Drug");
        headerRow.add("DispenseDate");
        headerRow.add("Duration");
        headerRow.add("ExpectedReturn");
        headerRow.add("TreatmentType");
        headerRow.add("PeriodTaken");
        headerRow.add("ProphylaxisType");
        headerRow.add("RegimenLine");
        return headerRow;
    }

    public List<Object> getPatientWABWHOCD4ExtractHeaderRow() {
        List<Object> headerRow = new ArrayList<Object>();
        headerRow.add("PatientID");
        headerRow.add("PatientPK");
        headerRow.add("FacilityID");
        headerRow.add("SiteCode");
        headerRow.add("eCD4");
        headerRow.add("eCD4Date");
        headerRow.add("eWHO");
        headerRow.add("eWHODate");
        headerRow.add("bCD4");
        headerRow.add("bCD4Date");
        headerRow.add("bWHO");
        headerRow.add("bWHODate");
        headerRow.add("lastWHO");
        headerRow.add("lastWHODate");
        headerRow.add("lastCD4");
        headerRow.add("lastCD4Date");
        headerRow.add("m12CD4");
        headerRow.add("m12CD4Date");
        headerRow.add("m6CD4");
        headerRow.add("m6CD4Date");
        headerRow.add("eWAB");
        headerRow.add("eWABDate");
        headerRow.add("bWAB");
        headerRow.add("bWABDate");
        return headerRow;
    }
}







