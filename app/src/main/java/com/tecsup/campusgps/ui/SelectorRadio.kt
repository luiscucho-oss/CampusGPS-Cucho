package com.tecsup.campusgps.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Selector de radio: 100, 200 o 500 m. Tocar el activo lo quita
@Composable
fun SelectorRadio(
    opciones: List<Int>,
    seleccionado: Int?,
    habilitado: Boolean,
    onSeleccionar: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Radio:", style = MaterialTheme.typography.labelLarge)
        opciones.forEach { metros ->
            FilterChip(
                selected = seleccionado == metros,
                enabled = habilitado,
                onClick = { onSeleccionar(if (seleccionado == metros) null else metros) },
                label = { Text("$metros m") }
            )
        }
    }
}
