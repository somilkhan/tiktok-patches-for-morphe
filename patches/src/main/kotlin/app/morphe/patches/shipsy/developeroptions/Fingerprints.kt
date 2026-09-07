package app.morphe.patches.shipsy.developeroptions

import app.morphe.patcher.Fingerprint

internal object CheckDeveloperOptionsSettingsFingerprint : Fingerprint(
    definingClass = "Lcom/shipsy/dtdc/riderapp/security/PlayIntegrityManager;",
    name = "checkDeveloperOptionsSettings",
    returnType = "Z",
)
