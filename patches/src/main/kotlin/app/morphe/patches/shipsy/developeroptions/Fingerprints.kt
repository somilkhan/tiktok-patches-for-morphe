package app.morphe.patches.shipsy.developeroptions

import app.morphe.patcher.fingerprint.methodFingerprint

internal object Fingerprints {
    val checkDeveloperOptionsSettings = methodFingerprint {
        returnType = "Z"
        custom { method, classDef ->
            method.name == "checkDeveloperOptionsSettings" &&
                classDef.type == "Lcom/shipsy/dtdc/riderapp/security/PlayIntegrityManager;"
        }
    }
}
