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

/**
 * Controller for duplicate patients
 */
kenyaemrApp.controller('DuplicatePatients', ['$scope', '$http', function($scope, $http) {

	$scope.byIdentifier = false;
	$scope.byFamilyName = true;
	$scope.byGivenName = true;
	$scope.byGender = true;
	$scope.byBirthdate = false;
	$scope.loading = false;
	$scope.results = [];

	/**
	 * Initializes the controller
	 */
	$scope.init = function() {
		$scope.refresh();
	};

	/**
	 * Refreshes the list
	 */
	$scope.refresh = function() {
		$scope.loading = true;
		$scope.results = [];

		var params = {
			byIdentifier: $scope.byIdentifier,
			byFamilyName: $scope.byFamilyName,
			byGivenName: $scope.byGivenName,
			byGender: $scope.byGender,
			byBirthdate: $scope.byBirthdate
		};

		$http.get(ui.fragmentActionLink('kenyadq', 'patient/mergeUtils', 'getDuplicatePatients', params))
			.success(function(data) {
				$scope.results = data;
				$scope.loading = false;
			})
			.error(function(data) {
				kenyaui.notifyError(data.globalErrors.join('<br/>'));
				$scope.loading = false;
			});
	};
}]);