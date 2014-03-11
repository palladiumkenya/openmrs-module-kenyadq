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

package org.openmrs.module.kenyadq.page.controller.datamgr;

import org.openmrs.Patient;
import org.openmrs.module.kenyadq.DqConstants;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Merge patients page
 */
@AppPage(DqConstants.APP_DATAQUALITY)
public class MergePatientsPageController {

	public void controller(@RequestParam(value = "patient1", required = false) Patient patient1,
						   @RequestParam(value = "patient2", required = false) Patient patient2,
						   @RequestParam("returnUrl") String returnUrl,
						   PageModel model) {

		model.addAttribute("patient1", patient1);
		model.addAttribute("patient2", patient2);
		model.addAttribute("returnUrl", returnUrl);
	}
}