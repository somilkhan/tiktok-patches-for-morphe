package app.morphe.patches.shipsy.developeroptions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val disableDeveloperOptionsBlockPatch = bytecodePatch(
    name = "Disable Developer Options and mock-location filter (Shipsy 4.1.3)",
    description = "Disables the Developer Options blocking routine and forces the server-provided mock-location filter flag off.",
    default = true,
) {
    execute {
        CheckDeveloperOptionsSettingsFingerprint.method.addInstructions(
            0,
            """
                return-void
            """,
        )

        FilterMockLocationFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v0
            """,
        )
    }
}
