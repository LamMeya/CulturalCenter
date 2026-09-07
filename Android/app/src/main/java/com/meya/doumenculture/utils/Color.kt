package com.meya.doumenculture.utils

import androidx.compose.ui.graphics.Color

// MARK: - Design Tokens
val Primary = Color(0xFF3D8E7A)
val PrimaryDark = Color(0xFF2E6E5E)
val Background = Color(0xFFFAF8F6)
val Card = Color(0xFFFFFFFF)
val Border = Color(0xFFEBE3DA)
val TextPrimary = Color(0xFF211C18)
val Muted = Color(0xFF8C7B6A)
val Accent = Color(0xFFE8A840)

fun String.hexToColor(): Color = try {
    val hex = this.replace("#", "")
    val color = hex.toLong(16)
    when (hex.length) {
        6 -> Color(color or 0xFF000000)
        8 -> Color(color)
        else -> TextPrimary
    }
} catch (_: Exception) {
    TextPrimary
}

fun bookingStatusColor(status: String?): Color = when (status) {
    "pending" -> Accent
    "won" -> Primary
    "lost" -> Muted
    "cancelled" -> Muted
    else -> Muted
}

fun teamRoleColor(role: String?): Color = when (role) {
    "leader" -> Accent
    "vice_leader" -> Primary
    "member" -> Muted
    else -> Muted
}
