package com.rx.geminipro.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.collections.forEach


data class MenuItemData(
    val painterResId: Int,
    val name: String,
    val onClick: () -> Unit
)

@Composable
fun AdditionalMenuItem(painter: Painter, name: String, onClick: () -> Unit)
{
    val haptic = LocalHapticFeedback.current
    Column(modifier = Modifier.padding(horizontal = 5.dp),){
        IconButton(
            modifier = Modifier.align(Alignment.CenterHorizontally).size(70.dp),
            onClick =
            {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            },
            content = {
                Image(
                    painter = painter,
                    contentDescription = null
                )
            }
        )
        Text(modifier = Modifier.align(Alignment.CenterHorizontally), text = name, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdditionalMenu(
    onClose: () -> Unit,
    items: List<MenuItemData>
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items.forEach { item ->
                AdditionalMenuItem(
                    painter = painterResource(id = item.painterResId),
                    name = item.name,
                    onClick = {
                        item.onClick()
                        onClose()
                    }
                )
            }
        }
    }
}