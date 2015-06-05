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

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptDescription;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.APIException;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.kenyacore.CoreConstants;
import org.openmrs.module.kenyadq.api.KenyaDqService;
import org.openmrs.module.kenyadq.api.db.KenyaDqDao;
import org.openmrs.serialization.SerializationException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
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

	@Autowired
	private ConceptService conceptService;

	private KenyaDqDao dao;

	protected static final Log log = LogFactory.getLog(KenyaDqServiceImpl.class);

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
		} catch (SerializationException ex) {
			throw new APIException(ex);
		}
	}

	@Override
	public List<Object> executeSqlQuery(String query, Map<String, Object> substitutions) {
		return dao.executeSqlQuery(query, substitutions);
	}

	@Override
	public List<Object> executeHqlQuery(String query, Map<String, Object> substitutions) {
		return dao.executeHqlQuery(query, substitutions);
	}

	/**
	 * Helper method to get all of a patient's identifiers organized by type
	 *
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

	public KenyaDqDao getDao() {
		return dao;
	}

	public void setDao(KenyaDqDao dao) {
		this.dao = dao;
	}

	public byte[] downloadAnalysisFile() {
		StringWriter stringWriter = new StringWriter();
		CSVWriter writer = new CSVWriter(stringWriter);
		List<Object> columnHeaders = new ArrayList<Object>();
		initColumnHeaders(columnHeaders);
		String select = select() + dynamic(columnHeaders) + from();
		Map<String, Object> substitutions = new HashMap<String, Object>();
		List<Object> resultList = dao.executeSqlQuery(select, substitutions);
		resultList.add(0, columnHeaders.toArray());
		for (Object object : resultList) {
			Object[] values = (Object[]) object;
			String[] row = new String[values.length];
			int i = 0;
			for (Object value : values) {
				row[i] = value != null ? value.toString() : null;
				i++;
			}
			writer.writeNext(row);
		}
		String csvString = stringWriter.toString();
		return csvString.getBytes();
	}

	public byte[] downloadDataDictionary() {
		StringWriter stringWriter = new StringWriter();
		CSVWriter writer = new CSVWriter(stringWriter);

		String[] header = new String[4];
		header[0] = "concept_id";
		header[1] = "concept_name";
		header[2] = "concept_description";
		header[3] = "concept_type";
		writer.writeNext(header);
		String csvString = null;
		try {
			List<Concept> concepts = conceptService.getAllConcepts();
			List<String> kenyaEmrConceptUuids = getKenyaEmrConceptUuids();
			for (Concept concept : concepts) {
				if (!kenyaEmrConceptUuids.contains(concept.getUuid())) {
					continue;
				}
				String[] row = new String[4];
				row[0] = concept.getId().toString();
				row[1] = concept.getPreferredName(CoreConstants.LOCALE).getName();
				String description = "";
				ConceptDescription cd = concept.getDescription(CoreConstants.LOCALE);
				if (cd != null) {
					description = cd.getDescription();
				}
				row[2] = description;
				row[3] = concept.getDatatype().getName();
				writer.writeNext(row);
			}
			csvString = stringWriter.toString();
		} catch (Exception ex) {
			throw new RuntimeException("Could not download data dictionary.");
		}
		return csvString.getBytes();
	}

	private void initColumnHeaders(List<Object> columnHeaders) {
		columnHeaders.add("encounter_uuid");
		columnHeaders.add("encounter_type");
		columnHeaders.add("encounter_location");
		columnHeaders.add("encounter_datetime");
		columnHeaders.add("encounter_voided");
		columnHeaders.add("person_uuid");
		columnHeaders.add("given_name");
		columnHeaders.add("middle_name");
		columnHeaders.add("family_name");
		columnHeaders.add("gender");
		columnHeaders.add("birthdate");
		columnHeaders.add("birthdate_estimated");
		columnHeaders.add("dead");
		columnHeaders.add("death_date");
		columnHeaders.add("cause_of_death");
//		columnHeaders.add("telephone");
		columnHeaders.add("postal_address");
		columnHeaders.add("school_employer_address");
		columnHeaders.add("location");
		columnHeaders.add("village_estate");
		columnHeaders.add("county");
		columnHeaders.add("district");
		columnHeaders.add("sub_location");
		columnHeaders.add("landmark");
//		columnHeaders.add("subchief");
		columnHeaders.add("province");
		columnHeaders.add("division");
		columnHeaders.add("house_no");
		columnHeaders.add("patient_voided");
		columnHeaders.add("visit_uuid");
		columnHeaders.add("visit_type");
		columnHeaders.add("visit_start_date");
		columnHeaders.add("visit_end_date");
		columnHeaders.add("obs_uuid");
	}

	private String select() {
		String select =
				"SELECT\n" +
						"\tCAST(e.uuid AS CHAR) AS encounter_uuid,\n" +
						"\tt.`name` AS encounter_type,\n" +
						"\tl.`name` AS encounter_location,\n" +
						"\te.encounter_datetime,\n" +
						"\te.voided AS encounter_voided,\n" +
						"\tCAST(p.uuid AS CHAR) AS person_uuid,\n" +
						"\tn.given_name,\n" +
						"\tn.middle_name,\n" +
						"\tn.family_name,\n" +
						"\tp.gender,\n" +
						"\tp.birthdate,\n" +
						"\tp.birthdate_estimated,\n" +
						"\tp.dead,\n" +
						"\tp.death_date,\n" +
						"\tp.cause_of_death,\n" +
//						"\ta.address1 AS telephone,\n" +
						"\ta.address1 AS postal_address,\n" +
						"\ta.address3 AS school_employer_address,\n" +
						"\ta.address6 AS location,\n" +
						"\ta.city_village AS village_estate,\n" +
						"\ta.country AS county,\n" +
						"\ta.county_district AS district,\n" +
						"\ta.address5 AS sub_location,\n" +
						"\ta.address2 AS landmark,\n" +
//						"\ta.address5 AS subchief,\n" +
						"\ta.state_province AS province,\n" +
						"\ta.address4 AS division,\n" +
						"\ta.postal_code AS house_no,\n" +
						"\tp.voided AS patient_voided,\n" +
						"\tCAST(v.uuid AS CHAR) AS visit_uuid,\n" +
						"\tvt.`name` AS visit_type,\n" +
						"\tv.date_started AS visit_start_date,\n" +
						"\tv.date_stopped AS visit_end_date,\n" +
						"\tCAST(o.uuid AS CHAR) AS obs_uuid,\n";
		return select;
	}

	private String dynamic(List<Object> columnHeaders) {
		List<Concept> concepts = conceptService.getAllConcepts();
		List<String> kenyaEmrConceptUuids = getKenyaEmrConceptUuids();
		String dynamic = "";
		for (Concept concept : concepts) {
			if (!kenyaEmrConceptUuids.contains(concept.getUuid())) {
				continue;
			}
			ConceptDatatype cd = concept.getDatatype();
			String valueColumn = "";
			if ("Boolean".equalsIgnoreCase(cd.getName())) {
				valueColumn = "value_boolean";
			} else if ("Coded".equalsIgnoreCase(cd.getName())) {
				valueColumn = "value_coded";
			} else if ("".equalsIgnoreCase(cd.getName())) {
				valueColumn = "value_drug";
			} else if ("Date".equalsIgnoreCase(cd.getName())
					|| "Time".equalsIgnoreCase(cd.getName())
					|| "Datetime".equalsIgnoreCase(cd.getName())) {
				valueColumn = "value_datetime";
			} else if ("Numeric".equalsIgnoreCase(cd.getName())) {
				valueColumn = "value_numeric";
			} else if ("Text".equalsIgnoreCase(cd.getName())
					|| "Structured Numeric".equalsIgnoreCase(cd.getName())) {
				valueColumn = "value_text";
			} else if ("Document".equalsIgnoreCase(cd.getName())) {
				valueColumn = "value_complex";
			}
			if (!"".equalsIgnoreCase(valueColumn)) {
				String conceptName = escape(concept.getPreferredName(CoreConstants.LOCALE).getName());
				dynamic += "\tCASE WHEN o.concept_id = " + concept.getId() + " THEN o." + valueColumn
						+ " END '" + conceptName + "',\n";
				columnHeaders.add(conceptName);
			}
		}
		return trimTraillingComma(dynamic);
	}

	private String from() {
		String from =
				"\n\tFROM\n" +
						"\tencounter e\n" +
						"INNER JOIN\n" +
						"\tencounter_type t ON e.encounter_type = t.encounter_type_id\n" +
						"INNER JOIN\n" +
						"\tlocation l ON e.location_id = l.location_id\n" +
						"INNER JOIN\n" +
						"\tperson p ON e.patient_id = p.person_id\n" +
						"INNER JOIN\n" +
						"\tperson_name n ON p.person_id = n.person_id\n" +
						"INNER JOIN\n" +
						"\tperson_address a ON p.person_id = a.person_id\n" +
						"INNER JOIN\n" +
						"\tvisit v ON e.visit_id = v.visit_id\n" +
						"INNER JOIN\n" +
						"\tvisit_type vt ON v.visit_type_id = vt.visit_type_id\n" +
						"INNER JOIN\n" +
						"\tobs o ON e.encounter_id = o.encounter_id\n" +
						"WHERE\n" +
						"\tn.voided = 0\n" +
						"GROUP BY\n" +
						"\te.encounter_id\n" +
						"ORDER BY\n" +
						"\te.encounter_id;";
		return from;
	}

	private List<String> getKenyaEmrConceptUuids() {
		List<String> conceptUuids = new ArrayList<String>();
		CSVReader reader = null;
		InputStream in = null;
		try {
			in = getClass().getClassLoader().getResourceAsStream("metadata/kemr_concepts_2015-05-27.csv");
			reader = new CSVReader(new InputStreamReader(in));
			String[] nextLine;
			while ((nextLine = reader.readNext()) != null) {
				if (nextLine.length > 0) {
					String conceptUuid = nextLine[0];
					if (conceptUuid != null && !conceptUuids.contains(conceptUuid)) {
						conceptUuids.add(conceptUuid);
					}
				}
			}
		} catch (Exception ex) {
			throw new RuntimeException("Could not read KenyaEMR concepts metadata file.");
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ex) {
					log.error(ex.getMessage());
				}
			}
			if (in != null) {
				try {
					in.close();
				} catch (Exception ex) {
					log.error(ex.getMessage());
				}
			}
		}
		Collections.sort(conceptUuids);
		return conceptUuids;
	}

	private String trimTraillingComma(String untrimmed) {
		String trimmed = untrimmed.substring(0, untrimmed.length() - 2);
		return trimmed;
	}

	private String escape(String string) {
		return string.replace("'", "''");
	}
}