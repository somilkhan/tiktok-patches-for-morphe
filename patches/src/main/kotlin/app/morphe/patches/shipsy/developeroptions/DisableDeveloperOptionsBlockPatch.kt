package app.morphe.patches.shipsy.developeroptions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val disableDeveloperOptionsBlockPatch = bytecodePatch(
    name = "Disable Developer Options block (Shipsy 4.1.3)",
    description = "Makes PlayIntegrityManager.checkDeveloperOptionsSettings report Developer Options as disabled.",
    default = true,
) {
    execute {
        CheckDeveloperOptionsSettingsFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )
    }
}
