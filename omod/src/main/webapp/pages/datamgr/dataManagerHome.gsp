<%
    ui.decorateWith("kenyaemr", "standardPage", [layout: "sidebar"])

    def menuItems = [
            [label: "Merge patient records", iconProvider: "kenyadq", icon: "buttons/patient_merge.png", href: ui.pageLink("kenyadq", "datamgr/findDuplicatePatients")],
            [label: "Data Analysis file", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadAnalysisFile")],
            [label: "Data Dictionary", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadDataDictionary")],
            [label: "DW Patient Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadPatientExtract")],
            [label: "DW Patient Status Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadPatientStatusExtract")],
            [label: "DW Patient Visit Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadPatientVisitExtract")],
            [label: "DW Patient Pharmacy Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadPatientPharmacyExtract")],
            [label: "DW Patient Laboratory Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadPatientLaboratoryExtract")],
            [label: "DW Patient WAB/WHO/CD4 Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadPatientWABWHOCD4Extract")],
            [label: "DW ART Patient Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadARTPatientExtract")],
            [label: "DW All (Except Visit)", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadAll")],
            [label: "DW Flat Patient Laboratory Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadFlatPatientLabExtract")],
            [label: "DW Flat Patient Visit Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadFlatPatientVisitExtract")],
            [label: "DW Flat Patient Pharmacy Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadFlatPatientPharmacyExtract")],
            [label: "DW Flat Patient Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadFlatPatientExtract")],
            [label: "DW Flat ART Patient Extract", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadFlatARTPatientExtract")],
            [label: "DW Flat All ", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadFlatAll")]
    ]
%>

<div class="ke-page-sidebar">
    ${ui.includeFragment("kenyaui", "widget/panelMenu", [heading: "Tasks", items: menuItems])}
</div>

<div class="ke-page-content">
    ${ui.includeFragment("kenyaemr", "system/databaseSummary")}
</div>