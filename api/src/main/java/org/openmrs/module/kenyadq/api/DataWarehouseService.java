package org.openmrs.module.kenyadq.api;

/**
 * Created by gitahi on 28/07/15.
 */
public interface DataWarehouseService {

    byte[] downloadPatientExtract();

    byte[] downloadPatientStatusExtract();

    byte[] downloadPatientVisitExtract();

    byte[] downloadPatientLaboratoryExtract();

    byte[] downloadPatientPharmacyExtract();

    byte[] downloadPatientWABWHOCD4Extract();

    byte[] downloadARTPatientExtract();

    byte[] downloadAll();
}
