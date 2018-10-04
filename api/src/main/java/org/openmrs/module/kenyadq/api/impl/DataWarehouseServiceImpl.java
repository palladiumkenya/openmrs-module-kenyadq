package org.openmrs.module.kenyadq.api.impl;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Weeks;
import org.joda.time.Years;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.LocationAttributeType;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.OrderType;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.Visit;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.FormService;
import org.openmrs.api.ObsService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyadq.api.DataWarehouseService;
import org.openmrs.module.kenyadq.api.db.KenyaDqDao;
import org.openmrs.module.kenyaemr.Metadata;
import org.openmrs.module.kenyaemr.metadata.FacilityMetadata;
import org.openmrs.module.kenyaemr.util.EmrUtils;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.reporting.dataset.DataSet;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.SqlDataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.service.DataSetDefinitionService;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by gitahi on 28/07/15.
 */
@Service
public class DataWarehouseServiceImpl implements DataWarehouseService {

    @Autowired
    private CsvCreator csvCreator;

    @Autowired
    @Qualifier("patientService")
    private PatientService patientService;

    @Autowired
    @Qualifier("encounterService")
    private EncounterService encounterService;

    @Autowired
    @Qualifier("visitService")
    private VisitService visitService;

    @Autowired
    @Qualifier("obsService")
    private ObsService obsService;

    @Autowired
    @Qualifier("formService")
    private FormService formService;

    @Autowired
    @Qualifier("conceptService")
    private ConceptService conceptService;

    @Autowired
    @Qualifier("orderService")
    private OrderService orderService;


    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final DateFormat OBS_DATE_FORMAT = new SimpleDateFormat("MM/dd/yy");

    private String mfl = "";

    private final boolean SAMPLE = false;
    private final int SAMPLE_SIZE = 10;

    CareSetting outpatient = Context.getOrderService().getCareSettingByName("OUTPATIENT");
    OrderType drugOrderType = Context.getOrderService().getOrderTypeByUuid(OrderType.DRUG_ORDER_TYPE_UUID);


    private List<Patient> patients;
    Map<Integer, EncounterInfo> firstEncounterMap = new HashMap<Integer, EncounterInfo>();

    private List<Patient> getPatients() {
        if (patients == null) {
            patients = patientService.getAllPatients();
        }
        return patients;
    }

    private EncounterInfo getFirstEncounter(Patient patient) {
        EncounterInfo firstEncounterInfo = firstEncounterMap.get(patient.getId());
        if (firstEncounterInfo == null) {
            firstEncounterInfo = getFirstEncounterInfo(patient);
            if ("".equals(mfl)) {
                mfl = firstEncounterInfo.locationInfo.mfl;
            }
            firstEncounterMap.put(patient.getId(), firstEncounterInfo);
        }
        return firstEncounterInfo;
    }

