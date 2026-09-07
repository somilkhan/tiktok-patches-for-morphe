package app.morphe.patches.shipsy.developeroptions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val disableDeveloperOptionsBlockPatch = bytecodePatch(
    name = "Disable Developer Options block (Shipsy 4.1.3)",
    description = "Disables the Developer Options blocking routine in DominoMainLandingActivity.",
    default = true,
) {
    execute {
        CheckDeveloperOptionsSettingsFingerprint.method.addInstructions(
            0,
            """
                return-void
            """,
        )
    }
}
