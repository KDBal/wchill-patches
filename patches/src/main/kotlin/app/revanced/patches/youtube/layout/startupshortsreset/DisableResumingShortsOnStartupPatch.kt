package app.revanced.patches.youtube.layout.startupshortsreset

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.all.misc.resources.addResources
import app.revanced.patches.all.misc.resources.addResourcesPatch
import app.revanced.patches.shared.misc.settings.preference.SwitchPreference
import app.revanced.patches.youtube.misc.extension.sharedExtensionPatch
import app.revanced.patches.youtube.misc.settings.PreferenceScreen
import app.revanced.patches.youtube.misc.settings.settingsPatch
import app.revanced.util.addInstructionsAtControlFlowLabel
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/revanced/extension/youtube/patches/DisableResumingStartupShortsPlayerPatch;"

val disableResumingShortsOnStartupPatch = bytecodePatch(
    name = "Disable resuming Shorts on startup",
    description = "Adds an option to disable the Shorts player from resuming on app startup when Shorts were last being watched.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        addResourcesPatch,
    )

    compatibleWith(
        "com.google.android.youtube"(
            "19.16.39",
            "19.25.37",
            "19.34.42",
            "19.43.41",
            "19.45.38",
            "19.46.42",
            "19.47.53",
        ),
    )

    execute {
        addResources("youtube", "layout.startupshortsreset.disableResumingShortsOnStartupPatch")

        PreferenceScreen.SHORTS.addPreferences(
            SwitchPreference("revanced_disable_resuming_shorts_player"),
        )

        userWasInShortsFingerprint.method.apply {
            val listenableInstructionIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_INTERFACE &&
                    getReference<MethodReference>()?.definingClass == "Lcom/google/common/util/concurrent/ListenableFuture;" &&
                    getReference<MethodReference>()?.name == "isDone"
            }
            val freeRegister = getInstruction<OneRegisterInstruction>(listenableInstructionIndex + 1).registerA

            addInstructionsAtControlFlowLabel(
                listenableInstructionIndex,
                """
                    invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->disableResumingStartupShortsPlayer()Z
                    move-result v$freeRegister
                    if-eqz v$freeRegister, :show_startup_shorts_player
                    return-void
                    :show_startup_shorts_player
                    nop
                """,
            )
        }

        userWasInShortsConfigFingerprint.method.addInstructions(
            0,
            """
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->disableResumingStartupShortsPlayer()Z
                move-result v0
                if-eqz v0, :show
                const/4 v0, 0x0
                return v0
                :show
                nop
            """
        )
    }
}
