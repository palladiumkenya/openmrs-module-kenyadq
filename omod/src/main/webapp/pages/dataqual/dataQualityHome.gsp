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
	<!-- Not sure yet what belongs here... -->
</div>