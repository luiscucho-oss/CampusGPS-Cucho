package com.tecsup.campusgps.data

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.tecsup.campusgps.model.Posicion
import kotlinx.coroutines.tasks.await

class UbicacionRepositoryGoogle(context: Context) : UbicacionRepository {

    private val cliente = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    // La pantalla comprueba el permiso antes de llamar; si falta, Fused lanza SecurityException
    @SuppressLint("MissingPermission")
    override suspend fun obtenerPosicion(): Result<Posicion> = runCatching {
        val token = CancellationTokenSource()
        val location = cliente
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
            .await()
            ?: error("No se pudo determinar la ubicación")
        Posicion(location.latitude, location.longitude)
    }
}
