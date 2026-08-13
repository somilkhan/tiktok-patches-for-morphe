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
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

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
private const val INET_ADDRESS = "Ljava/net/InetAddress;"

private fun singleInvokeArgumentRegister(instruction: ReferenceInstruction): Int? = when (instruction) {
    is FiveRegisterInstruction -> instruction.registerC
    is RegisterRangeInstruction -> instruction.startRegister
    else -> null
}

@Suppress("unused")
val simSpoofPatch = bytecodePatch(
    name = "SIM spoof",
    description = "Jaggu-style region source, gate, persisted locale, telephony spoof, and app-scoped DNS bypass for TikTok 46.2.3.",
    default = true,
) {
    dependsOn(sharedExtensionPatch, settingsPatch)
    compatibleWith(*AppCompatibilities.tiktok4623())

    execute {
        classDefForEach { classDef ->
            if (classDef.type != PERSISTED) return@classDefForEach
            classDef.methods.filter {
                it.name == "LIZIZ" && it.returnType == "V" && it.parameterTypes.size == 3 &&
                    it.parameterTypes[0] == "Landroid/content/Context;" &&
                    it.parameterTypes[1] == "Ljava/lang/String;" &&
                    it.parameterTypes[2] == LOCALE
            }.forEach { method ->
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
                method.implementation?.instructions?.mapIndexedNotNull { i, ins -> if (ins.opcode == Opcode.RETURN_OBJECT) i else null }
                    ?.asReversed()?.forEach { index ->
                        val reg = (mutable.implementation!!.instructions[index] as OneRegisterInstruction).registerA
                        mutable.addInstructions(index, "invoke-static {v$reg}, $EXT->$replacement(Ljava/lang/String;)Ljava/lang/String;\nmove-result-object v$reg")
                    }
            }
        }

        classDefForEach { classDef ->
            if (classDef.type != REGION_SOURCE) return@classDefForEach
            classDef.methods.filter { it.name == "LJIIIIZZ" && it.returnType == "Z" && it.parameterTypes.isEmpty() }.forEach { method ->
                val mutable = mutableClassDefBy(classDef.type).findMutableMethodOf(method)
                method.implementation?.instructions?.mapIndexedNotNull { i, ins -> if (ins.opcode == Opcode.RETURN) i else null }
                    ?.asReversed()?.forEach { index ->
                        val reg = (mutable.implementation!!.instructions[index] as OneRegisterInstruction).registerA
                        mutable.addInstructions(index, "invoke-static {v$reg}, $EXT->isInTikTokRegion(Z)Z\nmove-result v$reg")
                    }
            }
        }

        classDefForEach { classDef ->
            if (classDef.type != REGION_RESOLVER) return@classDefForEach
            classDef.methods.filter { it.name == "LIZ" && it.returnType == "Ljava/lang/String;" && it.parameterTypes.isEmpty() }.forEach { method ->
                val mutable = mutableClassDefBy(classDef.type).findMutableMethodOf(method)
                method.implementation!!.instructions.mapIndexedNotNull { i, ins -> if (ins.opcode == Opcode.RETURN_OBJECT) i else null }.asReversed().forEach { index ->
                    val reg = (mutable.implementation!!.instructions[index] as OneRegisterInstruction).registerA
                    mutable.addInstructions(index, "invoke-static {v$reg}, $EXT->getRegion(Ljava/lang/String;)Ljava/lang/String;\nmove-result-object v$reg")
                }
            }
        }

        classDefForEach { classDef ->
            if (classDef.type != REGION_CONFIG) return@classDefForEach
            classDef.methods.filter { it.returnType == "Ljava/util/Map;" && it.parameterTypes.isEmpty() }.forEach { method ->
                val mutable = mutableClassDefBy(classDef.type).findMutableMethodOf(method)
                method.implementation!!.instructions.mapIndexedNotNull { i, ins -> if (ins.opcode == Opcode.RETURN_OBJECT) i else null }.asReversed().forEach { index ->
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
                method.implementation!!.instructions.mapIndexedNotNull { i, ins -> if (ins.opcode == Opcode.RETURN_OBJECT) i else null }.asReversed().forEach { index ->
                    val reg = (mutable.implementation!!.instructions[index] as OneRegisterInstruction).registerA
                    mutable.addInstructions(index, "invoke-static {v$reg}, $EXT->$replacement(Ljava/lang/String;)Ljava/lang/String;\nmove-result-object v$reg")
                }
            }
        }

        val replacements = mapOf(
            "getSimCountryIso" to "getCountryIso", "getNetworkCountryIso" to "getCountryIso", "getSimCountryIsoForPhone" to "getCountryIso", "getNetworkCountryIsoForPhone" to "getCountryIso",
            "getSimOperator" to "getOperator", "getNetworkOperator" to "getOperator", "getNetworkOperatorForPhone" to "getOperator", "getSimOperatorNumeric" to "getOperator", "getNetworkOperatorNumeric" to "getOperator",
            "getSimOperatorName" to "getOperatorName", "getNetworkOperatorName" to "getOperatorName", "getNetworkOperatorNameForPhone" to "getOperatorName",
        )
        val patches = linkedMapOf<Method, ArrayDeque<Pair<Int, String>>>()
        val carrierNames = linkedMapOf<Method, ArrayDeque<Int>>()
        val carrierIds = linkedMapOf<Method, ArrayDeque<Int>>()
        val locales = linkedMapOf<Method, ArrayDeque<Int>>()
        val timezones = linkedMapOf<Method, ArrayDeque<Int>>()
        val dnsAll = linkedMapOf<Method, ArrayDeque<Pair<Int, Int>>>()
        val dnsOne = linkedMapOf<Method, ArrayDeque<Pair<Int, Int>>>()

        classDefForEach { classDef ->
            classDef.methods.forEach { method ->
                method.implementation?.instructions?.forEachIndexed { index, instruction ->
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL && instruction.opcode != Opcode.INVOKE_VIRTUAL_RANGE && instruction.opcode != Opcode.INVOKE_STATIC) return@forEachIndexed
                    val referenceInstruction = instruction as? ReferenceInstruction ?: return@forEachIndexed
                    val ref = referenceInstruction.reference as? MethodReference ?: return@forEachIndexed
                    if (ref.definingClass == TELEPHONY) {
                        if (ref.returnType == "Ljava/lang/String;" && replacements.containsKey(ref.name)) patches.getOrPut(method) { ArrayDeque() }.add(index to replacements.getValue(ref.name))
                        if (ref.returnType == "Ljava/lang/CharSequence;" && (ref.name == "getSimCarrierIdName" || ref.name == "getSimSpecificCarrierIdName")) carrierNames.getOrPut(method) { ArrayDeque() }.add(index)
                        if (ref.returnType == "I" && (ref.name == "getSimCarrierId" || ref.name == "getSimSpecificCarrierId")) carrierIds.getOrPut(method) { ArrayDeque() }.add(index)
                    }
                    if (ref.definingClass == LOCALE && ref.name == "getDefault" && ref.returnType == LOCALE && ref.parameterTypes.isEmpty()) locales.getOrPut(method) { ArrayDeque() }.add(index)
                    if (ref.definingClass == TIME_ZONE && ref.name == "getDefault" && ref.returnType == TIME_ZONE && ref.parameterTypes.isEmpty()) timezones.getOrPut(method) { ArrayDeque() }.add(index)
                    if (ref.definingClass == INET_ADDRESS && ref.name == "getAllByName" && ref.returnType == "[Ljava/net/InetAddress;" && ref.parameterTypes.size == 1 && ref.parameterTypes[0] == "Ljava/lang/String;") {
                        singleInvokeArgumentRegister(referenceInstruction)?.let { dnsAll.getOrPut(method) { ArrayDeque() }.add(index to it) }
                    }
                    if (ref.definingClass == INET_ADDRESS && ref.name == "getByName" && ref.returnType == "Ljava/net/InetAddress;" && ref.parameterTypes.size == 1 && ref.parameterTypes[0] == "Ljava/lang/String;") {
                        singleInvokeArgumentRegister(referenceInstruction)?.let { dnsOne.getOrPut(method) { ArrayDeque() }.add(index to it) }
                    }
                }
            }
        }

        patches.forEach { (method, list) ->
            val mutable = mutableClassDefBy(method.definingClass).findMutableMethodOf(method)
            while (list.isNotEmpty()) {
                val (index, name) = list.removeLast(); val next = mutable.implementation!!.instructions.getOrNull(index + 1) as? OneRegisterInstruction ?: continue
                val reg = next.registerA
                mutable.addInstructions(index + 2, "invoke-static {v$reg}, $EXT->$name(Ljava/lang/String;)Ljava/lang/String;\nmove-result-object v$reg")
            }
        }
        carrierNames.forEach { (method, list) ->
            val mutable = mutableClassDefBy(method.definingClass).findMutableMethodOf(method)
            while (list.isNotEmpty()) {
                val index = list.removeLast(); val next = mutable.implementation!!.instructions.getOrNull(index + 1) as? OneRegisterInstruction ?: continue
                val reg = next.registerA
                mutable.addInstructions(index + 2, "invoke-static {v$reg}, $EXT->getCarrierIdName(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;\nmove-result-object v$reg")
            }
        }
        carrierIds.forEach { (method, list) ->
            val mutable = mutableClassDefBy(method.definingClass).findMutableMethodOf(method)
            while (list.isNotEmpty()) {
                val index = list.removeLast(); val next = mutable.implementation!!.instructions.getOrNull(index + 1) as? OneRegisterInstruction ?: continue
                val reg = next.registerA
                mutable.addInstructions(index + 2, "invoke-static {v$reg}, $EXT->getCarrierId(I)I\nmove-result v$reg")
            }
        }
        locales.forEach { (method, list) ->
            val mutable = mutableClassDefBy(method.definingClass).findMutableMethodOf(method)
            while (list.isNotEmpty()) {
                val index = list.removeLast(); val next = mutable.implementation!!.instructions.getOrNull(index + 1) as? OneRegisterInstruction ?: continue
                val reg = next.registerA
                mutable.addInstructions(index + 2, "invoke-static {v$reg}, $EXT->getLocale(Ljava/util/Locale;)Ljava/util/Locale;\nmove-result-object v$reg")
            }
        }
        timezones.forEach { (method, list) ->
            val mutable = mutableClassDefBy(method.definingClass).findMutableMethodOf(method)
            while (list.isNotEmpty()) {
                val index = list.removeLast(); val next = mutable.implementation!!.instructions.getOrNull(index + 1) as? OneRegisterInstruction ?: continue
                val reg = next.registerA
                mutable.addInstructions(index + 2, "invoke-static {v$reg}, $EXT->getTimeZone(Ljava/util/TimeZone;)Ljava/util/TimeZone;\nmove-result-object v$reg")
            }
        }
        dnsAll.forEach { (method, list) ->
            val mutable = mutableClassDefBy(method.definingClass).findMutableMethodOf(method)
            while (list.isNotEmpty()) {
                val (index, hostReg) = list.removeLast()
                val result = mutable.implementation!!.instructions.getOrNull(index + 1) as? OneRegisterInstruction ?: continue
                val resultReg = result.registerA
                mutable.addInstructions(index + 2, "invoke-static {v$hostReg, v$resultReg}, $EXT->resolveAll(Ljava/lang/String;[Ljava/net/InetAddress;)[Ljava/net/InetAddress;\nmove-result-object v$resultReg")
            }
        }
        dnsOne.forEach { (method, list) ->
            val mutable = mutableClassDefBy(method.definingClass).findMutableMethodOf(method)
            while (list.isNotEmpty()) {
                val (index, hostReg) = list.removeLast()
                val result = mutable.implementation!!.instructions.getOrNull(index + 1) as? OneRegisterInstruction ?: continue
                val resultReg = result.registerA
                mutable.addInstructions(index + 2, "invoke-static {v$hostReg, v$resultReg}, $EXT->resolveOne(Ljava/lang/String;Ljava/net/InetAddress;)Ljava/net/InetAddress;\nmove-result-object v$resultReg")
            }
        }

        SettingsStatusLoadFingerprint.method.addInstruction(0, "invoke-static {}, Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableSimSpoof()V")
    }
}
