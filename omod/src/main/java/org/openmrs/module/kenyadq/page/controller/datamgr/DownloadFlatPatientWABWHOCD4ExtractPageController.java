package org.openmrs.module.kenyadq.page.controller.datamgr;

import org.openmrs.module.kenyadq.api.KenyaDqService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.FileDownload;

/**
 * Created by dev on 2/4/17.
 */
public class DownloadFlatPatientWABWHOCD4ExtractPageController {
    public FileDownload controller(@SpringBean("kenyaDqService") KenyaDqService kenyaDqService) {
        String fileName = "PatientWABWHOCD4Extract" + "-" + kenyaDqService.location() + "-" + kenyaDqService.timeStamp() + ".csv";
        FileDownload download = new FileDownload(fileName, "text/csv", kenyaDqService.downloadFlatPatientWABWHOCD4Extract());
        return download;
    }
}
