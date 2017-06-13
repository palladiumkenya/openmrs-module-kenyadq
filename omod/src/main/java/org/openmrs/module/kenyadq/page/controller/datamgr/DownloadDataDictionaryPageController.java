package org.openmrs.module.kenyadq.page.controller.datamgr;

import org.openmrs.module.kenyadq.api.KenyaDqService;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.FileDownload;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by gitahi on 12/05/15.
 */
public class DownloadDataDictionaryPageController {

	public FileDownload controller(@SpringBean("kenyaDqService") KenyaDqService kenyaDqService) {
		Date now = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HHmmss");
		String dateString = format.format(now);
		String fileName = "KenyaEMR Data Dictionary " + dateString + ".csv";
		FileDownload download = new FileDownload(fileName, "text/csv", kenyaDqService.downloadDataDictionary());
		return download;
	}
}
