<%
	ui.decorateWith("kenyaemr", "standardPage")
%>

<div class="ke-page-content">
	${ ui.includeFragment("kenyadq", "patient/mergePatients", [ patient1: patient1, patient2: patient2, returnUrl: returnUrl ]) }
</div>