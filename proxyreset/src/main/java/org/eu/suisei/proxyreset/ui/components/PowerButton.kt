package org.eu.suisei.proxyreset.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.eu.suisei.proxyreset.R

@Composable
fun PowerButton(
    isConnected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val buttonColor by animateColorAsState(
        targetValue = if (isConnected) Color.White.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(500),
        label = "buttonColor"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(160.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = buttonColor,
            modifier = Modifier.size(140.dp),
            enabled = enabled,
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_power),
                    contentDescription = if (isConnected) "Disconnect proxy" else "Connect proxy",
                    tint = if (isConnected) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}
