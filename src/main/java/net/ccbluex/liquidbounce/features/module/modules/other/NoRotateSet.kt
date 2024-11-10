/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationSettings
import net.ccbluex.liquidbounce.utils.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.extensions.rotation
import net.ccbluex.liquidbounce.value.boolean
import net.minecraft.entity.player.EntityPlayer

object NoRotateSet : Module("NoRotateSet", Category.OTHER, gameDetecting = false, hideModule = false) {
    var savedRotation = Rotation.ZERO

    private val ignoreOnSpawn by boolean("IgnoreOnSpawn", false)
    val affectRotation by boolean("AffectRotation", true)

    private val options = RotationSettings(this) { affectRotation }.apply {
        applyServerSideValue.excludeWithState(true)
        resetTicksValue.excludeWithState(1)

        withoutKeepRotation()
    }

    fun shouldModify(player: EntityPlayer) = handleEvents() && (!ignoreOnSpawn || player.ticksExisted != 0)

    fun rotateBackToPlayerRotation() {
        val player = mc.thePlayer ?: return

        currentRotation = player.rotation

        setTargetRotation(savedRotation, options = options)
    }
}