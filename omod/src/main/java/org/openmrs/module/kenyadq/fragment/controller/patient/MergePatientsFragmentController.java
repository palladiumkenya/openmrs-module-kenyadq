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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyacore.identifier.IdentifierManager;
import org.openmrs.module.kenyadq.DataQualityConstants;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppAction;
import org.openmrs.module.kenyaui.validator.ValidatingCommandObject;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.BindParams;
import org.openmrs.ui.framework.annotation.MethodParam;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.openmrs.ui.framework.fragment.action.FailureResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Merge patients form fragment
 */
public class MergePatientsFragmentController {

	protected static final Log log = LogFactory.getLog(MergePatientsFragmentController.class);

	public void controller(@RequestParam(required = false, value = "returnUrl") String returnUrl,
						   FragmentModel model) {

		model.addAttribute("command", new MergePatientsForm());
		model.addAttribute("returnUrl", returnUrl);
	}

	/**
	 * Handles a merge action request
	 * @param form the form
	 * @param ui
	 * @return
	 */
	@AppAction(DataQualityConstants.APP_DATAQUALITY)
	public Object mergePatients(@MethodParam("newMergePatientsForm") @BindParams MergePatientsForm form, UiUtils ui) {
		ui.validate(form, form, null);

		try {
			Context.getPatientService().mergePatients(form.getPatient1(), form.getPatient2());
		}
		catch (Exception ex) {
			log.error("Unable to merge patients #" + form.getPatient1().getId() + " and #" + form.getPatient2().getId(), ex);
			new FailureResult("Unable to merge");
		}

		return SimpleObject.fromObject(form.getPatient1(), ui, "patientId");
	}

	/**
	 * Gets a summary of the specified patient
	 * @param patient the patient
	 * @return the summary
	 */
	@AppAction(DataQualityConstants.APP_DATAQUALITY)
	public SimpleObject patientSummary(@RequestParam("patientId") Patient patient,
									   UiUtils ui,
									   @SpringBean KenyaUiUtils kenyaUi,
									   @SpringBean IdentifierManager identifierManager) {

		List<SimpleObject> infopoints = new ArrayList<SimpleObject>();
		infopoints.add(dataPoint("id", patient.getId()));
		infopoints.add(dataPoint("Gender", patient.getGender().toLowerCase().equals("f") ? "Female" : "Male"));
		infopoints.add(dataPoint("Birthdate", kenyaUi.formatDate(patient.getBirthdate())));
		infopoints.add(dataPoint("Death date", kenyaUi.formatDate(patient.getDeathDate())));
		infopoints.add(dataPoint("Created", kenyaUi.formatDate(patient.getDateCreated())));
		infopoints.add(dataPoint("Modified", kenyaUi.formatDate(patient.getDateChanged())));

		List<SimpleObject> identifiers = new ArrayList<SimpleObject>();
		for (PatientIdentifier identifier : identifierManager.getPatientDisplayIdentifiers(patient)) {
			identifiers.add(dataPoint(identifier.getIdentifierType().getName(), identifier.getIdentifier()));
		}

		SimpleObject summary = new SimpleObject();
		summary.put("infopoints", infopoints);
		summary.put("identifiers", identifiers);

		return summary;
	}

	protected SimpleObject dataPoint(String label, Object value) {
		return SimpleObject.create("label", label, "value", value);
	}

	/**
	 * Creates a merge patients form
	 * @return the form
	 */
	public MergePatientsForm newMergePatientsForm() {
		return new MergePatientsForm();
	}

	/**
	 * Form command object
	 */
	public class MergePatientsForm extends ValidatingCommandObject {

		private Patient patient1;

		private Patient patient2;

		@Override
		public void validate(Object o, Errors errors) {
			require(errors, "patient1");
			require(errors, "patient2");
		}

		/**
		 * Gets the first patient (preferred)
		 * @return the patient
		 */
		public Patient getPatient1() {
			return patient1;
		}

		/**
		 * Sets the first patient (preferred)
		 * @param patient1 the patient
		 */
		public void setPatient1(Patient patient1) {
			this.patient1 = patient1;
		}

		/**
		 * Gets the second patient
		 * @return the patient
		 */
		public Patient getPatient2() {
			return patient2;
		}

		/**
		 * Sets the second patient
		 * @param patient2 the patient
		 */
		public void setPatient2(Patient patient2) {
			this.patient2 = patient2;
		}
	}
}