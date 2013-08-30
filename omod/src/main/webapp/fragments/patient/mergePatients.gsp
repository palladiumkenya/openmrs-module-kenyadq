<%
	ui.decorateWith("kenyaui", "panel", [ heading: "Merge Patients", frameOnly: true ])
%>

<form id="merge-patients-form" method="post" action="${ ui.actionLink("kenyadq", "mergePatients", "mergePatients") }">

	<div class="ke-panel-controls">
		${ ui.includeFragment("kenyaui", "widget/buttonlet", [
				type: "switch", label: "Switch", onClick: "switchPatients()"
		]) }
	</div>

	<div class="ke-panel-content">

		<div class="ke-form-globalerrors" style="display: none"></div>

		<table style="width: 100%">
			<tr>
				<td class="ke-field-label" style="width: 50%; text-align: center">Patient 1 (Preferred)</td>
				<td class="ke-field-label" style="width: 50%; text-align: center">Patient 2</td>
			</tr>
			<tr>
				<td>${ ui.includeFragment("kenyaui", "widget/field", [ id: "patient1-select", object: command, property: "patient1" ]) }</td>
				<td>${ ui.includeFragment("kenyaui", "widget/field", [ id: "patient2-select", object: command, property: "patient2" ]) }</td>
			</tr>
			<tr>
				<td style="vertical-align: top">
					<fieldset>
						<legend>Information</legend>
						<div id="patient1-infopoints" class="patient1-item"></div>
					</fieldset>
				</td>
				<td style="vertical-align: top">
					<fieldset>
						<legend>Information</legend>
						<div id="patient2-infopoints" class="patient1-item"></div>
					</fieldset>
				</td>
			</tr>
			<tr>
				<td style="vertical-align: top">
					<fieldset>
						<legend>Identifiers</legend>
						<div id="patient1-identifiers" class="patient1-item"></div>
					</fieldset>
				</td>
				<td style="vertical-align: top">
					<fieldset>
						<legend>Identifiers</legend>
						<div id="patient2-identifiers" class="patient1-item"></div>
					</fieldset>
				</td>
			</tr>
			<tr>
				<td style="vertical-align: top">
					<fieldset>
						<legend>Encounters</legend>
						<div id="patient1-encounters" class="patient2-item"></div>
					</fieldset>
				</td>
				<td style="vertical-align: top">
					<fieldset>
						<legend>Encounters</legend>
						<div id="patient2-encounters" class="patient2-item"></div>
					</fieldset>
				</td>
			</tr>
		</table>

	</div>

	<div class="ke-panel-controls">
		<input class="ke-button" type="submit" value="Merge" />
		<input class="ke-button cancel-button" type="button" value="Cancel"/>
	</div>

</form>

<script type="text/javascript">
	jq(function() {
		jq('#merge-patients-form .cancel-button').click(function() {
			location.href = '${ returnUrl }';
		});

		jq('#patient1-select').on('change', function() {
			updatePatientSummary(jq(this).val(), '1');
		});
		jq('#patient2-select').on('change', function() {
			updatePatientSummary(jq(this).val(), '2');
		});

		kenyaui.setupAjaxPost('merge-patients-form', {
			onSuccess: function(data) {
				location.href = '${ returnUrl }';
			}
		});
	});

	/**
	 * Switches the patients so the other one is preferred
	 */
	function switchPatients() {
		var patient1Id = jq('#patient1-select').val();
		var patient2Id = jq('#patient2-select').val();

		kenyaui.setSearchFieldValue('patient1-select', patient2Id);
		kenyaui.setSearchFieldValue('patient2-select', patient1Id);

		swapContent('#patient1-infopoints', '#patient2-infopoints');
		swapContent('#patient1-identifiers', '#patient2-identifiers');
		swapContent('#patient1-encounters', '#patient2-encounters');
	}

	/**
	 * Updates a patient summary
	 * @param patientId the patient side
	 * @param position '1' or '2'
	 */
	function updatePatientSummary(patientId, position) {
		jq('.patient' + position + '-item').html('').addClass('ke-loading');

		ui.getFragmentActionAsJson('kenyadq', 'patient/mergePatients', 'patientSummary', { patientId : patientId }, function (patient) {
			showDataPoints('#patient' + position + '-infopoints', patient.infopoints);
			showDataPoints('#patient' + position + '-identifiers', patient.identifiers);

			jq('.patient' + position + '-item').removeClass('ke-loading');
		});
	}

	function showDataPoints(selector, dataPoints) {
		var html = '';
		for (var i = 0; i < dataPoints.length; ++i) {
			html += createDataPoint(dataPoints[i].label, dataPoints[i].value);
		}
		jq(selector).html(html);
	}

	function createDataPoint(label, value) {
		return '<div class="ke-datapoint"><span class="ke-label">' + label + '</span>: <span class="ke-value">' + value + '</span></div>'
	}

	function swapContent(selector1, selector2) {
		var html1 = jq(selector1).html();
		var html2 = jq(selector2).html();
		jq(selector1).html(html2);
		jq(selector2).html(html1);
	}
</script>