    @Override
    public byte[] downloadPatientExtract() {
        List<Object> data = new ArrayList<Object>();
        List<Patient> patients = getPatients();
        int i = 0;
        for (Patient patient : patients) {
            if (SAMPLE) {
                if (i > SAMPLE_SIZE) {
                    break;
                }
                if (patient.getId() % 13 != 0) {
                    continue;
                }
                i++;
            }
            List<Object> row = new ArrayList<Object>();
            try {
                PersonInfo personInfo = getPersonInfo(patient);
                EncounterInfo firstEncounterInfo = getFirstEncounter(patient);
                EncounterInfo hivEnrollmentEncounterInfo = getHivEnrollmentEncounterInfo(patient);
                EncounterInfo mchmsEnrollmentEncounterInfo = getMchmsEnrollmentEncounterInfo(patient);
                EncounterInfo tbEnrollmentEncounterInfo = getTbEnrollmentEncounterInfo(patient);
                VisitInfo lastVisitInfo = getLastVisitInfo(patient);

                Map<String, ObsInfo> obsInfoMap = getObsInfoMap
                        (
                                patient,
                                true,
                                Metadata.Concept.METHOD_OF_ENROLLMENT,
                                Metadata.Concept.CIVIL_STATUS,
                                Metadata.Concept.EDUCATION,
                                Metadata.Concept.DATE_OF_HIV_DIAGNOSIS,
                                "1088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        );

                ObsInfo patientSourceObsInfo = readObsInfoFromMap(obsInfoMap, Metadata.Concept.METHOD_OF_ENROLLMENT);
                ObsInfo maritalStatusObsInfo = readObsInfoFromMap(obsInfoMap, Metadata.Concept.CIVIL_STATUS);
                ObsInfo educationLevelObsInfo = readObsInfoFromMap(obsInfoMap, Metadata.Concept.EDUCATION);
                ObsInfo hivConfirmationDateObsInfo = readObsInfoFromMap(obsInfoMap, Metadata.Concept.DATE_OF_HIV_DIAGNOSIS);
                ObsInfo previousArtObsInfo = readObsInfoFromMap(obsInfoMap, "1088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                row.add(personInfo.upn);
                row.add(personInfo.pk);
                row.add(firstEncounterInfo.locationInfo.mfl);
                row.add(firstEncounterInfo.locationInfo.facilityName);
                row.add(personInfo.gender);
                row.add(personInfo.birthDate);
                row.add(personInfo.dateCreated);
                row.add(hivEnrollmentEncounterInfo.encounterDate);
                row.add(mchmsEnrollmentEncounterInfo.encounterDate);
                row.add(tbEnrollmentEncounterInfo.encounterDate);
                row.add(patientSourceObsInfo.value);
                row.add(firstEncounterInfo.locationInfo.region);
                row.add(firstEncounterInfo.locationInfo.district);
                row.add(firstEncounterInfo.locationInfo.village);
                row.add(personInfo.nextOfKinName);
                row.add(lastVisitInfo.visitDate);
                row.add(maritalStatusObsInfo.value);
                row.add(educationLevelObsInfo.value);
                row.add(fromObsDateToStandardDate(hivConfirmationDateObsInfo.value));
                row.add(previousArtObsInfo.value);
                row.add(previousArtObsInfo.date);
                row.add(AuxilliaryInfo.EMR);
                row.add(AuxilliaryInfo.PROJECT);

                data.add(row.toArray());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return csvCreator.createCsv(data, getPatientHeaderRow());
    }

    @Override
    public byte[] downloadPatientStatusExtract() {
        List<Object> data = new ArrayList<Object>();
        List<Patient> patients = getPatients();
        int i = 0;
        for (Patient patient : patients) {
            if (SAMPLE) {
                if (i > SAMPLE_SIZE) {
                    break;
                }
                if (patient.getId() % 13 != 0) {
                    continue;
                }
                i++;
            }
            try {
                List<Object> row = new ArrayList<Object>();
                PersonInfo personInfo = getPersonInfo(patient);
                EncounterInfo firstEncounterInfo = getFirstEncounter(patient);
                EncounterInfo hivDiscontinuationEncounterInfo = getHivDiscontinuationEncounterInfo(patient);

                Map<String, ObsInfo> obsInfoMap = getObsInfoMap
                        (
                                patient,
                                true,
                                Metadata.Concept.REASON_FOR_PROGRAM_DISCONTINUATION
                        );

                ObsInfo exitReasonObsInfo = readObsInfoFromMap(obsInfoMap,
                        Metadata.Concept.REASON_FOR_PROGRAM_DISCONTINUATION);

                row.add(personInfo.upn);
                row.add(personInfo.pk);
                row.add(firstEncounterInfo.locationInfo.mfl);
                row.add(firstEncounterInfo.locationInfo.facilityName);
                row.add("");
                row.add(hivDiscontinuationEncounterInfo.encounterDate);
                row.add(exitReasonObsInfo.value);
                data.add(row.toArray());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return csvCreator.createCsv(data, getPatientStatusHeaderRow());
    }

    @Override
    public byte[] downloadPatientVisitExtract() {
        List<Object> data = new ArrayList<Object>();
        List<Patient> patients = getPatients();
        int i = 0;

        Map<Integer, List<VisitInfo>> visitInfoMap = getVisitInfoMap(patients);
        for (Patient patient : patients) {
            if (SAMPLE) {
                if (i > SAMPLE_SIZE) {
                    break;
                }
                if (patient.getId() % 13 != 0) {
                    continue;
                }
                i++;
            }
            List<Object> common = new ArrayList<Object>();
            try {
                PersonInfo personInfo = getPersonInfo(patient);
                EncounterInfo firstEncounterInfo = getFirstEncounter(patient);

                common.add(personInfo.upn);
                common.add(personInfo.pk);
                common.add(firstEncounterInfo.locationInfo.facilityName);
                common.add(firstEncounterInfo.locationInfo.mfl);

                List<VisitInfo> visitInfos = visitInfoMap.get(patient.getId());
                if (visitInfos == null) {
                    continue;
                }
                for (VisitInfo visitInfo : visitInfos) {
                    List<Object> row = new ArrayList<Object>();
                    row.addAll(common);
                    row.add(visitInfo.pk);
                    row.add(visitInfo.visitDate);

                    //This is what DW calls service but in KenyaEMR it is visit type
                    row.add(visitInfo.type);


                    Map<String, ObsInfo> obsInfoMap = getObsInfoMap
                            (
                                    patient,
                                    visitInfo.visit,
                                    false,
                                    "1246AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    Metadata.Concept.CURRENT_WHO_STAGE,
                                    Metadata.Concept.PREGNANCY_STATUS,
                                    Metadata.Concept.LAST_MONTHLY_PERIOD,
                                    Metadata.Concept.EXPECTED_DATE_OF_DELIVERY,
                                    Metadata.Concept.HEIGHT_CM,
                                    Metadata.Concept.WEIGHT_KG,
                                    "5085AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "5086AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "6042AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "161652AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "159777AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "159423AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "161557AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "161558AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                                    "5096AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            );


                    {
                        //This is what DW calls visit type but in KenyaEMR it is Scheduled? (vs unscheduled, of course)
                        ObsInfo obsInfo = readObsInfoFromMap(obsInfoMap, "1246AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        row.add("true".equals(obsInfo.value) ? "Scheduled" : "Unscheduled");
                    }

                    String lmp;

                    {
                        ObsInfo obsInfo = readObsInfoFromMap(obsInfoMap, Metadata.Concept.CURRENT_WHO_STAGE);
                        row.add(obsInfo.value);
                    }
                    {
                        row.add("");//WABStage not available in KenyaEMR
                    }
                    {
                        ObsInfo obsInfo = readObsInfoFromMap(obsInfoMap, Metadata.Concept.PREGNANCY_STATUS);
                        row.add(obsInfo.value);
                    }
                    {
                        ObsInfo obsInfo = readObsInfoFromMap(obsInfoMap, Metadata.Concept.LAST_MONTHLY_PERIOD);
                        lmp = fromObsDateToStandardDate(obsInfo.value);
                        row.add(lmp);
                    }
                    {
                        ObsInfo obsInfo = readObsInfoFromMap(obsInfoMap, Metadata.Concept.EXPECTED_DATE_OF_DELIVERY);
                        row.add(fromObsDateToStandardDate(obsInfo.value));
                    }
                    {
                        ObsInfo obsInfo = readObsInfoFromMap(obsInfoMap, Metadata.Concept.HEIGHT_CM);
                        row.add(obsInfo.value);
                    }
                    {
                        ObsInfo obsInfo = readObsInfoFromMap(obsInfoMap, Metadata.Concept.WEIGHT_KG);
                        row.add(obsInfo.value);
                    }
                    {
                        ObsInfo xObsInfo = readObsInfoFromMap(obsInfoMap, "5085AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        ObsInfo yObsInfo = readObsInfoFromMap(obsInfoMap, "5086AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        if (xObsInfo.value != null && yObsInfo.value != null) {
                            row.add(xObsInfo.value + "/" + yObsInfo.value);
                        } else {
                            row.add("");
                        }
                    }
                    {
                        ObsInfo obsInfo = readObsInfoFromMap(obsInfoMap, "6042AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        row.add(obsInfo.value);
                    }
                    {
                        row.add("");//OIDate not available in KenyaEMR
                    }
                    {
                        ObsInfo obsInfo = readObsInfoFromMap(obsInfoMap, "161652AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        row.add(obsInfo.value);
                    }
                    {
                        //adherence category not in kemr
//                    ObsInfo obsInfo = getObsInfo(patient, visitInfo.visit, Metadata.Concept.WEIGHT_KG, false);
                        row.add("");
                    }
                    {
                        ObsInfo obsInfo = readObsInfoFromMap(obsInfoMap, Metadata.Concept.FAMILY_PLANNING);
                        row.add(obsInfo.value);
                    }
                    {
                        ObsInfo aObsInfo = readObsInfoFromMap(obsInfoMap, "159777AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        ObsInfo bObsInfo = readObsInfoFromMap(obsInfoMap, "159423AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        ObsInfo cObsInfo = readObsInfoFromMap(obsInfoMap, "161557AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        ObsInfo dObsInfo = readObsInfoFromMap(obsInfoMap, "161558AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        row.add
                                (
                                        (("Yes".equals(aObsInfo.value) ? "Condoms," : "")
                                                + ("Yes".equals(bObsInfo.value) ? "Disclosure," : "")
                                                + ("Yes".equals(cObsInfo.value) ? "Partner Testing," : "")
                                                + ("Yes".equals(dObsInfo.value) ? "Screened STI" : "")).replaceAll(",$", "")
                                );
                    }
                    {
                        if (lmp != null) {
                            try {
                                Date lmpDate = DATE_FORMAT.parse(lmp);
                                Date visitDate = visitInfo.visit.getStartDatetime();
                                int weeks = Weeks.weeksBetween(new DateTime(lmpDate), new DateTime(visitDate)).getWeeks();
                                row.add(weeks);
                            } catch (Exception ex) {
                                row.add("");
                            }
                        } else {
                            row.add("");
                        }
                    }
                    {
                        ObsInfo obsInfo = readObsInfoFromMap(obsInfoMap, "5096AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        row.add(fromObsDateToStandardDate(obsInfo.value));
                    }
                    data.add(row.toArray());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return csvCreator.createCsv(data, getPatientVisitHeaderRow());
    }

    private ObsInfo readObsInfoFromMap(Map<String, ObsInfo> map, String conceptUuid) {
        ObsInfo obsInfo = map.get(conceptUuid);
        if (obsInfo == null) {
            obsInfo = new ObsInfo();
        }
        return obsInfo;
    }

    public byte[] downloadPatientLaboratoryExtract() {
        List<Object> data = new ArrayList<Object>();
        List<Patient> patients = getPatients();
        int i = 0;
        for (Patient patient : patients) {
            if (SAMPLE) {
                if (i > SAMPLE_SIZE) {
                    break;
                }
                if (patient.getId() % 13 != 0) {
                    continue;
                }
                i++;
            }
            List<Object> common = new ArrayList<Object>();
            try {
                PersonInfo personInfo = getPersonInfo(patient);
                EncounterInfo firstEncounterInfo = getFirstEncounter(patient);
                common.add(personInfo.upn);
                common.add(personInfo.pk);
                common.add(firstEncounterInfo.locationInfo.mfl);
                common.add(firstEncounterInfo.locationInfo.facilityName);

                List<VisitInfo> visitInfos = getLabVisitInfos(patient, "17a381d1-7e29-406a-b782-aa903b963c28");
                for (VisitInfo visitInfo : visitInfos) {
                    List<Object> encounterData = new ArrayList<Object>();
                    encounterData.addAll(common);
                    encounterData.add(visitInfo.pk);
                    Set<Obs> obses = visitInfo.encounter.getAllObs();
                    for (Obs obs : obses) {
                        List<Object> row = new ArrayList<Object>();
                        row.addAll(encounterData);
                        row.add("");//date ordered not available in KenyaEMR
                        row.add(DATE_FORMAT.format(obs.getObsDatetime()));
                        row.add(obs.getConcept().getName(Locale.ENGLISH));
                        row.add(obs.getValueAsString(Locale.ENGLISH));
                        data.add(row.toArray());
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return csvCreator.createCsv(data, getPatientLaboratoryHeaderRow());
    }

    public byte[] downloadPatientPharmacyExtract() {
        List<Object> data = new ArrayList<Object>();
        List<Patient> patients = getPatients();
        int i = 0;
        for (Patient patient : patients) {
            if (SAMPLE) {
                if (i > SAMPLE_SIZE) {
                    break;
                }
                if (patient.getId() % 13 != 0) {
                    continue;
                }
                i++;
            }
            List<Object> common = new ArrayList<Object>();
            try {
                PersonInfo personInfo = getPersonInfo(patient);
                EncounterInfo firstEncounterInfo = getFirstEncounter(patient);
                common.add(personInfo.upn);
                common.add(firstEncounterInfo.locationInfo.mfl);
                common.add(firstEncounterInfo.locationInfo.facilityName);
                common.add(personInfo.pk);

                List<EncounterInfo> encounterInfos = getOtherMedicationEncounters(patient);
                for (EncounterInfo encounterInfo : encounterInfos) {
                    Encounter encounter = encounterInfo.encounter;

                    List<Object> visitSegment = new ArrayList<Object>();
                    visitSegment.addAll(common);
                    if (encounter.getVisit() != null) {
                        visitSegment.add(encounter.getVisit().getId());
                    }
                    Set<Obs> obses = encounter.getObsAtTopLevel(false);
                    for (Obs obs : obses) {
                        List<Object> row = new ArrayList<Object>();
                        row.addAll(visitSegment);
                        Set<Obs> children = obs.getRelatedObservations();
                        double duration = 0;
                        double multiplier = 0;
                        for (Obs child : children) {
                            if (child.getConcept().getUuid().equals("1282AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                                row.add(child.getValueAsString(Locale.ENGLISH));
                                row.add(DATE_FORMAT.format(encounter.getEncounterDatetime()));
                            } else if (child.getConcept().getUuid().equals("159368AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                                duration = child.getValueNumeric();
                            } else if (child.getConcept().getUuid().equals("1732AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {
                                multiplier = 0;
                                Concept answer = child.getValueCoded();
                                if (answer.getUuid().equals("1822AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {//hours
                                    multiplier = (1 / 24);
                                } else if (answer.getUuid().equals("1072AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {//days
                                    multiplier = 1;
                                } else if (answer.getUuid().equals("1073AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {//weeks
                                    multiplier = 7;
                                } else if (answer.getUuid().equals("1074AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")) {//months
                                    multiplier = 30;
                                }
                            }
                        }
                        int durationInDays = (int) (duration * multiplier);
                        row.add(durationInDays);
                        Date toReturn = new DateTime(new Date()).plusDays(durationInDays).toDate();
                        row.add(DATE_FORMAT.format(toReturn));
                        row.add("");//KEMR doesn't store TreatmentType
                        row.add("");//KEMR doesn't store PeriodTaken
                        row.add("");//KEMR doesn't store ProphylaxisType
                        data.add(row.toArray());
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return csvCreator.createCsv(data, getPatientPharmacyExtractHeaderRow());
    }

    public byte[] downloadPatientWABWHOCD4Extract() {
        //get all cd4 obs for patient ordered by date
        //get all who obs for patient ordered by date
        //get enrollment date
        //get art init date

        //check if 1st cd4/who is within 3 months of enrollment

        //pick last cd4/who
        List<Object> data = new ArrayList<Object>();
        List<Patient> patients = getPatients();
        int i = 0;
        for (Patient patient : patients) {
            if (SAMPLE) {
                if (i > SAMPLE_SIZE) {
                    break;
                }
                if (patient.getId() % 13 != 0) {
                    continue;
                }
                i++;
            }
            List<Object> row = new ArrayList<Object>();
            try {
                PersonInfo personInfo = getPersonInfo(patient);
                EncounterInfo firstEncounterInfo = getFirstEncounter(patient);
                EncounterInfo hivEnrollmentEncounterInfo = getHivEnrollmentEncounterInfo(patient);

                List<ObsInfo> cd4ObsInfos = getObsInfos(patient, Metadata.Concept.CD4_COUNT);
                ObsInfo enrollmentCd4 = new ObsInfo();
                ObsInfo lastCd4 = new ObsInfo();
                ObsInfo initCd4 = new ObsInfo();
                ObsInfo m6Cd4 = new ObsInfo();
                ObsInfo m12Cd4 = new ObsInfo();
                if (!cd4ObsInfos.isEmpty()) {
                    if (datesWithinDays(cd4ObsInfos.get(0).obs.getObsDatetime(),
                            hivEnrollmentEncounterInfo.encounter.getEncounterDatetime(), 90)) {
                        enrollmentCd4 = cd4ObsInfos.get(0);
                    }
                    lastCd4 = cd4ObsInfos.get(cd4ObsInfos.size() - 1);

                    Date artInitDate = getARTInitDate(patient);
                    if (artInitDate != null) {
                        for (ObsInfo cd4ObsInfo : cd4ObsInfos) {
                            if (datesWithinDays(cd4ObsInfo.obs.getObsDatetime(), artInitDate, 90)) {
                                initCd4 = cd4ObsInfo;
                                break;
                            }
                        }

                        for (ObsInfo cd4ObsInfo : cd4ObsInfos) {
                            if (datesWithinDays(cd4ObsInfo.obs.getObsDatetime(), artInitDate, 150, 210)) {
                                m6Cd4 = cd4ObsInfo;
                                break;
                            }
                        }

                        for (ObsInfo cd4ObsInfo : cd4ObsInfos) {
                            if (datesWithinDays(cd4ObsInfo.obs.getObsDatetime(), artInitDate, 330, 390)) {
                                m12Cd4 = cd4ObsInfo;
                                break;
                            }
                        }
                    }
                }

                List<ObsInfo> whoObsInfos = getObsInfos(patient, Metadata.Concept.CURRENT_WHO_STAGE);
                ObsInfo enrollmentWho = new ObsInfo();
                ObsInfo lastWho = new ObsInfo();
                ObsInfo initWho = new ObsInfo();
                if (!whoObsInfos.isEmpty()) {
                    if (datesWithinDays(whoObsInfos.get(0).obs.getObsDatetime(),
                            hivEnrollmentEncounterInfo.encounter.getEncounterDatetime(), 90)) {
                        enrollmentWho = whoObsInfos.get(0);
                    }
                    lastWho = whoObsInfos.get(whoObsInfos.size() - 1);

                    Date artInitDate = getARTInitDate(patient);
                    if (artInitDate != null) {
                        for (ObsInfo whoObsInfo : whoObsInfos) {
                            if (datesWithinDays(whoObsInfo.obs.getObsDatetime(), artInitDate, 90)) {
                                initWho = whoObsInfo;
                                break;
                            }
                        }
                    }
                }


                row.add(personInfo.upn);
                row.add(personInfo.pk);
                row.add(firstEncounterInfo.locationInfo.location.getId());
                row.add(firstEncounterInfo.locationInfo.mfl);
                row.add(enrollmentCd4.value);
                row.add(enrollmentCd4.date);
                row.add(enrollmentWho.value);
                row.add(enrollmentWho.date);
                row.add(initCd4.value);
                row.add(initCd4.date);
                row.add(initWho.value);
                row.add(initWho.date);
                row.add(lastWho.value);
                row.add(lastWho.date);
                row.add(lastCd4.value);
                row.add(lastCd4.date);
                row.add(m12Cd4.value);
                row.add(m12Cd4.date);
                row.add(m6Cd4.value);
                row.add(m6Cd4.date);
                data.add(row.toArray());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return csvCreator.createCsv(data, getPatientWABWHOCD4ExtractHeaderRow());
    }

    private boolean datesWithinDays(Date one, Date two, int days) {
        int diffInDays = Math.abs(Days.daysBetween(new DateTime(one).toLocalDate(), new DateTime(two).toLocalDate()).getDays());
        return diffInDays <= days;
    }

    private boolean datesWithinDays(Date one, Date two, int from, int to) {
        int diffInDays = Math.abs(Days.daysBetween(new DateTime(one).toLocalDate(), new DateTime(two).toLocalDate()).getDays());
        return diffInDays >= from && diffInDays <= to;
    }

    private int diffInYears(Date one, Date two) {
        return Math.abs(Years.yearsBetween(new DateTime(one).toLocalDate(), new DateTime(two).toLocalDate()).getYears());
    }

    public byte[] downloadARTPatientExtract() {
        List<Object> data = new ArrayList<Object>();
        List<Patient> patients = getPatients();
        int i = 0;
        for (Patient patient : patients) {
            if (SAMPLE) {
                if (i > SAMPLE_SIZE) {
                    break;
                }
                if (patient.getId() % 13 != 0 || patient.getId() == 28) {
                    continue;
                }
                i++;
            }
            List<Object> row = new ArrayList<Object>();
            try {
                PersonInfo personInfo = getPersonInfo(patient);
                EncounterInfo firstEncounterInfo = getFirstEncounter(patient);
                EncounterInfo hivEnrollmentEncounterInfo = getHivEnrollmentEncounterInfo(patient);
                EncounterInfo hivDiscontinuationEncounterInfo = getHivDiscontinuationEncounterInfo(patient);

                VisitInfo lastVisitInfo = getLastVisitInfo(patient);

                ObsInfo exitReasonObsInfo = getObsInfo(patient, Metadata.Concept.REASON_FOR_PROGRAM_DISCONTINUATION, true);
                ObsInfo patientSourceObsInfo = getObsInfo(patient, Metadata.Concept.METHOD_OF_ENROLLMENT, true);

                List<ObsInfo> artDrugHist = getObsInfos(patient, "966AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                ObsInfo prevArt = new ObsInfo();
                if (artDrugHist != null && !artDrugHist.isEmpty()) {
                    prevArt = artDrugHist.get(0);
                }

                Date dob = personInfo.patient.getBirthdate();

                Integer ageAtEnrollment = diffInYears(dob, hivEnrollmentEncounterInfo.encounter.getEncounterDatetime());
                Integer ageAtLastVisit = null;
                if (lastVisitInfo != null && lastVisitInfo.visit != null) {
                    Date lastVisitDate = lastVisitInfo.visit.getStartDatetime();
                    ageAtLastVisit = diffInYears(lastVisitDate, dob);
                }

                Date artStartDate = null;
                if (prevArt.obs != null) {
                    artStartDate = prevArt.obs.getObsDatetime();
                } else {
                    artStartDate = getARTInitDate(patient);
                }

                String artStartDateString = null;
                Integer ageAtArtStart = null;
                if (artStartDate != null) {
                    artStartDateString = DATE_FORMAT.format(artStartDate);
                    ageAtArtStart = diffInYears(artStartDate, dob);
                }


                String startRegimen = null;
                String startRegimenLine = null;


                String lastArtDate = null;
                String lastRegimen = null;
                String lastRegimenLine = null;
                String lastRegimenDuration = null;

                String expectedReturn = null;

                Map<String, List<Order>> drugOrders = getDrugOrders(patient);
                if (drugOrders != null && !drugOrders.isEmpty()) {
                    List<String> keys = new ArrayList<String>(drugOrders.keySet());
                    List<Order> startDrugOrders = drugOrders.get(keys.get(0));
                    List<Order> lastDrugOrders = drugOrders.get(keys.get(keys.size() - 1));

                    startRegimen = getRegimen(startDrugOrders);
                    startRegimenLine = getRegimenLine(startRegimen);

                    lastRegimen = getRegimen(lastDrugOrders);
                    lastRegimenLine = getRegimenLine(lastRegimen);

                    lastArtDate = keys.get(keys.size() - 1);

                    System.out.println("");
                }

                row.add(personInfo.pk);
                row.add(personInfo.upn);
                row.add(ageAtEnrollment);
                row.add(ageAtArtStart);
                row.add(ageAtLastVisit);
                row.add(firstEncounterInfo.locationInfo.mfl);
                row.add(firstEncounterInfo.locationInfo.facilityName);
                row.add(hivEnrollmentEncounterInfo.encounterDate);
                row.add(patientSourceObsInfo.value);
                row.add(personInfo.gender);
                row.add(artStartDateString);
                row.add(prevArt.date);
                row.add(prevArt.value);
                row.add(artStartDateString);
                row.add(startRegimen);
                row.add(startRegimenLine);
                row.add(lastArtDate);
                row.add(lastRegimen);
                row.add(lastRegimenLine);
                row.add("");//Duration not in KenyaEMR
                row.add("");//ExpectedReturn cannot be calculated in the absence of Duration Above
                row.add(lastVisitInfo.visitDate);
                row.add(exitReasonObsInfo.value);
                row.add(hivDiscontinuationEncounterInfo.encounterDate);

                data.add(row.toArray());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return csvCreator.createCsv(data, getARTPatientExtractHeaderRow());
    }

    @Override
    public byte[] downloadAll() {
        Map<String, byte[]> contents = new HashMap<String, byte[]>();
        contents.put("ARTPatientExtract" + "-" + location() + "-" + timeStamp() + mfl + ".csv", downloadARTPatientExtract());
        contents.put("PatientExtract" + "-" + location() + "-" + timeStamp() + mfl + ".csv", downloadPatientExtract());
        contents.put("PatientLaboratoryExtract" + "-" + location() + "-" + timeStamp() + mfl + ".csv", downloadPatientLaboratoryExtract());
        contents.put("PatientPharmacyExtract" + "-" + location() + "-" + timeStamp() + mfl + ".csv", downloadPatientPharmacyExtract());
        contents.put("PatientStatusExtract" + "-" + location() + "-" + timeStamp() + mfl + ".csv", downloadPatientStatusExtract());
//        contents.put("PatientVisitExtract" + "-" + location() + "-" + timeStamp() + mfl + ".csv", downloadPatientVisitExtract());
        contents.put("PatientWABWHOCD4Extract" + "-" + location() + "-" + timeStamp() + mfl + ".csv", downloadPatientWABWHOCD4Extract());
        byte[] ret = null;
        try {
            ret = zipBytes(contents);
        } catch (Exception ex) {

        }
        return ret;
    }

    public byte[] zipBytes(Map<String, byte[]> contents) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        for (String fileName : contents.keySet()) {
            byte[] input = contents.get(fileName);
            ZipEntry entry = new ZipEntry(fileName);
            entry.setSize(input.length);
            zos.putNextEntry(entry);
            zos.write(input);
            zos.closeEntry();
        }
        zos.close();
        return baos.toByteArray();
    }

    private String getRegimen(List<Order> drugOrders) {
        String regimen = "";
        for (Order drugOrder : drugOrders) {
            ConceptName name = drugOrder.getConcept().getBestShortName(Locale.ENGLISH);
            if (name != null) {
                regimen += name.getName() + "+";
            }
        }

        return stripLastChar(regimen, '+');
    }

    private String getRegimenLine(String regimen) {
        String line = "First line";
        if (regimen.contains("AZT")
                || regimen.contains("TDF")
                || regimen.contains("3TC")
                || regimen.contains("NVP")
                || regimen.contains("EVF")) {
            line = "First line";
        }
        if (regimen.contains("RTV")
                || regimen.contains("LPV")
                || regimen.contains("3TC")
                || regimen.contains("ATV")
                || regimen.contains("D4T")) {
            line = "Second line";
        }
        return line;
    }

    private String stripLastChar(String str, char last) {
        if (str.length() > 0 && str.charAt(str.length() - 1) == last) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
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

    protected List<Object> getPatientStatusHeaderRow() {
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

    protected List<Object> getPatientVisitHeaderRow() {
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
        headerRow.add("OI");
        headerRow.add("OIDate");
        headerRow.add("SubstitutionFirstlineRegimenDate");
        headerRow.add("SubstitutionFirstlineRegimenReason");
        headerRow.add("SubstitutionSecondlineRegimenDate");
        headerRow.add("SubstitutionSecondlineRegimenReason");
        headerRow.add("SecondlineRegimenChangeDate");
        headerRow.add("SecondlineRegimenChangeReason");
        headerRow.add("Adherence");
        headerRow.add("AdherenceCategory");
        headerRow.add("FamilyPlanningMethod");
        headerRow.add("PwP");
        headerRow.add("GestationAge");
        headerRow.add("NextAppointmentDate");
        return headerRow;
    }

    protected List<Object> getPatientLaboratoryHeaderRow() {
        List<Object> headerRow = new ArrayList<Object>();
        headerRow.add("PatientID");
        headerRow.add("PatientPK");
        headerRow.add("SiteCode");
        headerRow.add("FacilityName");
        headerRow.add("VisitID");
        headerRow.add("OrderedByDate");
        headerRow.add("ReportedByDate");
        headerRow.add("TestName");
        headerRow.add("TestResult");
        return headerRow;
    }

    protected List<Object> getPatientPharmacyExtractHeaderRow() {
        List<Object> headerRow = new ArrayList<Object>();
        headerRow.add("PatientID");
        headerRow.add("SiteCode");
        headerRow.add("FacilityName");
        headerRow.add("PatientPK");
        headerRow.add("VisitID");
        headerRow.add("Drug");
        headerRow.add("DispenseDate");
        headerRow.add("Duration");
        headerRow.add("ExpectedReturn");
        headerRow.add("TreatmentType");
        headerRow.add("PeriodTaken");
        headerRow.add("ProphylaxisType");
        return headerRow;
    }

    protected List<Object> getPatientWABWHOCD4ExtractHeaderRow() {
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
        return headerRow;
    }

            protected List<Object> getARTPatientExtractHeaderRow() {
        List<Object> headerRow = new ArrayList<Object>();
        headerRow.add("PatientPK");
        headerRow.add("PatientID");
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

    private PersonInfo getPersonInfo(Patient patient) {
        PersonInfo personInfo = new PersonInfo();
        if (patient.getBirthdate() != null) {
            personInfo.birthDate = DATE_FORMAT.format(patient.getBirthdate());
        }
        personInfo.gender = patient.getGender();
        personInfo.pk = patient.getId();
        if (patient.getDateCreated() != null) {
            personInfo.dateCreated = DATE_FORMAT.format(patient.getDateCreated());
        }

        PatientIdentifier upn = patient.getPatientIdentifier("Unique Patient Number");
        personInfo.upn = upn != null ? upn.getIdentifier() : "";

        PersonAttribute attribute = patient.getAttribute("Next of kin name");
        personInfo.nextOfKinName = attribute != null ? attribute.getValue() : "";
        personInfo.patient = patient;

        return personInfo;
    }

    private EncounterInfo getFirstEncounterInfo(Patient patient) {
        return getEncounterInfo(patient, (EncounterType) null, true);
    }

    private EncounterInfo getHivEnrollmentEncounterInfo(Patient patient) {
        return getEncounterInfo(patient,
                encounterService.getEncounterTypeByUuid("de78a6be-bfc5-4634-adc3-5f1a280455cc"), false);
    }

    private EncounterInfo getHivDiscontinuationEncounterInfo(Patient patient) {
        return getEncounterInfo(patient,
                encounterService.getEncounterTypeByUuid("2bdada65-4c72-4a48-8730-859890e25cee"), false);
    }

    private EncounterInfo getMchmsEnrollmentEncounterInfo(Patient patient) {
        return getEncounterInfo(patient, "3ee036d8-7c13-4393-b5d6-036f2fe45126", false);
    }

    private EncounterInfo getTbEnrollmentEncounterInfo(Patient patient) {
        return getEncounterInfo(patient, "9d8498a4-372d-4dc4-a809-513a2434621e", false);
    }

    private List<EncounterInfo> getOtherMedicationEncounters(Patient patient) {
        Form form = formService.getFormByUuid("d4ff8ad1-19f8-484f-9395-04c755de9a47");
        return getEncounterInfos(patient, null, form, false);
    }

    private EncounterInfo getEncounterInfo(Patient patient, String encounterTypeuuid, boolean includeLocation) {
        EncounterType encounterType = encounterService.getEncounterTypeByUuid(encounterTypeuuid);
        return getEncounterInfo(patient, encounterType, includeLocation);
    }

    private EncounterInfo getEncounterInfo(Patient patient, EncounterType encounterType, boolean includeLocation) {
        return getEncounterInfo(patient, encounterType, null, includeLocation);
    }

    private EncounterInfo getEncounterInfo(Patient patient, EncounterType encounterType, Form form, boolean includeLocation) {
        List<EncounterInfo> encounterInfos = getEncounterInfos(patient, encounterType, form, includeLocation);
        if (encounterInfos != null && !encounterInfos.isEmpty()) {
            return encounterInfos.get(0);
        }
        return new EncounterInfo();
    }

    private List<EncounterInfo> getEncounterInfos(Patient patient, EncounterType encounterType, Form form, boolean includeLocation) {
        List<EncounterInfo> encounterInfos = new ArrayList<EncounterInfo>();

        List<EncounterType> encounterTypes = null;
        if (encounterType != null) {
            encounterTypes = new ArrayList<EncounterType>();
            encounterTypes.add(encounterType);
        }

        List<Form> forms = null;
        if (form != null) {
            forms = new ArrayList<Form>();
            forms.add(form);
        }

        List<Encounter> encounters = encounterService.getEncounters(patient, null, null, null, forms, encounterTypes,
                null, true);
        if (encounters != null && !encounters.isEmpty()) {
            for (Encounter encounter : encounters) {
                EncounterInfo encounterInfo = new EncounterInfo();
                LocationInfo locationInfo = new LocationInfo();
                encounterInfo.locationInfo = locationInfo;
                if (encounter.getEncounterDatetime() != null) {
                    encounterInfo.encounterDate = DATE_FORMAT.format(encounter.getEncounterDatetime());
                }
                if (includeLocation) {
                    Location location = encounter.getLocation();
                    locationInfo.facilityName = location.getName();
                    locationInfo.region = location.getStateProvince();
                    locationInfo.district = location.getCountyDistrict();
                    locationInfo.village = location.getCityVillage();
                    LocationAttributeType codeAttrType = MetadataUtils.existing(LocationAttributeType.class,
                            FacilityMetadata._LocationAttributeType.MASTER_FACILITY_CODE);
                    List<LocationAttribute> locationAttributes = location.getActiveAttributes(codeAttrType);
                    if (locationAttributes != null && !locationAttributes.isEmpty()) {
                        LocationAttribute la = locationAttributes.get(0);
                        locationInfo.mfl = la.getValueReference();
                    }
                    locationInfo.location = location;
                }
                encounterInfo.encounter = encounter;
                encounterInfos.add(encounterInfo);
            }

        }
        return encounterInfos;
    }

    private List<VisitInfo> getLabVisitInfos(Patient patient, String encounterTypeuuid) {
        EncounterType encounterType = encounterService.getEncounterTypeByUuid(encounterTypeuuid);
        List<EncounterType> encounterTypes = null;
        if (encounterType != null) {
            encounterTypes = new ArrayList<EncounterType>();
            encounterTypes.add(encounterType);
        }
        List<VisitInfo> visitInfos = new ArrayList<VisitInfo>();
        List<Encounter> encounters = encounterService.getEncounters(patient, null, null, null, null, encounterTypes,
                null, true);
        if (encounters != null && !encounters.isEmpty()) {
            for (Encounter encounter : encounters) {
                Visit visit = encounter.getVisit();
                if (visit != null) {
                    VisitInfo visitInfo = new VisitInfo();
                    visitInfo.pk = visit.getId();
                    visitInfo.encounter = encounter;
                    visitInfos.add(visitInfo);
                }
            }
        }
        return visitInfos;
    }

    private VisitInfo getLastVisitInfo(Patient patient) {
        VisitInfo visitInfo = new VisitInfo();
        List<Visit> visits = visitService.getVisitsByPatient(patient);
        if (visits != null && !visits.isEmpty()) {
            Visit last = visits.get(visits.size() - 1);
            if (last.getStartDatetime() != null) {
                visitInfo.visitDate = DATE_FORMAT.format(last.getStartDatetime());
                visitInfo.visit = last;
            }
        }
        return visitInfo;
    }

//    private List<VisitInfo> getVisitInfos(Patient patient) {
//        List<VisitInfo> visitInfos = new ArrayList<VisitInfo>();
//        List<Visit> visits = visitService.getVisitsByPatient(patient);
//        for (Visit visit : visits) {
//            VisitInfo visitInfo = new VisitInfo();
//            visitInfo.pk = visit.getId();
//            visitInfo.type = visit.getVisitType().getName();
//            if (visit.getStartDatetime() != null) {
//                visitInfo.visitDate = DATE_FORMAT.format(visit.getStartDatetime());
//            }
//            visitInfo.visit = visit;
//            visitInfos.add(visitInfo);
//        }
//        return visitInfos;
//    }

    private Map<Integer, List<VisitInfo>> getVisitInfoMap(List<Patient> patients) {
        Map<Integer, List<VisitInfo>> visitInfoMap = new HashMap<Integer, List<VisitInfo>>();
        List<Visit> visits = visitService.getVisits(null, patients, null, null, null, null, null, null, null, true, false);
        for (Visit visit : visits) {
            List<VisitInfo> visitInfos = visitInfoMap.get(visit.getPatient().getId());
            if (visitInfos == null) {
                visitInfos = new ArrayList<VisitInfo>();
                visitInfoMap.put(visit.getPatient().getId(), visitInfos);
            }
            VisitInfo visitInfo = new VisitInfo();
            visitInfo.pk = visit.getId();
            visitInfo.type = visit.getVisitType().getName();
            if (visit.getStartDatetime() != null) {
                visitInfo.visitDate = DATE_FORMAT.format(visit.getStartDatetime());
            }
            visitInfo.visit = visit;
            visitInfos.add(visitInfo);
        }
        return visitInfoMap;
    }

    private ObsInfo getObsInfo(Patient patient, String conceptUuid, boolean first) {
        Concept concept = conceptService.getConceptByUuid(conceptUuid);
        return getObsInfo(patient, concept, first);
    }

    private ObsInfo getObsInfo(Patient patient, Concept concept, boolean first) {
        ObsInfo obsInfo = new ObsInfo();
        List<Obs> obsList = obsService.getObservationsByPersonAndConcept(patient, concept);
        if (obsList != null && !obsList.isEmpty()) {
            Obs obs;
            if (first) {
                obs = obsList.get(0);
            } else {
                obs = obsList.get(obsList.size() - 1);
            }
            obsInfo.value = obs.getValueAsString(Locale.ENGLISH);
            if (obs.getObsDatetime() != null) {
                obsInfo.date = DATE_FORMAT.format(obs.getObsDatetime());
            }
        }
        return obsInfo;
    }

    private List<ObsInfo> getObsInfos(Patient patient, String conceptUuid) {
        Concept concept = conceptService.getConceptByUuid(conceptUuid);
        return getObsInfos(patient, concept);
    }

    private List<ObsInfo> getObsInfos(Patient patient, Concept concept) {
        List<ObsInfo> obsInfos = new ArrayList<ObsInfo>();
        List<Obs> obsList = obsService.getObservationsByPersonAndConcept(patient, concept);
        Collections.sort(obsList, new Comparator<Obs>() {
            @Override
            public int compare(Obs o1, Obs o2) {
                return o1.getObsDatetime().compareTo(o2.getObsDatetime());
            }
        });
        if (obsList != null && !obsList.isEmpty()) {
            for (Obs obs : obsList) {
                ObsInfo obsInfo = new ObsInfo();
                obsInfo.value = obs.getValueAsString(Locale.ENGLISH);
                obsInfo.date = DATE_FORMAT.format(obs.getObsDatetime());
                obsInfo.obs = obs;
                obsInfos.add(obsInfo);
            }
        }
        return obsInfos;
    }

    private ObsInfo getObsInfo(Patient patient, Visit visit, String conceptUuid, boolean first) {
        ObsInfo obsInfo = new ObsInfo();

        List<Person> persons = new ArrayList<Person>();
        persons.add(patient);

        List<Encounter> encounters = new ArrayList<Encounter>();
        encounters.addAll(visit.getEncounters());

        Concept concept = conceptService.getConceptByUuid(conceptUuid);
        List<Concept> concepts = new ArrayList<Concept>();
        concepts.add(concept);

        List<Obs> obsList = obsService.getObservations(persons, encounters, concepts, null, null, null,
                null, null, null, null, null, false);

        if (obsList != null && !obsList.isEmpty()) {
            Obs obs;
            if (first) {
                obs = obsList.get(0);
            } else {
                obs = obsList.get(obsList.size() - 1);
            }
            obsInfo.value = obs.getValueAsString(Locale.ENGLISH);
            if (obs.getObsDatetime() != null) {
                obsInfo.date = DATE_FORMAT.format(obs.getObsDatetime());
            }
        }
        return obsInfo;
    }

    private Map<String, ObsInfo> getObsInfoMap(Patient patient, boolean first, String... conceptUuids) {
        return getObsInfoMap(patient, (List<Encounter>) null, first, conceptUuids);
    }

    private Map<String, ObsInfo> getObsInfoMap(Patient patient, Encounter encounter,
                                               boolean first, String... conceptUuids) {
        List<Encounter> encounters = new ArrayList<Encounter>();
        encounters.add(encounter);
        return getObsInfoMap(patient, encounters, first, conceptUuids);
    }

    private Map<String, ObsInfo> getObsInfoMap(Patient patient, Visit visit,
                                               boolean first, String... conceptUuids) {
        List<Encounter> encounters = new ArrayList<Encounter>();
        encounters.addAll(visit.getEncounters());
        return getObsInfoMap(patient, encounters, first, conceptUuids);
    }

    private Map<String, ObsInfo> getObsInfoMap(Patient patient, List<Encounter> encounters,
                                               boolean first, String... conceptUuids) {

        Map<String, ObsInfo> obsInfoMap = new HashMap<String, ObsInfo>();

        List<Person> persons = new ArrayList<Person>();
        persons.add(patient);

        List<Concept> concepts = new ArrayList<Concept>();
        for (String conceptUuid : conceptUuids) {
            concepts.add(conceptService.getConceptByUuid(conceptUuid));
        }

        List<Obs> obsList = obsService.getObservations(persons, encounters, concepts, null, null, null,
                null, null, null, null, null, false);

        for (Obs obs : obsList) {
            ObsInfo obsInfo = new ObsInfo();
            obsInfo.value = obs.getValueAsString(Locale.ENGLISH);
            if (obs.getObsDatetime() != null) {
                obsInfo.date = DATE_FORMAT.format(obs.getObsDatetime());
            }
            if (first) {
                if (!obsInfoMap.containsKey(obs.getConcept().getUuid())) {
                    obsInfoMap.put(obs.getConcept().getUuid(), obsInfo);
                }
            } else {
                obsInfoMap.put(obs.getConcept().getUuid(), obsInfo);
            }
        }

        return obsInfoMap;
    }

    private ObsInfo getObsInfo(Patient patient, Encounter encounter, String conceptUuid, boolean first) {
        ObsInfo obsInfo = new ObsInfo();

        List<Person> persons = new ArrayList<Person>();
        persons.add(patient);

        List<Encounter> encounters = new ArrayList<Encounter>();
        encounters.add(encounter);

        Concept concept = conceptService.getConceptByUuid(conceptUuid);
        List<Concept> concepts = new ArrayList<Concept>();
        concepts.add(concept);

        List<Obs> obsList = obsService.getObservations(persons, encounters, concepts, null, null, null,
                null, null, null, null, null, false);

        if (obsList != null && !obsList.isEmpty()) {
            Obs obs;
            if (first) {
                obs = obsList.get(0);
            } else {
                obs = obsList.get(obsList.size() - 1);
            }
            obsInfo.value = obs.getValueAsString(Locale.ENGLISH);
            if (obs.getObsDatetime() != null) {
                obsInfo.date = DATE_FORMAT.format(obs.getObsDatetime());
            }
        }
        return obsInfo;
    }

    private Date getARTInitDate(Patient patient) {

        List<Order> orders = orderService.getOrders(patient, outpatient, drugOrderType, false);//getOrdersByPatient(patient);
        if (orders != null && !orders.isEmpty()) {
            Collections.sort(orders, new Comparator<Order>() {
                @Override
                public int compare(Order o1, Order o2) {
                    return o1.getDateActivated().compareTo(o2.getDateActivated());
                }
            });
            return orders.get(0).getDateActivated();
        }
        return null;
    }

    private Map<String, List<Order>> getDrugOrders(Patient patient) {
        Map<String, List<Order>> collectiveOrders = new LinkedHashMap<String, List<Order>>();

        List<Order> orders = orderService.getOrders(patient, outpatient, drugOrderType, false);//getOrdersByPatient(patient);
        if (orders != null && !orders.isEmpty()) {
            Collections.sort(orders, new Comparator<Order>() {
                @Override
                public int compare(Order o1, Order o2) {
                    return o1.getDateActivated().compareTo(o2.getDateActivated());
                }
            });
            for (Order order : orders) {
                String date = DATE_FORMAT.format(order.getDateActivated());
                if (!collectiveOrders.containsKey(date)) {
                    collectiveOrders.put(date, new ArrayList<Order>());
                }
                List<Order> orderedAtOnce = collectiveOrders.get(date);
                orderedAtOnce.add(order);
            }
        }
        return collectiveOrders;
    }


    private class PersonInfo {

        private int pk;
        private String upn;
        private String gender;
        private String birthDate;
        private String nextOfKinName;
        private String dateCreated;
        private Patient patient;
    }

    private class LocationInfo {

        private String mfl;
        private String facilityName;
        private String region;
        private String district;
        private String village;
        private Location location;
    }

    private class EncounterInfo {

        private String encounterDate;
        private LocationInfo locationInfo;
        private Encounter encounter;
    }

    private class VisitInfo {

        private int pk;
        private String type;
        private String visitDate;
        private Visit visit;
        private Encounter encounter;
    }

    private class ObsInfo {
        private String value;
        private String date;
        private Obs obs;
    }

    private static class AuxilliaryInfo {

        private final static String EMR = "KenyaEMR";
        private final static String PROJECT = "I-TECH";
    }

    private String fromObsDateToStandardDate(String obsDate) {
        if (obsDate == null) {
            return obsDate;
        }
        try {
            Date date = OBS_DATE_FORMAT.parse(obsDate);
            return DATE_FORMAT.format(date);
        } catch (Exception ex) {
            return obsDate;
        }
    }

    public String timeStamp() {
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HHmmss");
        return format.format(now);
    }

    public String location() {
        AdministrationService administrationService = org.openmrs.api.context.Context.getAdministrationService();
        GlobalProperty globalProperty = administrationService.getGlobalPropertyObject("kenyaemr.defaultLocation");
        if (globalProperty.getValue() != null) {
            return ((Location) globalProperty.getValue()).getName();
        }
        return "Unknown Location";
    }

}
