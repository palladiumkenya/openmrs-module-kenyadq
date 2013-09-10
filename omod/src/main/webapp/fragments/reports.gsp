<%
	ui.decorateWith("kenyaui", "panel", [ heading: config.heading ])

	def onReportClick = { report ->
		def opts = [ appId: currentApp.id, reportId: report.id, returnUrl: ui.thisUrl() ]
		"""location.href = '${ ui.pageLink('kenyaemr', 'runReport', opts) }';"""
	}
%>

${ ui.includeFragment("kenyaemr", "widget/reportStack", [ reports: config.reports, onReportClick: onReportClick ]) }