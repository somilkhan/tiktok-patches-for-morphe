package app.morphe.patches.shipsy.developeroptions

import app.morphe.patcher.Fingerprint

internal object CheckDeveloperOptionsSettingsFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type == "Lcom/shipsy/dtdc/riderapp/security/PlayIntegrityManager;" &&
            method.name == "checkDeveloperOptionsSettings"
    },
)
