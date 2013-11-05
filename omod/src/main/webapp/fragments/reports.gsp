<%
	ui.decorateWith("kenyaui", "panel", [ heading: config.heading ])

	def onReportClick = { report ->
		def opts = [ appId: currentApp.id, reportUuid: report.definitionUuid, returnUrl: ui.thisUrl() ]
		"""location.href = '${ ui.pageLink('kenyaemr', 'report', opts) }';"""
	}
%>

${ ui.includeFragment("kenyaemr", "widget/reportStack", [ reports: config.reports, onReportClick: onReportClick ]) }