<%
	ui.decorateWith("kenyaemr", "standardPage", [ layout: "sidebar" ])

	def menuItems = [
			[ label: "Merge patient records", iconProvider: "kenyadq", icon: "buttons/patient_merge.png", href: ui.pageLink("kenyadq", "dataqual/findDuplicatePatients") ]
	]
%>

<div class="ke-page-sidebar">
	${ ui.includeFragment("kenyaui", "widget/panelMenu", [ heading: "Tasks", items: menuItems ]) }
</div>

<div class="ke-page-content">
	${ ui.includeFragment("kenyadq", "reports", [ heading: "Common Reports", reports: commonReports ]) }

	<% programReports.each { programName, programReports -> %>
		${ ui.includeFragment("kenyadq", "reports", [ heading: programName, reports: programReports ]) }
	<% } %>
</div>