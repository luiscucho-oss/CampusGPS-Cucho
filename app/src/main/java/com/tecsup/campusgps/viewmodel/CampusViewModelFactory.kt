package com.tecsup.campusgps.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tecsup.campusgps.data.LugaresRepositoryLocal
import com.tecsup.campusgps.data.UbicacionRepositoryGoogle

// El Context se usa aquí para crear el Repository; el ViewModel nunca lo recibe
class CampusViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MapaViewModel::class.java)) {
            "ViewModel desconocido: ${modelClass.name}"
        }
        return MapaViewModel(
            UbicacionRepositoryGoogle(context.applicationContext),
            LugaresRepositoryLocal()
        ) as T
    }
}
