package app.morphe.patches.tiktok.misc.spoof.sim

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.tiktok.misc.extension.sharedExtensionPatch
import app.morphe.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint
import app.morphe.patches.tiktok.misc.settings.settingsPatch
import app.morphe.util.findMutableMethodOf
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXT = "Lapp/morphe/extension/tiktok/spoof/sim/SpoofSimPatch;"
private const val TELEPHONY = "Landroid/telephony/TelephonyManager;"
private const val REGION_RESOLVER = "LX/C35590hVz;"
private const val REGION_CONFIG = "LX/C379311yQ;"
private const val WRAPPER = "LX/C34171AbR;"
private const val PERSISTED = "LX/C215817xU;"
private const val REGION_SERVICE = "Lcom/ss/android/ugc/aweme/ecommerce/dependency/location/LocationDependencyService;"
private const val REGION_SOURCE = "LX/1C4Y;"
private const val LOCALE = "Ljava/util/Locale;"
private const val TIME_ZONE = "Ljava/util/TimeZone;"

@Suppress("unused")
val simSpoofPatch = bytecodePatch(
    name = "SIM spoof",
    description = "Jaggu-style region source, gate, persisted locale, and telephony spoof for TikTok 46.2.3.",
    default = true,
) {
    dependsOn(sharedExtensionPatch, settingsPatch)
    compatibleWith(*AppCompatibilities.tiktok4623())
    execute {
        classDefForEach { classDef ->
            if (classDef.type != PERSISTED) return@classDefForEach
            classDef.methods.filter { it.name == "LIZIZ" && it.returnType == "V" && it.parameterTypes.size == 3 && it.parameterTypes[2] == LOCALE }.forEach { method ->
                mutableClassDefBy(classDef.type).findMutableMethodOf(method).addInstructions(0, "invoke-static {p2}, $EXT->getLocale(Ljava/util/Locale;)Ljava/util/Locale;\nmove-result-object p2")
            }
        }
        classDefForEach { classDef ->
            if (classDef.type != REGION_SERVICE) return@classDefForEach
            classDef.methods.filter { it.name == "isInTikTokRegion" && it.returnType == "Z" && it.parameterTypes.isEmpty() }.forEach { method ->
                mutableClassDefBy(classDef.type).findMutableMethodOf(method).addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            }
        }
        val regionSourceMap = mapOf("LIZ" to "getRegion", "LIZIZ" to "getRegion", "LJ" to "getCountryIso", "LJFF" to "getCountryIso")
        classDefForEach { classDef ->
            if (classDef.type != REGION_SOURCE) return@classDefForEach
            classDef.methods.forEach { method ->
                val replacement = regionSourceMap[method.name] ?: return@forEach
                if (method.parameterTypes.isNotEmpty() || method.returnType != "Ljava/lang/String;") return@forEach
                val mutable = mutableClassDefBy(classDef.type).findMutableMethodOf(method)
                method.implementation?.instructions?.mapIndexedNotNull { i, ins -> if (ins.opcode == Opcode.RETURN_OBJECT) i else null }?.asReversed()?.forEach { index ->
                    val reg = (mutable.implementation!!.instructions[index] as OneRegisterInstruction).registerA
                    mutable.addInstructions(index, "invoke-static {v$reg}, $EXT->$replacement(Ljava/lang/String;)Ljava/lang/String;\nmove-result-object v$reg")
                }
            }
        }
        classDefForEach { classDef ->
            if (classDef.type != REGION_SOURCE) return@classDefForEach
            classDef.methods.filter { it.name == "LJIIIIZZ" && it.returnType == "Z" && it.parameterTypes.isEmpty() }.forEach { method ->
                val mutable = mutableClassDefBy(classDef.type).findMutableMethodOf(method)
                method.implementation?.instructions?.mapIndexedNotNull { i, ins -> if (ins.opcode == Opcode.RETURN) i else null }?.asReversed()?.forEach { index ->
                    val reg = (mutable.implementation!!.instructions[index] as OneRegisterInstruction).registerA
                    mutable.addInstructions(index, "invoke-static {v$reg}, $EXT->isInTikTokRegion(Z)Z\nmove-result v$reg")
                }
            }
        }
        classDefForEach { classDef ->
            if (classDef.type != REGION_RESOLVER) return@classDefForEach
            classDef.methods.filter { it.name == "LIZ" && it.returnType == "Ljava/lang/String;" && it.parameterTypes.isEmpty() }.forEach { method ->
                val mutable = mutableClassDefBy(classDef.type).findMutableMethodOf(method)
                method.implementation?.instructions?.mapIndexedNotNull { i, ins -> if (ins.opcode == Opcode.RETURN_OBJECT) i else null }?.asReversed()?.forEach { index ->
                    val reg = (mutable.implementation!!.instructions[index] as OneRegisterInstruction).registerA
                    mutable.addInstructions(index, "invoke-static {v$reg}, $EXT->getRegion(Ljava/lang/String;)Ljava/lang/String;\nmove-result-object v$reg")
                }
            }
        }
        classDefForEach { classDef ->
            if (classDef.type != REGION_CONFIG) return@classDefForEach
            classDef.methods.filter { it.returnType == "Ljava/util/Map;" && it.parameterTypes.isEmpty() }.forEach { method ->
                val mutable = mutableClassDefBy(classDef.type).findMutableMethodOf(method)
                method.implementation?.instructions?.mapIndexedNotNull { i, ins -> if (ins.opcode == Opcode.RETURN_OBJECT) i else null }?.asReversed()?.forEach { index ->
                    val reg = (mutable.implementation!!.instructions[index] as OneRegisterInstruction).registerA
                    mutable.addInstructions(index, "invoke-static {v$reg}, $EXT->spoofRegionMap(Ljava/util/Map;)Ljava/util/Map;\nmove-result-object v$reg")
                }
            }
        }
        val wrapperMap = mapOf("LIZJ" to "getCountryIso", "LJIIIIZZ" to "getCountryIso", "LJ" to "getOperator", "LJIIJ" to "getOperator", "LJI" to "getOperatorName", "LJIIL" to "getOperatorName")
        classDefForEach { classDef ->
            if (classDef.type != WRAPPER) return@classDefForEach
            classDef.methods.forEach { method ->
                val replacement = wrapperMap[method.name] ?: return@forEach
                if (method.returnType != "Ljava/lang/String;" || method.parameterTypes.isNotEmpty()) return@forEach
                val mutable = mutableClassDefBy(classDef.type).findMutableMethodOf(method)
                method.implementation?.instructions?.mapIndexedNotNull { i, ins -> if (ins.opcode == Opcode.RETURN_OBJECT) i else null }?.asReversed()?.forEach { index ->
                    val reg = (mutable.implementation!!.instructions[index] as OneRegisterInstruction).registerA
                    mutable.addInstructions(index, "invoke-static {v$reg}, $EXT->$replacement(Ljava/lang/String;)Ljava/lang/String;\nmove-result-object v$reg")
                }
            }
        }
        // Keep the verified, buildable telephony and locale hooks. The native Jaggu network
        // implementation is deliberately not approximated with a process-wide DNS/DoH hook.
        classDefForEach { classDef ->
            if (classDef.type != TELEPHONY) return@classDefForEach
        }
        SettingsStatusLoadFingerprint.method.addInstruction(0, "invoke-static {}, Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableSimSpoof()V")
    }
}
