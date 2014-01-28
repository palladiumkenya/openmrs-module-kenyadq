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

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.PatientService;
import org.openmrs.module.kenyacore.test.TestUtils;
import org.openmrs.module.kenyadq.api.KenyaDqService;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.*;

/**
 * Tests for {@link KenyaDqServiceImpl}
 */
public class KenyaDqServiceImplTest extends BaseModuleContextSensitiveTest {

	@Autowired
	private PatientService patientService;

	@Autowired
	private KenyaDqService kenyaDqService;

	/**
	 * @see KenyaDqServiceImpl#mergePatients(org.openmrs.Patient, org.openmrs.Patient)
	 */
	@Test
	public void mergePatients_shouldMergePatientsNotLeavingDuplicateIds() {
		PatientIdentifierType omrsIdType = patientService.getPatientIdentifierTypeByUuid("1a339fe9-38bc-4ab3-b180-320988c0b968");
		PatientIdentifierType oldIdType = patientService.getPatientIdentifierTypeByUuid("2f470aa8-1d73-43b7-81b5-01f0c0dfa53c");

		Patient patient7 = TestUtils.getPatient(7);
		Patient patient8 = TestUtils.getPatient(8);

		TestUtils.savePatientIdentifier(patient7, omrsIdType, "123456-G"); // Give patient #7 an extra OpenMRS ID which will be purged as well
		TestUtils.savePatientIdentifier(patient8, oldIdType, "OLDFOR8");

		kenyaDqService.mergePatients(patient7, patient8);

		Assert.assertThat(patient8.isVoided(), is(true)); // Check patient #8 was voided

		Assert.assertThat(patient7.isVoided(), is(false));
		Assert.assertThat(patient7.getIdentifiers(), hasSize(3)); // 123456-G should exist but is voided
		Assert.assertThat(patient7.getActiveIdentifiers(), hasSize(2));

		Assert.assertThat(patient7.getPatientIdentifiers(omrsIdType), hasSize(1));
		Assert.assertThat(patient7.getPatientIdentifier(omrsIdType).getIdentifier(), is("6TS-4")); // Check patient #7's ID was kept

		Assert.assertThat(patient7.getPatientIdentifiers(oldIdType), hasSize(1));
		Assert.assertThat(patient7.getPatientIdentifier(oldIdType).getIdentifier(), is("OLDFOR8")); // Check patient #8's ID was copied
	}
}