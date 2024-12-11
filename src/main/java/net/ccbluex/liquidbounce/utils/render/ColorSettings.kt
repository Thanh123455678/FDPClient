/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.Element.Companion.MAX_GRADIENT_COLORS
import net.ccbluex.liquidbounce.value.float
import net.ccbluex.liquidbounce.value.int
import java.awt.Color

class ColorSettingsFloat(owner: Any, name: String, val index: Int? = null, generalApply: () -> Boolean = { true }) {
    private val r by float(
        "$name-R${index ?: ""}",
        if ((index ?: 0) % 3 == 1) 255f else 0f,
        0f..255f, subjective = true
    ) { generalApply() }
    private val g by float(
        "$name-G${index ?: ""}",
        if ((index ?: 0) % 3 == 2) 255f else 0f,
        0f..255f, subjective = true
    ) { generalApply() }
    private val b by float(
        "$name-B${index ?: ""}",
        if ((index ?: 0) % 3 == 0) 255f else 0f,
        0f..255f, subjective = true
    ) { generalApply() }

    fun color() = Color(r / 255f, g / 255f, b / 255f)

    init {
        when (owner) {
            is Element -> owner.addConfigurable(this)
            is Module -> owner.addConfigurable(this)
            // Should any other class use this, add here
        }
    }

    companion object {
        fun create(
            owner: Any, name: String, colors: Int = MAX_GRADIENT_COLORS, generalApply: (Int) -> Boolean = { true },
        ): List<ColorSettingsFloat> {
            return (1..colors).map { ColorSettingsFloat(owner, name, it) { generalApply(it) } }
        }
    }
}

class ColorSettingsInteger(
    owner: Any, name: String? = null, val index: Int? = null, withAlpha: Boolean = true,
    zeroAlphaCheck: Boolean = false, applyMax: Boolean = false,
    alphaApply: () -> Boolean? = { null }, generalApply: () -> Boolean = { true },
) {
    private val string = if (name == null) "" else "$name-"
    private val max = if (applyMax) 255 else 0

    private var red = int(
        "${string}R${index ?: ""}",
        max,
        0..255, subjective = true
    ) { generalApply() && (!zeroAlphaCheck || a > 0) }
    private var green = int(
        "${string}G${index ?: ""}",
        max,
        0..255, subjective = true
    ) { generalApply() && (!zeroAlphaCheck || a > 0) }
    private var blue = int(
        "${string}B${index ?: ""}",
        max,
        0..255, subjective = true
    ) { generalApply() && (!zeroAlphaCheck || a > 0) }
    private var alpha = int(
        "${string}Alpha${index ?: ""}",
        255,
        0..255, subjective = true
    ) { alphaApply() ?: generalApply() && withAlpha }

    private var r by red
    private var g by green
    private var b by blue
    private var a by alpha

    fun color(a: Int = this.a) = Color(r, g, b, a)

    fun color() = Color(r, g, b, a)

    fun with(r: Int? = null, g: Int? = null, b: Int? = null, a: Int? = null): ColorSettingsInteger {
        r?.let { red.setAndUpdateDefault(it) }
        g?.let { green.setAndUpdateDefault(it) }
        b?.let { blue.setAndUpdateDefault(it) }
        a?.let { alpha.setAndUpdateDefault(it) }

        return this
    }

    fun with(color: Color) = with(color.red, color.green, color.blue, color.alpha)

    init {
        when (owner) {
            is Element -> owner.addConfigurable(this)
            is Module -> owner.addConfigurable(this)
            // Should any other class use this, add here
        }
    }

    companion object {
        fun create(
            owner: Any, name: String, colors: Int, withAlpha: Boolean = true, zeroAlphaCheck: Boolean = true,
            applyMax: Boolean = false, generalApply: (Int) -> Boolean = { true },
        ): List<ColorSettingsInteger> {
            return (1..colors).map {
                ColorSettingsInteger(
                    owner,
                    name,
                    it,
                    withAlpha,
                    zeroAlphaCheck,
                    applyMax
                ) { generalApply(it) }
            }
        }
    }
}

fun List<ColorSettingsFloat>.toColorArray(max: Int) = (0 until max).map {
    val colors = this[it].color()

    floatArrayOf(
        colors.red.toFloat() / 255f,
        colors.green.toFloat() / 255f,
        colors.blue.toFloat() / 255f,
        1f
    )
}