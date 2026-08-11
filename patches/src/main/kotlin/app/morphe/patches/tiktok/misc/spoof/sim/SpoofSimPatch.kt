/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/spoof/sim/SpoofSimPatch.kt
 */

package app.morphe.patches.tiktok.misc.spoof.sim

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.tiktok.misc.extension.sharedExtensionPatch
import app.morphe.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint
import app.morphe.patches.tiktok.misc.settings.settingsPatch
import app.morphe.util.findMutableMethodOf
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/morphe/extension/tiktok/spoof/sim/SpoofSimPatch;"
private const val TELEPHONY = "Landroid/telephony/TelephonyManager;"
private const val LOCALE = "Ljava/util/Locale;"
private const val TIME_ZONE = "Ljava/util/TimeZone;"

@Suppress("unused")
val simSpoofPatch = bytecodePatch(
    name = "SIM spoof",
    description = "Spoofs SIM country/operator, locale, and timezone signals for TikTok 46.2.3.",
    default = true,
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
    )

    compatibleWith(*AppCompatibilities.tiktok4623())

    execute {
        val stringReplacements = mapOf(
            "getSimCountryIso" to "getCountryIso",
            "getNetworkCountryIso" to "getCountryIso",
            "getSimOperator" to "getOperator",
            "getNetworkOperator" to "getOperator",
            "getNetworkOperatorForPhone" to "getOperator",
            "getSimOperatorName" to "getOperatorName",
            "getNetworkOperatorName" to "getOperatorName",
        )

        val patchesByMethod = linkedMapOf<Method, ArrayDeque<Pair<Int, String>>>()
        val carrierNamePatches = linkedMapOf<Method, ArrayDeque<Int>>()
        val carrierIdPatches = linkedMapOf<Method, ArrayDeque<Int>>()
        val localePatches = linkedMapOf<Method, ArrayDeque<Int>>()
        val timezonePatches = linkedMapOf<Method, ArrayDeque<Int>>()

        classDefForEach { classDef ->
            for (method in classDef.methods) {
                val implementation = method.implementation ?: continue
                implementation.instructions.forEachIndexed { index, instruction ->
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL &&
                        instruction.opcode != Opcode.INVOKE_VIRTUAL_RANGE &&
                        instruction.opcode != Opcode.INVOKE_STATIC
                    ) {
                        return@forEachIndexed
                    }

                    val methodReference = (instruction as ReferenceInstruction).reference as MethodReference

                    if (methodReference.definingClass == TELEPHONY) {
                        if (stringReplacements.containsKey(methodReference.name) &&
                            methodReference.returnType == "Ljava/lang/String;"
                        ) {
                            patchesByMethod.getOrPut(method) { ArrayDeque() }
                                .add(index to stringReplacements.getValue(methodReference.name))
                        }

                        if ((methodReference.name == "getSimCarrierIdName" ||
                                methodReference.name == "getSimSpecificCarrierIdName") &&
                            methodReference.returnType == "Ljava/lang/CharSequence;"
                        ) {
                            carrierNamePatches.getOrPut(method) { ArrayDeque() }.add(index)
                        }

                        if ((methodReference.name == "getSimCarrierId" ||
                                methodReference.name == "getSimSpecificCarrierId") &&
                            methodReference.returnType == "I"
                        ) {
                            carrierIdPatches.getOrPut(method) { ArrayDeque() }.add(index)
                        }
                    }

                    if (methodReference.definingClass == LOCALE &&
                        methodReference.name == "getDefault" &&
                        methodReference.returnType == LOCALE
                    ) {
                        localePatches.getOrPut(method) { ArrayDeque() }.add(index)
                    }

                    if (methodReference.definingClass == TIME_ZONE &&
                        methodReference.name == "getDefault" &&
                        methodReference.returnType == TIME_ZONE
                    ) {
                        timezonePatches.getOrPut(method) { ArrayDeque() }.add(index)
                    }
                }
            }
        }

        patchesByMethod.forEach { (method, patches) ->
            val mutableMethod = mutableClassDefBy(method.definingClass).findMutableMethodOf(method)
            while (patches.isNotEmpty()) {
                val (index, replacement) = patches.removeLast()
                val nextInstr = mutableMethod.getInstruction(index + 1)
                if (nextInstr !is OneRegisterInstruction) continue
                val resultRegister = nextInstr.registerA
                mutableMethod.addInstructions(index + 2, """
                    invoke-static {v$resultRegister}, $EXTENSION_CLASS_DESCRIPTOR->$replacement(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$resultRegister
                """)
            }
        }

        carrierNamePatches.forEach { (method, patches) ->
            val mutableMethod = mutableClassDefBy(method.definingClass).findMutableMethodOf(method)
            while (patches.isNotEmpty()) {
                val index = patches.removeLast()
                val nextInstr = mutableMethod.getInstruction(index + 1)
                if (nextInstr !is OneRegisterInstruction) continue
                val resultRegister = nextInstr.registerA
                mutableMethod.addInstructions(index + 2, """
                    invoke-static {v$resultRegister}, $EXTENSION_CLASS_DESCRIPTOR->getCarrierIdName(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                    move-result-object v$resultRegister
                """)
            }
        }

        carrierIdPatches.forEach { (method, patches) ->
            val mutableMethod = mutableClassDefBy(method.definingClass).findMutableMethodOf(method)
            while (patches.isNotEmpty()) {
                val index = patches.removeLast()
                val nextInstr = mutableMethod.getInstruction(index + 1)
                if (nextInstr !is OneRegisterInstruction) continue
                val resultRegister = nextInstr.registerA
                mutableMethod.addInstructions(index + 2, """
                    invoke-static {v$resultRegister}, $EXTENSION_CLASS_DESCRIPTOR->getCarrierId(I)I;
                    move-result v$resultRegister
                """)
            }
        }

        localePatches.forEach { (method, patches) ->
            val mutableMethod = mutableClassDefBy(method.definingClass).findMutableMethodOf(method)
            while (patches.isNotEmpty()) {
                val index = patches.removeLast()
                val nextInstr = mutableMethod.getInstruction(index + 1)
                if (nextInstr !is OneRegisterInstruction) continue
                val resultRegister = nextInstr.registerA
                mutableMethod.addInstructions(index + 2, """
                    invoke-static {v$resultRegister}, $EXTENSION_CLASS_DESCRIPTOR->getDefaultLocale(Ljava/util/Locale;)Ljava/util/Locale;
                    move-result-object v$resultRegister
                """)
            }
        }

        timezonePatches.forEach { (method, patches) ->
            val mutableMethod = mutableClassDefBy(method.definingClass).findMutableMethodOf(method)
            while (patches.isNotEmpty()) {
                val index = patches.removeLast()
                val nextInstr = mutableMethod.getInstruction(index + 1)
                if (nextInstr !is OneRegisterInstruction) continue
                val resultRegister = nextInstr.registerA
                mutableMethod.addInstructions(index + 2, """
                    invoke-static {v$resultRegister}, $EXTENSION_CLASS_DESCRIPTOR->getDefaultTimeZone(Ljava/util/TimeZone;)Ljava/util/TimeZone;
                    move-result-object v$resultRegister
                """)
            }
        }

        SettingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableSimSpoof()V",
        )
    }
}
