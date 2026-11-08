package com.akerusan.fieldincidentreporter.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.akerusan.fieldincidentreporter.core.designsystem.theme.ColorPair

/** A small, non-interactive label with a colored background, used for severity, status and sync. */
@Composable
fun Pill(text: String, colors: ColorPair, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = colors.container,
        contentColor = colors.content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}
