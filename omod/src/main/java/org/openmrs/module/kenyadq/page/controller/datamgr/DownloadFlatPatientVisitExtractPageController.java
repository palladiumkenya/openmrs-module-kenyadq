package org.openmrs.module.kenyadq.page.controller.datamgr;

import org.openmrs.module.kenyadq.api.KenyaDqService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.FileDownload;

/**
 * Created by dev on 1/22/17.
 */
public class DownloadFlatPatientVisitExtractPageController {

    public FileDownload controller(@SpringBean("kenyaDqService") KenyaDqService kenyaDqService) {
        String fileName = "PatientVisitExtract" + "-" + kenyaDqService.location() + "-" + kenyaDqService.timeStamp() + ".csv";
        FileDownload download = new FileDownload(fileName, "text/csv", kenyaDqService.downloadFlatPatientVisitExtract());
        return download;
    }
}
