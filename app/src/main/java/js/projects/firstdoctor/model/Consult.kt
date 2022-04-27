package js.projects.firstdoctor.model

data class Consult(
    var chiefComplaint: String? = "recComp",
    var medicalHistory: String? = "recHistory",
    var problemPlace: String? = null
)
