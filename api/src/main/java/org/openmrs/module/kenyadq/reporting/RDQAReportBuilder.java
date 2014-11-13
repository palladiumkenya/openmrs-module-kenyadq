/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.kenyadq.reporting;

import org.openmrs.Concept;
import org.openmrs.PatientIdentifierType;
import org.openmrs.calculation.patient.PatientCalculation;
import org.openmrs.module.kenyacore.report.CohortReportDescriptor;
import org.openmrs.module.kenyacore.report.ReportUtils;
import org.openmrs.module.kenyacore.report.builder.AbstractCohortReportBuilder;
import org.openmrs.module.kenyacore.report.builder.Builds;
import org.openmrs.module.kenyacore.report.data.patient.definition.CalculationDataDefinition;
import org.openmrs.module.kenyadq.calculation.rdqa.CD4AtArtStartDateCalculation;
import org.openmrs.module.kenyadq.calculation.rdqa.DateOfDeathCalculation;
import org.openmrs.module.kenyadq.calculation.rdqa.DateOfLastCTXCalculation;
import org.openmrs.module.kenyadq.calculation.rdqa.PatientCheckOutStatusCalculation;
import org.openmrs.module.kenyadq.calculation.rdqa.PatientLastEncounterDateCalculation;
import org.openmrs.module.kenyadq.calculation.rdqa.ValueAtDateOfOtherPatientCalculationCalculation;
import org.openmrs.module.kenyadq.calculation.rdqa.VisitsForAPatientCalculation;
import org.openmrs.module.kenyadq.calculation.rdqa.WeightAtArtStartDateCalculation;
import org.openmrs.module.kenyadq.converter.ConceptNamesDataConverter;
import org.openmrs.module.kenyadq.converter.ObsDatetimeConverter;
import org.openmrs.module.kenyadq.converter.ObsValueDatetimeConverter;
import org.openmrs.module.kenyadq.converter.ObsValueNumericConverter;
import org.openmrs.module.kenyadq.converter.WHOStageDataConverter;
import org.openmrs.module.kenyaemr.Dictionary;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.CurrentArtRegimenCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.InitialArtStartDateCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.TransferInDateCalculation;
import org.openmrs.module.kenyaemr.calculation.library.hiv.art.TransferOutDateCalculation;
import org.openmrs.module.kenyaemr.metadata.HivMetadata;
import org.openmrs.module.kenyaemr.reporting.data.converter.CalculationResultConverter;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.common.TimeQualifier;
import org.openmrs.module.reporting.data.DataDefinition;
import org.openmrs.module.reporting.data.converter.BirthdateConverter;
import org.openmrs.module.reporting.data.converter.DataConverter;
import org.openmrs.module.reporting.data.converter.ObjectFormatter;
import org.openmrs.module.reporting.data.patient.definition.ConvertedPatientDataDefinition;
import org.openmrs.module.reporting.data.patient.definition.PatientIdentifierDataDefinition;
import org.openmrs.module.reporting.data.person.definition.BirthdateDataDefinition;
import org.openmrs.module.reporting.data.person.definition.ConvertedPersonDataDefinition;
import org.openmrs.module.reporting.data.person.definition.GenderDataDefinition;
import org.openmrs.module.reporting.data.person.definition.ObsForPersonDataDefinition;
import org.openmrs.module.reporting.data.person.definition.PersonIdDataDefinition;
import org.openmrs.module.reporting.data.person.definition.PreferredNameDataDefinition;
import org.openmrs.module.reporting.dataset.definition.PatientDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.springframework.stereotype.Component;

@Component
@Builds({"kenyadq.common.report.rdqaReport"})
public class RDQAReportBuilder extends AbstractCohortReportBuilder {
	public static final String DATE_FORMAT = "dd-MM-yyyy";

	/**
	 *
	 * @see org.openmrs.module.kenyacore.report.builder.AbstractCohortReportBuilder#addColumns(org.openmrs.module.kenyacore.report.CohortReportDescriptor, org.openmrs.module.reporting.dataset.definition.PatientDataSetDefinition)
	 */
	@Override
	protected void addColumns(CohortReportDescriptor report, PatientDataSetDefinition dsd) {

		PatientIdentifierType upn = MetadataUtils.existing(PatientIdentifierType.class, HivMetadata._PatientIdentifierType.UNIQUE_PATIENT_NUMBER);
		DataConverter identifierFormatter = new ObjectFormatter("{identifier}");
		DataDefinition identifierDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(upn.getName(), upn), identifierFormatter);

		Concept cd4Concept = Dictionary.getConcept(Dictionary.CD4_COUNT);
		Concept weightConcept = Dictionary.getConcept(Dictionary.WEIGHT_KG);

		InitialArtStartDateCalculation artStartDate = new InitialArtStartDateCalculation();
		PatientCalculation hybridCalc = new ValueAtDateOfOtherPatientCalculationCalculation(artStartDate, cd4Concept);


		DataConverter nameFormatter = new ObjectFormatter("{familyName}, {givenName}");
		DataDefinition nameDef = new ConvertedPersonDataDefinition("name", new PreferredNameDataDefinition(), nameFormatter);

