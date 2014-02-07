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
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.api.IdentifierNotUniqueException;
import org.openmrs.api.context.Context;
import org.openmrs.module.kenyadq.DqConstants;
import org.openmrs.module.kenyadq.api.KenyaDqService;
import org.openmrs.module.kenyaui.KenyaUiUtils;
import org.openmrs.module.kenyaui.annotation.AppAction;
import org.openmrs.module.kenyaui.form.ValidatingCommandObject;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.BindParams;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.annotation.MethodParam;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.openmrs.ui.framework.fragment.action.FailureResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * Merge patients form fragment
 */
public class MergePatientsFragmentController {

	protected static final Log log = LogFactory.getLog(MergePatientsFragmentController.class);

	public void controller(@FragmentParam(value = "patient1", required = false) Patient patient1,
						   @FragmentParam(value = "patient2", required = false) Patient patient2,
						   @FragmentParam(value = "returnUrl") String returnUrl,
						   FragmentModel model) {

		model.addAttribute("command", new MergePatientsForm(patient1, patient2));
		model.addAttribute("returnUrl", returnUrl);
	}

	/**
	 * Handles a merge action request
	 * @param form the form
	 * @param ui
	 * @return
	 */
	@AppAction(DqConstants.APP_DATAQUALITY)
	public Object merge(@MethodParam("newMergePatientsForm") @BindParams MergePatientsForm form,
						UiUtils ui,
						@SpringBean KenyaUiUtils kenyaUi,
						HttpSession session) {
		ui.validate(form, form, null);

		try {
			Context.getService(KenyaDqService.class).mergePatients(form.getPatient1(), form.getPatient2());

			kenyaUi.notifySuccess(session, "Patients merged successfully");
		}
		catch (IdentifierNotUniqueException ex) {
			return new FailureResult(ex.getMessage());
		}
		catch (Exception ex) {
			log.error("Unable to merge patients #" + form.getPatient1().getId() + " and #" + form.getPatient2().getId(), ex);
			return new FailureResult("Unable to merge");
		}

		return SimpleObject.fromObject(form.getPatient1(), ui, "patientId");
	}

	/**
	 * Gets a summary of the specified patient
	 * @param patient the patient
	 * @return the summary
	 */
	@AppAction(DqConstants.APP_DATAQUALITY)
	public SimpleObject patientSummary(@RequestParam("patientId") Patient patient,
									   @SpringBean KenyaUiUtils kenyaUi) {

		List<SimpleObject> infopoints = new ArrayList<SimpleObject>();
		infopoints.add(dataPoint("id", patient.getId()));
		infopoints.add(dataPoint("Gender", patient.getGender().toLowerCase().equals("f") ? "Female" : "Male"));
		infopoints.add(dataPoint("Birthdate", kenyaUi.formatDate(patient.getBirthdate())));
		infopoints.add(dataPoint("Death date", kenyaUi.formatDate(patient.getDeathDate())));
		infopoints.add(dataPoint("Created", kenyaUi.formatDate(patient.getDateCreated())));
		infopoints.add(dataPoint("Modified", kenyaUi.formatDate(patient.getDateChanged())));

		List<SimpleObject> names = new ArrayList<SimpleObject>();
		for (PersonName name : patient.getNames()) {
			names.add(dataPoint(null, name.getFullName()));
		}

		List<SimpleObject> identifiers = new ArrayList<SimpleObject>();
		for (PatientIdentifier identifier : patient.getActiveIdentifiers()) {
			identifiers.add(dataPoint(identifier.getIdentifierType().getName(), identifier.getIdentifier()));
		}

		List<SimpleObject> attributes = new ArrayList<SimpleObject>();
		for (PersonAttribute attribute : patient.getActiveAttributes()) {
			attributes.add(dataPoint(attribute.getAttributeType().getName(), attribute.getValue()));
		}

		List<SimpleObject> encounters = new ArrayList<SimpleObject>();
		for (Encounter encounter : Context.getEncounterService().getEncountersByPatient(patient)) {
			StringBuilder sb = new StringBuilder(encounter.getEncounterType().getName());
			if (encounter.getLocation() != null) {
				sb.append(" @ " + encounter.getLocation().getName());
			}
			encounters.add(dataPoint(kenyaUi.formatDate(encounter.getEncounterDatetime()), sb.toString()));
		}

		SimpleObject summary = new SimpleObject();
		summary.put("infopoints", infopoints);
		summary.put("names", names);
		summary.put("identifiers", identifiers);
		summary.put("attributes", attributes);
		summary.put("encounters", encounters);

		return summary;
	}

	/**
	 * Convenience method to create a simple data point object
	 * @param label the label
	 * @param value the value
	 * @return the simple object
	 */
	protected SimpleObject dataPoint(String label, Object value) {
		return SimpleObject.create("label", label, "value", value);
	}

	/**
	 * Creates a merge patients form
	 * @return the form
	 */
	public MergePatientsForm newMergePatientsForm() {
		return new MergePatientsForm(null, null);
	}

	/**
	 * Form command object
	 */
	public class MergePatientsForm extends ValidatingCommandObject {

		private Patient patient1;

		private Patient patient2;

		public MergePatientsForm(Patient patient1, Patient patient2) {
			this.patient1 = patient1;
			this.patient2 = patient2;
		}

		@Override
		public void validate(Object o, Errors errors) {
			require(errors, "patient1");
			require(errors, "patient2");

			if (patient1 != null && patient2 != null && patient1.equals(patient2)) {
				errors.reject("Patients must be different");
			}
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