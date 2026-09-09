package app.morphe.patches.shipsy.developeroptions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val disableDeveloperOptionsBlockPatch = bytecodePatch(
    name = "Disable Developer Options and fake-GPS enforcement gates (Shipsy 4.1.3)",
    description = "Disables the Developer Options UI guard, forces the mock-location filter flag off, and forces both fake-GPS configuration gates off for the patched test build.",
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

        ShouldEnableFakeGpsDetectionFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )

        ShouldCheckoutForFakeGpsFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )
    }
}
