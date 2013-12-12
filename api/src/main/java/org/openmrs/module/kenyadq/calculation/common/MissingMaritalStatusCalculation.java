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

package org.openmrs.module.kenyadq.calculation.common;

import org.openmrs.Concept;
import org.openmrs.calculation.BaseCalculation;
import org.openmrs.calculation.patient.PatientCalculation;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.calculation.result.ResultUtil;
import org.openmrs.module.kenyacore.calculation.BooleanResult;
import org.openmrs.module.kenyacore.calculation.Calculations;
import org.openmrs.module.kenyadq.DqMetadata;
import org.openmrs.module.metadatadeploy.MetadataUtils;

import java.util.Collection;
import java.util.Map;

/**
 * Calculates patients with missing marital status
 */
public class MissingMaritalStatusCalculation extends BaseCalculation implements PatientCalculation {

	/**
	 * @see PatientCalculation#evaluate(java.util.Collection, java.util.Map, org.openmrs.calculation.patient.PatientCalculationContext)
	 */
	@Override
	public CalculationResultMap evaluate(Collection<Integer> cohort, Map<String, Object> parameterValues, PatientCalculationContext context) {
		Concept civilStatus = MetadataUtils.getConcept(DqMetadata.Concept.CIVIL_STATUS);
		CalculationResultMap civilStatusObss = Calculations.allObs(civilStatus, cohort, context);

		CalculationResultMap ret = new CalculationResultMap();
		for (int ptId : cohort) {
			boolean missing = ResultUtil.isFalse(civilStatusObss.get(ptId));

			ret.put(ptId, new BooleanResult(missing, this, context));
		}
		return ret;
	}
}