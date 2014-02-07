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

package org.openmrs.module.kenyadq.fragment.controller.patient;

import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyadq.DqConstants;
import org.openmrs.module.kenyaui.annotation.AppAction;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.fragment.action.FailureResult;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Merge utility actions
 */
public class MergeUtilsFragmentController {

	@AppAction(DqConstants.APP_DATAQUALITY)
	public Object getDuplicatePatients(@RequestParam("byIdentifier") boolean byIdentifier,
								@RequestParam("byFamilyName") boolean byFamilyName,
								@RequestParam("byGivenName") boolean byGivenName,
								@RequestParam("byGender") boolean byGender,
								@RequestParam("byBirthdate") boolean byBirthdate,
								UiUtils ui) {

		List<String> attributes = new ArrayList<String>();
		if (byIdentifier) {
			attributes.add("identifier");
		}
		if (byFamilyName) {
			attributes.add("familyName");
		}
		if (byGivenName) {
			attributes.add("givenName");
		}
		if (byGender) {
			attributes.add("gender");
		}
		if (byBirthdate) {
			attributes.add("birthdate");
		}

		if (attributes.size() > 0) {
			List<Patient> patients = Context.getPatientService().getDuplicatePatientsByAttributes(attributes);
			return ui.simplifyCollection(patients);
		}
		else {
			return new FailureResult("Must specify at least one thing to match by");
		}
	}

}