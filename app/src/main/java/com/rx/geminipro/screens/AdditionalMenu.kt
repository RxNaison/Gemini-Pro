package com.rx.geminipro.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdditionalMenu(onClose: () -> Unit, items: List<@Composable () -> Unit>)
{
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        onDismissRequest = onClose,
        windowInsets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                            WindowInsets(0, 0, 0, 0)
                       else
                            BottomSheetDefaults.windowInsets
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            FlowRow(
                modifier = Modifier
                    .padding(start = 40.dp, top = 8.dp, bottom = 16.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { item ->
                    item()
                }
            }
        }
    }
}