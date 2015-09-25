package org.openmrs.module.kenyadq.page.controller.datamgr;

import org.openmrs.module.kenyadq.api.KenyaDqService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.FileDownload;

/**
 * Created by gitahi on 12/05/15.
 */
public class DownloadAllPageController {

    public FileDownload controller(@SpringBean("kenyaDqService") KenyaDqService kenyaDqService) {
        String fileName = kenyaDqService.location() + "-" + kenyaDqService.timeStamp() + ".zip";
        FileDownload download = new FileDownload(fileName, "application/zip", kenyaDqService.downloadAll());
        return download;
    }
}
