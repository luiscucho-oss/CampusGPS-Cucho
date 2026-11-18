package com.tecsup.campusgps.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.tecsup.campusgps.model.Posicion
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class UbicacionRepositoryGoogle(context: Context) : UbicacionRepository {

    private val contexto = context.applicationContext
    private val cliente by lazy { LocationServices.getFusedLocationProviderClient(contexto) }

    override fun playServicesDisponible(): Boolean =
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(contexto) == ConnectionResult.SUCCESS

    // La pantalla comprueba el permiso antes de llamar; si falta, Fused lanza SecurityException
    @SuppressLint("MissingPermission")
    override suspend fun obtenerPosicion(): Result<Posicion> = runCatching {
        val gestor = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(gestor)) throw UbicacionNoDisponibleException()

        val token = CancellationTokenSource()
        // getCurrentLocation devuelve null si no puede determinar la ubicación;
        // el tiempo límite evita quedarse en "Buscando" para siempre
        val location = withTimeoutOrNull(TIEMPO_LIMITE_MS) {
            cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
        } ?: run {
            token.cancel()
            throw UbicacionNoDisponibleException()
        }
        Posicion(location.latitude, location.longitude)
    }

    private companion object {
        const val TIEMPO_LIMITE_MS = 15_000L
    }
}
