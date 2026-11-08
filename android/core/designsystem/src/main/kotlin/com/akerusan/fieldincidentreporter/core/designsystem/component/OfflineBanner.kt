package com.akerusan.fieldincidentreporter.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.akerusan.fieldincidentreporter.core.designsystem.R

/** Explains that the app keeps working offline. Announced by TalkBack when it appears. */
@Composable
fun OfflineBanner(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.CloudOff, contentDescription = null)
                Text(text = stringResource(R.string.offline_banner), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
