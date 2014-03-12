<%
	ui.decorateWith("kenyaemr", "standardPage", [ layout: "sidebar" ])

	ui.includeJavascript("kenyadq", "controllers/merge.js")

	def menuItems = [
			[ label: "Select patients manually", iconProvider: "kenyadq", icon: "buttons/patient_merge.png", href: ui.pageLink("kenyadq", "datamgr/mergePatients", [ returnUrl: ui.thisUrl() ]) ],
			[ label: "Back to home", iconProvider: "kenyaui", icon: "buttons/back.png", label: "Back to home", href: ui.pageLink("kenyadq", "datamgr/dataManagerHome") ]
	]
%>

<script type="text/javascript">
	jQuery(function() {
		jQuery('#merge_selected').click(function() {
			var patientIds = [];
			jQuery('input.selected_patient:checked').each(function() {
				patientIds.push(jQuery(this).val());
			});

			if (patientIds.length == 2) {
				ui.navigate('kenyadq', 'datamgr/mergePatients', { patient1: patientIds[0], patient2: patientIds[1], returnUrl: location.href });
			}
			else {
				kenyaui.notifyError('Please select exactly 2 patients to merge');
			}
		});
	});
</script>

<div class="ke-page-sidebar">
	${ ui.includeFragment("kenyaui", "widget/panelMenu", [ heading: "Tasks", items: menuItems ]) }
</div>

<div class="ke-page-content">
	<div class="ke-panel-frame" ng-controller="DuplicatePatients" ng-init="init()">
		<div class="ke-panel-heading">Possible Duplicate Patients</div>
		<div class="ke-panel-controls">
			<table style="width: 100%">
				<tr>
					<td style="text-align: left">Match by:
						<input ng-model="byIdentifier" ng-change="refresh()" ng-disabled="loading" type="checkbox" /> Identifier
						<input ng-model="byFamilyName" ng-change="refresh()" ng-disabled="loading" type="checkbox" /> Family name
						<input ng-model="byGivenName" ng-change="refresh()" ng-disabled="loading" type="checkbox" /> Given name
						<input ng-model="byGender" ng-change="refresh()" ng-disabled="loading" type="checkbox" /> Gender
						<input ng-model="byBirthdate" ng-change="refresh()" ng-disabled="loading" type="checkbox" /> Birthdate
					</td>
					<td style="text-align: right">
						<button type="button" id="merge_selected"><img src="${ ui.resourceLink("kenyaui", "images/glyphs/ok.png") }" /> Merge selected</button>
					</td>
				</tr>
			</table>
		</div>
		<div class="ke-panel-content">
			<div ng-repeat="patient in results" class="ke-stack-item">
				<input type="checkbox" name="patientId" value="{{ patient.id }}" class="selected_patient" />
				${ ui.includeFragment("kenyaemr", "patient/result.mini") }
			</div>
			<div ng-if="loading" style="text-align: center; padding-top: 5px">
				<img src="${ ui.resourceLink("kenyaui", "images/loading.gif") }" />
			</div>
		</div>
	</div>
</div>