<%
	ui.decorateWith("kenyaemr", "standardPage")
%>

<div class="ke-page-content">
	${ ui.includeFragment("kenyadq", "patient/mergePatients", [ returnUrl: returnUrl ]) }
</div>