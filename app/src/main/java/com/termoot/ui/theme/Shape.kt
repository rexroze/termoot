package com.termoot.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val TermootShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // Tiny decor
    small = RoundedCornerShape(8.dp),         // Inputs, buttons, chips
    medium = RoundedCornerShape(12.dp),       // Cards, dialogs
    large = RoundedCornerShape(16.dp),        // FAB, bottom sheets
    extraLarge = RoundedCornerShape(24.dp)    // Modal sheets
)
