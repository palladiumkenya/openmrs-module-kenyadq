<%
	ui.decorateWith("kenyaemr", "standardPage", [ layout: "sidebar" ])
%>

<div class="ke-page-sidebar">
	${ ui.includeFragment("kenyaui", "widget/panelMenu", [
			heading: "Tasks",
			items: [
					[ iconProvider: "kenyadq", icon: "buttons/patient_merge.png", label: "Merge Duplicate Patients", href: ui.pageLink("kenyadq", "dataqual/mergePatients", [ returnUrl: ui.thisUrl() ]) ]
			]
	]) }
</div>

<div class="ke-page-content">
	${ ui.includeFragment("kenyadq", "reports", [ heading: "Common Reports", reports: commonReports ]) }

	<% programReports.each { programName, programReports -> %>
		${ ui.includeFragment("kenyadq", "reports", [ heading: programName, reports: programReports ]) }
	<% } %>
</div>