		dsd.addColumn("id", new PersonIdDataDefinition(), "");
		dsd.addColumn("Unique Patient No", identifierDef, "");
		dsd.addColumn("Name", nameDef, "");
        dsd.addColumn("Sex", new GenderDataDefinition(), "");
		dsd.addColumn("Date of Birth", new BirthdateDataDefinition(), "", new BirthdateConverter(DATE_FORMAT));
		dsd.addColumn("Art Start Date", new CalculationDataDefinition("Art Start Date", new InitialArtStartDateCalculation()), "", new CalculationResultConverter());
		dsd.addColumn("Entry Point", new ObsForPersonDataDefinition("Entry Point", TimeQualifier.LAST, Dictionary.getConcept(Dictionary.METHOD_OF_ENROLLMENT), null, null), "", new ConceptNamesDataConverter());
		dsd.addColumn("CD4 at Art Start", new CalculationDataDefinition("CD4 at Art Start", new CD4AtArtStartDateCalculation()), "", new CalculationResultConverter());
		dsd.addColumn("Weight at Art Start", new CalculationDataDefinition("Weight at Art Start", new WeightAtArtStartDateCalculation()), "", new CalculationResultConverter());
		dsd.addColumn("Current Regimen", new CalculationDataDefinition("Current Regimen", new CurrentArtRegimenCalculation()), "", new CalculationResultConverter());
		dsd.addColumn("Transfer In Date", new CalculationDataDefinition("Transfer In Date", new TransferInDateCalculation()), "", new CalculationResultConverter());
		dsd.addColumn("Transfer Out Date", new CalculationDataDefinition("Transfer Out Date", new TransferOutDateCalculation()), "", new CalculationResultConverter());
		dsd.addColumn("Date of Death", new CalculationDataDefinition("Date of Death", new DateOfDeathCalculation()), "", new CalculationResultConverter());
		dsd.addColumn("First CD4 Count", new ObsForPersonDataDefinition("First CD4 Count", TimeQualifier.FIRST, Dictionary.getConcept(Dictionary.CD4_COUNT), null, null), "", new ObsValueNumericConverter(1));
		dsd.addColumn("First CD4 Count Date", new ObsForPersonDataDefinition("First CD4 Count Date", TimeQualifier.FIRST, Dictionary.getConcept(Dictionary.CD4_COUNT), null, null), "", new ObsDatetimeConverter());
		dsd.addColumn("Last CD4 Count", new ObsForPersonDataDefinition("Last CD4 Count", TimeQualifier.LAST, Dictionary.getConcept(Dictionary.CD4_COUNT), null, null), "", new ObsValueNumericConverter(1));
		dsd.addColumn("Last CD4 Count Date", new ObsForPersonDataDefinition("Last CD4 Count Date", TimeQualifier.LAST, Dictionary.getConcept(Dictionary.CD4_COUNT), null, null), "", new ObsDatetimeConverter());

		dsd.addColumn("First WHO Stage", new ObsForPersonDataDefinition("First WHO Stage", TimeQualifier.FIRST, Dictionary.getConcept(Dictionary.CURRENT_WHO_STAGE), null, null), "", new WHOStageDataConverter());
		dsd.addColumn("First WHO Stage Date", new ObsForPersonDataDefinition("First WHO Stage Date", TimeQualifier.FIRST, Dictionary.getConcept(Dictionary.CURRENT_WHO_STAGE), null, null), "", new ObsDatetimeConverter());
		dsd.addColumn("Last WHO Stage", new ObsForPersonDataDefinition("Last WHO Stage", TimeQualifier.LAST, Dictionary.getConcept(Dictionary.CURRENT_WHO_STAGE), null, null), "", new WHOStageDataConverter());
		dsd.addColumn("Last WHO Stage Date", new ObsForPersonDataDefinition("Last WHO Stage Date", TimeQualifier.LAST, Dictionary.getConcept(Dictionary.CURRENT_WHO_STAGE), null, null), "", new ObsDatetimeConverter());

		dsd.addColumn("CTX/Dapsone last, documentation date", new CalculationDataDefinition("CTX/Dapsone last, documentation date", new DateOfLastCTXCalculation()), "", new CalculationResultConverter());
		dsd.addColumn("Last encounter date in the blue card", new CalculationDataDefinition("Last Encounter Date", new PatientLastEncounterDateCalculation()), "", new CalculationResultConverter());
		dsd.addColumn("Next Appointment Date", new ObsForPersonDataDefinition("Next Appointment Date", TimeQualifier.LAST, Dictionary.getConcept(Dictionary.RETURN_VISIT_DATE), null, null), "", new ObsValueDatetimeConverter());
		dsd.addColumn("Number of visits in paper bluecards", new CalculationDataDefinition("Total Visits", new VisitsForAPatientCalculation()), "", new DataConverter() {
			@Override
			public Class<?> getInputDataType() {
				return Integer.class;
			}

			@Override
			public Class<?> getDataType() {
				return Integer.class;
			}

			@Override
			public Object convert(Object input) {
				return input;
			}
		});

		dsd.addColumn("Patient checked-out ", new CalculationDataDefinition("Checked Out", new PatientCheckOutStatusCalculation()), "", new CalculationResultConverter());
	}

	@Override
	protected Mapped<CohortDefinition> buildCohort(CohortReportDescriptor descriptor, PatientDataSetDefinition dsd) {

        //try sql cohort methods within kenya emr
        String sql ="select   e.patient_id  " +
                "  from encounter e  " +
                "  inner join person p  " +
                "  on p.person_id=e.patient_id   " +
                "    where e.voided = 0  " +
                "    and p.voided=0   " +
                "    and DATE(e.encounter_datetime) = '2005-06-15'  ";

        CohortDefinition cd = new SqlCohortDefinition(sql);
        cd.setName("Patients who attended during a particular period");

		return ReportUtils.map(cd, "");
	}
}
