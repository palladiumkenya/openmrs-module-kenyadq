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

package org.openmrs.module.kenyadq.api.impl;

import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.kenyadq.api.KenyaDqService;
import org.openmrs.serialization.SerializationException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
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

	/**
	 * @see org.openmrs.module.kenyadq.api.KenyaDqService#mergePatients(org.openmrs.Patient, org.openmrs.Patient)
	 */
	public void mergePatients(Patient preferred, Patient notPreferred) throws APIException {
		try {
			Set<PatientIdentifier> preferredPatientIdentifiers = new HashSet<PatientIdentifier>(preferred.getActiveIdentifiers());

			patientService.mergePatients(preferred, notPreferred);

			for (Map.Entry<PatientIdentifierType, List<PatientIdentifier>> entry : getAllPatientIdentifiers(preferred).entrySet()) {
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
		}
		catch (SerializationException ex) {
			throw new APIException(ex);
		}
	}

	/**
	 * Helper method to get all of a patient's identifiers organized by type
	 * @param patient the patient
	 * @return the map of identifier types to identifiers
	 */
	protected Map<PatientIdentifierType, List<PatientIdentifier>> getAllPatientIdentifiers(Patient patient) {
		Map<PatientIdentifierType, List<PatientIdentifier>> ids = new HashMap<PatientIdentifierType, List<PatientIdentifier>>();
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
}