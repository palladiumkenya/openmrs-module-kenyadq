package org.openmrs.module.kenyadq.utils;

import org.openmrs.api.context.Context;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaemr.wrapper.Facility;

/**
 * Created by amosl on 1/16/17.
 */
public class KenyaDqUtils {

    private static Facility facility = new Facility(Context.getService(KenyaEmrService.class).getDefaultLocation());

    public static String getMflCode() {
        return facility.getMflCode();
    }

    public static String getFacilityName() {
        return facility.getTarget().getName();
    }

    public static Integer getFacilityId() {
        return facility.getId();
    }
}
