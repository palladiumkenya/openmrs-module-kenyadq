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
package org.openmrs.module.kenyadq.calculation.rdqa;

import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.calculation.result.SimpleResult;
import org.openmrs.module.kenyacore.calculation.AbstractPatientCalculation;
import org.openmrs.module.kenyaemr.Dictionary;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Returns the date of the last CTX dispensed
 */
public class DateOfLastCTXCalculation extends AbstractPatientCalculation {

	Concept ctxConcept = Dictionary.getConcept(Dictionary.COTRIMOXAZOLE_DISPENSED);
	Concept yes = Dictionary.getConcept(Dictionary.YES);
	Concept medicationOrder = Dictionary.getConcept(Dictionary.MEDICATION_ORDERS);
	Concept sulphurCtx = Dictionary.getConcept(Dictionary.SULFAMETHOXAZOLE_TRIMETHOPRIM);

	@Override
	public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues, PatientCalculationContext context) {

		VisitService service = Context.getVisitService();
		PatientService patientService = Context.getPatientService();
		EncounterService encounterService = Context.getEncounterService();
		ObsService obsService = Context.getObsService();

		CalculationResultMap ret = new CalculationResultMap();

		for (Integer ptid : cohort) {
			Date lastCtx = null;
			Patient patient = patientService.getPatient(ptid);
			List<Visit> pVisits = service.getVisitsByPatient(patient);
			if (pVisits.size() > 0) {

				Integer listSize = pVisits.size();
				List<Visit> requiredVisits = listSize <= 4? pVisits : pVisits.subList(listSize - 4, listSize-1);
				//reverse elements in the list
				Collections.reverse(requiredVisits);

				for (Visit v : requiredVisits) {
					List<Encounter> encounters = encounterService.getEncountersByVisit(v, false);
					if (encounters.size() > 0) {
						List<Obs> obsList = obsService.getObservations(Arrays.asList(Context.getPersonService().getPerson(ptid)), encounters, Arrays.asList(ctxConcept, medicationOrder), Arrays.asList(yes, sulphurCtx), null, null, null, null, null, null, null, false);

						if (obsList.size() > 0) {
							Collections.reverse(obsList);
							lastCtx = obsList.get(0).getObsDatetime();
							break;
						}
					}
				}
			}
			ret.put(ptid, new SimpleResult(lastCtx, this));

		}

		return ret;
	}


}