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
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/morphe/extension/tiktok/spoof/sim/SpoofSimPatch;"
private const val TELEPHONY = "Landroid/telephony/TelephonyManager;"
private const val LOCALE = "Ljava/util/Locale;"
private const val TIME_ZONE = "Ljava/util/TimeZone;"
private const val MAP = "Ljava/util/Map;"
private const val HASH_MAP = "Ljava/util/HashMap;"
private const val LINKED_HASH_MAP = "Ljava/util/LinkedHashMap;"
private const val JSON_OBJECT = "Lorg/json/JSONObject;"
private const val BUNDLE = "Landroid/os/Bundle;"

@Suppress("unused")
val simSpoofPatch = bytecodePatch(
    name = "SIM spoof",
    description = "Spoofs SIM, locale, timezone, and TikTok request region signals using the selected region preset.",
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
        val requestPatches = linkedMapOf<Method, ArrayDeque<Pair<Int, Pair<Int, Int>>>>()

        classDefForEach { classDef ->
            for (method in classDef.methods) {
                val implementation = method.implementation ?: continue
                implementation.instructions.forEachIndexed { index, instruction ->
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL &&
                        instruction.opcode != Opcode.INVOKE_VIRTUAL_RANGE &&
                        instruction.opcode != Opcode.INVOKE_INTERFACE &&
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

                    if (instruction.opcode == Opcode.INVOKE_INTERFACE ||
                        instruction.opcode == Opcode.INVOKE_VIRTUAL
                    ) {
                        if (instruction !is FiveRegisterInstruction) return@forEachIndexed

                        val definingClass = methodReference.definingClass
                        val name = methodReference.name
                        val parameterTypes = methodReference.parameterTypes

                        val isMapPut =
                            (definingClass == MAP || definingClass == HASH_MAP || definingClass == LINKED_HASH_MAP) &&
                                    name == "put" && parameterTypes.size == 2 &&
                                    parameterTypes[0] == "Ljava/lang/Object;" &&
                                    parameterTypes[1] == "Ljava/lang/Object;"

                        val isJsonPut =
                            definingClass == JSON_OBJECT && name == "put" &&
                                    parameterTypes.size == 2 &&
                                    parameterTypes[0] == "Ljava/lang/String;" &&
                                    parameterTypes[1] == "Ljava/lang/Object;"

                        val isBundlePutString =
                            definingClass == BUNDLE && name == "putString" &&
                                    parameterTypes.size == 2 &&
                                    parameterTypes[0] == "Ljava/lang/String;" &&
                                    parameterTypes[1] == "Ljava/lang/String;"

                        if (isMapPut || isJsonPut || isBundlePutString) {
                            requestPatches.getOrPut(method) { ArrayDeque() }
                                .add(index to (instruction.registerD to instruction.registerE))
                        }
                    }
                }
            }
        }

        patchesByMethod.forEach { (method, patches) ->
            val mutableMethod = mutableClassDefBy(method.definingClass).findMutableMethodOf(method)
            while (patches.isNotEmpty()) {
                val (index, replacement) = patches.removeLast()
                val resultRegister = mutableMethod.getInstruction<OneRegisterInstruction>(index + 1).registerA
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
                val resultRegister = mutableMethod.getInstruction<OneRegisterInstruction>(index + 1).registerA
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
                val resultRegister = mutableMethod.getInstruction<OneRegisterInstruction>(index + 1).registerA
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
                val resultRegister = mutableMethod.getInstruction<OneRegisterInstruction>(index + 1).registerA
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
                val resultRegister = mutableMethod.getInstruction<OneRegisterInstruction>(index + 1).registerA
                mutableMethod.addInstructions(index + 2, """
                    invoke-static {v$resultRegister}, $EXTENSION_CLASS_DESCRIPTOR->getDefaultTimeZone(Ljava/util/TimeZone;)Ljava/util/TimeZone;
                    move-result-object v$resultRegister
                """)
            }
        }

        requestPatches.forEach { (method, patches) ->
            val mutableMethod = mutableClassDefBy(method.definingClass).findMutableMethodOf(method)
            while (patches.isNotEmpty()) {
                val (index, registers) = patches.removeLast()
                val keyRegister = registers.first
                val valueRegister = registers.second
                mutableMethod.addInstructions(index, """
                    invoke-static {v$keyRegister, v$valueRegister}, $EXTENSION_CLASS_DESCRIPTOR->spoofRequestParam(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v$valueRegister
                """)
            }
        }

        SettingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableSimSpoof()V",
        )
    }
}
