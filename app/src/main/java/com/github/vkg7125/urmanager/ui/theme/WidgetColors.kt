package com.github.vkg7125.urmanager.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

object WidgetColors {
    val widgetBackground = ColorProvider(
        day = Color(0xFF131829),
        night = Color(0xFF131829)
    )
    val textColorPrimary = ColorProvider(
        day = Color(0xFFFFFFFF),
        night = Color(0xFFFFFFFF)
    )
    val textColorSecondary = ColorProvider(
        day = Color(0xFFB0B0B0),
        night = Color(0xFFB0B0B0)
    )
    val errorColor = ColorProvider(
        day = Color(0xFFD32F2F),
        night = Color(0xFFFF5252)
    )
}