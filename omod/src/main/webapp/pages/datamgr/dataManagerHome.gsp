<%
    ui.decorateWith("kenyaemr", "standardPage", [layout: "sidebar"])

    def menuItems = [
            [label: "Merge patient records", iconProvider: "kenyadq", icon: "buttons/patient_merge.png", href: ui.pageLink("kenyadq", "datamgr/findDuplicatePatients")],
            [label: "Data Analysis file", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadAnalysisFile")],
            [label: "Data Dictionary", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadDataDictionary")],
            [label: "DW Patient Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadFlatPatientExtract")],
            [label: "DW Patient Status Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadFlatPatientStatusExtract")],
            [label: "DW Patient Visit Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadFlatPatientVisitExtract")],
            [label: "DW Patient Pharmacy Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadFlatPatientPharmacyExtract")],
            [label: "DW Patient Laboratory Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadFlatPatientLabExtract")],
            [label: "DW Patient WAB/WHO/CD4 Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadFlatPatientWABWHOCD4Extract")],
            [label: "DW ART Patient Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadFlatARTPatientExtract")],
            [label: "DW All Extracts ", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadFlatAll")]
    ]
%>

<div class="ke-page-sidebar">
    ${ui.includeFragment("kenyaui", "widget/panelMenu", [heading: "Tasks", items: menuItems])}
</div>

<div class="ke-page-content">
    ${ui.includeFragment("kenyaemr", "system/databaseSummary")}
</div>