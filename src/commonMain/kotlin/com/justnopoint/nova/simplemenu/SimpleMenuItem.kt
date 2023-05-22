package com.justnopoint.nova.simplemenu

import com.justnopoint.nova.MenuFonts
import com.justnopoint.nova.NovaWindow
import com.justnopoint.nova.TextAlignment

open class SimpleMenuItem private constructor(
    private val config: MenuItemConfig
): MenuItem {
    private var parent: SimpleMenu? = null

    private class MenuItemConfig {
        lateinit var title: RenderableText
        var hint: RenderableText? = null
        var value: RenderableText? = null
        var valueProvider: (() -> String)? = null
        var valueChanger: (() -> Unit)? = null
    }
    class Builder {
        private val config = MenuItemConfig()
        private var titleString = ""
        private var titleFont = MenuFonts.menuFontDefault
        private var titleAlignment = TextAlignment.LEFT
        private var valueFont = MenuFonts.menuFontDefault
        private var valueAlignment = TextAlignment.RIGHT
        private var hintString: String? = null
        private var hintFont = MenuFonts.menuFontGray
        fun setTitle(title: String): Builder {
            titleString = title
            return this
        }
        fun setTitleFont(font: ConfiguredFont): Builder {
            titleFont = font
            return this
        }
        fun setTitleAlignment(alignment: TextAlignment): Builder {
            titleAlignment = alignment
            return this
        }
        fun setValueFont(font: ConfiguredFont): Builder {
            valueFont = font
            return this
        }
        fun setValueAlignment(alignment: TextAlignment): Builder {
            valueAlignment = alignment
            return this
        }
        fun setHint(hint: String): Builder {
            hintString = hint
            return this
        }
        fun setHintFont(font: ConfiguredFont): Builder {
            hintFont = font
            return this
        }
        fun getter(getter: (() -> String)): Builder {
            config.valueProvider = getter
            return this
        }
        fun setter(setter: (() -> Unit)): Builder {
            config.valueChanger = setter
            return this
        }
        fun setValue(font: RenderableText): Builder {
            config.value = font
            return this
        }
        fun build(): SimpleMenuItem {
            config.title = RenderableText(
                text = titleString,
                font = titleFont,
                alignment = titleAlignment
            )
            config.valueProvider?.let {
                config.value = RenderableText(
                    text = "",
                    font = valueFont,
                    alignment = valueAlignment
                )
            }
            hintString?.let {
                config.hint = RenderableText(
                    text = it,
                    font = hintFont
                )
            }

            return SimpleMenuItem(config)
        }
    }

    override fun getHeight(): Int {
        var height = config.title.getLineHeight()
        if(config.title.alignment == config.value?.alignment) {
            height += config.value?.getLineHeight()?:0
        }
        return height
    }

    override fun setParent(parent: SimpleMenu) {
        this.parent = parent
    }

    override fun render(window: NovaWindow, selected: Boolean, frame: Int, y: Int) {
        if(parent == null) return

        var currentY = y
        renderTitle(window, selected, frame, currentY)
        if(config.title.alignment == config.value?.alignment) {
            currentY += config.value?.getLineHeight()?:0
        }
        renderValue(window, currentY)
        if(selected) {
            renderHint(window)
        }
    }

    private fun renderTitle(window: NovaWindow, selected: Boolean, frame: Int, y: Int) {
        config.title.apply {
            val x = when(alignment) {
                TextAlignment.LEFT -> parent!!.leftEdge
                TextAlignment.RIGHT -> parent!!.rightEdge
                TextAlignment.CENTER -> (parent!!.leftEdge + parent!!.rightEdge) / 2
            }
            render(window, selected, frame, x, y)
        }
    }

    private fun renderValue(window: NovaWindow, y: Int) {
        config.value?.apply {
            val x = when(alignment) {
                TextAlignment.LEFT -> parent!!.leftEdge
                TextAlignment.RIGHT -> parent!!.rightEdge
                TextAlignment.CENTER -> (parent!!.leftEdge + parent!!.rightEdge) / 2
            }
            text = config.valueProvider?.invoke()?:""
            render(window, false, 0, x, y)
        }
    }

    private fun renderHint(window: NovaWindow) {
        config.hint?.render(window, false, 0, parent!!.hintCoord.first, parent!!.hintCoord.second)
    }

    override fun onSelect() {
        config.valueChanger?.invoke()
    }
}

interface MenuItem {
    fun setParent(parent: SimpleMenu)
    fun onSelect()
    fun getHeight(): Int
    fun render(window: NovaWindow, selected: Boolean, frame: Int, y: Int)
}