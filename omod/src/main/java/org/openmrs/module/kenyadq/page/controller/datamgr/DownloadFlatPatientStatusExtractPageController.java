package org.openmrs.module.kenyadq.page.controller.datamgr;

import org.openmrs.module.kenyadq.api.KenyaDqService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.FileDownload;

/**
 * Created by dev on 2/4/17.
 */
public class DownloadFlatPatientStatusExtractPageController {
    public FileDownload controller(@SpringBean("kenyaDqService") KenyaDqService kenyaDqService) {
        String fileName = "PatientStatusExtract" + "-" + kenyaDqService.location() + "-" + kenyaDqService.timeStamp() + ".csv";
        FileDownload download = new FileDownload(fileName, "text/csv", kenyaDqService.downloadFlatPatientStatusExtract());
        return download;
    }
}
