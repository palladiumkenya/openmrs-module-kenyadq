package org.openmrs.module.kenyadq.page.controller.datamgr;

import org.openmrs.module.kenyadq.api.KenyaDqService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.FileDownload;

/**
 * Created by dev on 1/30/17.
 */
public class DownloadFlatAllPageController {

    public FileDownload controller(@SpringBean("kenyaDqService") KenyaDqService kenyaDqService) {
        String fileName = kenyaDqService.location() + "-" + kenyaDqService.timeStamp() + ".zip";
        FileDownload download = new FileDownload(fileName, "application/zip", kenyaDqService.downloadFlatAll());
        return download;
    }
}
