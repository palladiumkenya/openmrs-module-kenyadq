<%
    ui.decorateWith("kenyaemr", "standardPage", [ layout: "sidebar" ])

    def menuItems = [
            [ label: "Merge patient records", iconProvider: "kenyadq", icon: "buttons/patient_merge.png", href: ui.pageLink("kenyadq", "datamgr/findDuplicatePatients") ],
                    [ label: "Download analysis file", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadAnalysisFile") ],
            [ label: "Download data dictionary", iconProvider: "kenyadq", icon: "buttons/download_analysis_file.png", href: ui.pageLink("kenyadq", "datamgr/downloadDataDictionary") ]
    ]
%>

<div class="ke-page-sidebar">
    ${ ui.includeFragment("kenyaui", "widget/panelMenu", [ heading: "Tasks", items: menuItems ]) }
</div>

<div class="ke-page-content">
    ${ ui.includeFragment("kenyaemr", "system/databaseSummary") }
</div>