/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.RotationUpdateEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationSettings
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.utils.inventory.hotBarSlot
import net.ccbluex.liquidbounce.utils.inventory.inventorySlot
import net.ccbluex.liquidbounce.utils.inventory.isSplashPotion
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.boolean
import net.ccbluex.liquidbounce.value.choices
import net.ccbluex.liquidbounce.value.float
import net.ccbluex.liquidbounce.value.int
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.item.ItemPotion
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.potion.Potion

object AutoPot : Module("AutoPot", Category.PLAYER, hideModule = false) {

    private val health by float("Health", 15F, 1F..20F) { healPotion || regenerationPotion }
    private val delay by int("Delay", 500, 500..1000)

    // Useful potion options
    private val healPotion by boolean("HealPotion", true)
    private val regenerationPotion by boolean("RegenPotion", true)
    private val fireResistancePotion by boolean("FireResPotion", true)
    private val strengthPotion by boolean("StrengthPotion", true)
    private val jumpPotion by boolean("JumpPotion", true)
    private val speedPotion by boolean("SpeedPotion", true)

    private val openInventory by boolean("OpenInv", false)
    private val simulateInventory by boolean("SimulateInventory", true) { !openInventory }

    private val groundDistance by float("GroundDistance", 2F, 0F..5F)
    private val mode by choices("Mode", arrayOf("Normal", "Jump", "Port"), "Normal")

    private val options = RotationSettings(this).withoutKeepRotation().apply {
        applyServerSideValue.hideWithState(false)
        resetTicksValue.hideWithState()

        immediate = true
    }

    private val msTimer = MSTimer()
    private var potion = -1

    @EventTarget
    fun onRotationUpdate(event: RotationUpdateEvent) {
        if (!msTimer.hasTimePassed(delay) || mc.playerController.isInCreativeMode)
            return

        val player = mc.thePlayer ?: return

        // Hotbar Potion
        val potionInHotbar = findPotion(36, 45)

        if (potionInHotbar != null) {
            if (player.onGround) {
                when (mode.lowercase()) {
                    "jump" -> player.tryJump()
                    "port" -> player.moveEntity(0.0, 0.42, 0.0)
                }
            }

            // Prevent throwing potions into the void
            val fallingPlayer = FallingPlayer(player)

            val collisionBlock = fallingPlayer.findCollision(20)?.pos

            if (player.posY - (collisionBlock?.y ?: return) - 1 > groundDistance)
                return

            potion = potionInHotbar

            if (player.rotationPitch <= 80F) {
                setTargetRotation(Rotation(player.rotationYaw, nextFloat(80F, 90F)).fixedSensitivity(), options)
            }

            TickScheduler += {
                sendPacket(C09PacketHeldItemChange(potion))

                if (potion >= 0 && RotationUtils.serverRotation.pitch >= 75F) {
                    val itemStack = player.hotBarSlot(potion).stack

                    if (itemStack != null) {
                        sendPackets(
                            C08PacketPlayerBlockPlacement(itemStack),
                            C09PacketHeldItemChange(player.inventory.currentItem)
                        )

                        msTimer.reset()
                    }

                    potion = -1
                }
            }
            return
        }

        // Inventory Potion -> Hotbar Potion
        val potionInInventory = findPotion(9, 36) ?: return

        if (InventoryUtils.hasSpaceInHotbar()) {
            if (openInventory && mc.currentScreen !is GuiInventory)
                return

            TickScheduler += {
                if (simulateInventory)
                    serverOpenInventory = true

                mc.playerController.windowClick(0, potionInInventory, 0, 1, player)

                if (simulateInventory && mc.currentScreen !is GuiInventory)
                    serverOpenInventory = false

                msTimer.reset()
            }
        }

    }

    private fun findPotion(startSlot: Int, endSlot: Int): Int? {
        val player = mc.thePlayer

        for (i in startSlot until endSlot) {
            val stack = player.inventorySlot(i).stack

            if (stack == null || stack.item !is ItemPotion || !stack.isSplashPotion())
                continue

            val itemPotion = stack.item as ItemPotion

            for (potionEffect in itemPotion.getEffects(stack))
                if (player.health <= health && healPotion && potionEffect.potionID == Potion.heal.id)
                    return i

            if (!player.isPotionActive(Potion.regeneration))
                for (potionEffect in itemPotion.getEffects(stack))
                    if (player.health <= health && regenerationPotion && potionEffect.potionID == Potion.regeneration.id)
                        return i

            if (!player.isPotionActive(Potion.fireResistance))
                for (potionEffect in itemPotion.getEffects(stack))
                    if (fireResistancePotion && potionEffect.potionID == Potion.fireResistance.id)
                        return i

            if (!player.isPotionActive(Potion.moveSpeed))
                for (potionEffect in itemPotion.getEffects(stack))
                    if (speedPotion && potionEffect.potionID == Potion.moveSpeed.id)
                        return i

            if (!player.isPotionActive(Potion.jump))
                for (potionEffect in itemPotion.getEffects(stack))
                    if (jumpPotion && potionEffect.potionID == Potion.jump.id)
                        return i

            if (!player.isPotionActive(Potion.damageBoost))
                for (potionEffect in itemPotion.getEffects(stack))
                    if (strengthPotion && potionEffect.potionID == Potion.damageBoost.id)
                        return i
        }

        return null
    }

    override val tag
        get() = health.toString()

}