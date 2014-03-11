<%
	ui.decorateWith("kenyaemr", "standardPage", [ layout: "sidebar" ])
%>

<div class="ke-page-content">
	${ ui.includeFragment("kenyadq", "reports", [ heading: "Common Reports", reports: commonReports ]) }

	<% programReports.each { programName, programReports -> %>
		${ ui.includeFragment("kenyadq", "reports", [ heading: programName, reports: programReports ]) }
	<% } %>
</div>