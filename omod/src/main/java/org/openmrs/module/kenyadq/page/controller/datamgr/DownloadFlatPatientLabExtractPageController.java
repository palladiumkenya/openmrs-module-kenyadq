package org.openmrs.module.kenyadq.page.controller.datamgr;

import org.openmrs.module.kenyadq.api.KenyaDqService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.FileDownload;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by dev on 1/22/17.
 */
public class DownloadFlatPatientLabExtractPageController {

    public FileDownload controller(@SpringBean("kenyaDqService") KenyaDqService kenyaDqService) {
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HHmmss");
        String dateString = format.format(now);
        String fileName = "KenyaEMR Flat Lab Extract File " + dateString + ".csv";
        FileDownload download = new FileDownload(fileName, "text/csv", kenyaDqService.downloadFlatPatientLabExtract());
        return download;
    }
}
