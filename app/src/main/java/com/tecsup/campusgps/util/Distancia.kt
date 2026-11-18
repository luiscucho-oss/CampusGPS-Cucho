package com.tecsup.campusgps.util

import android.location.Location
import com.tecsup.campusgps.model.Lugar
import com.tecsup.campusgps.model.Posicion

// Función pura: distancia en metros entre la posición del usuario y un lugar
fun distanciaMetros(desde: Posicion, hasta: Lugar): Float {
    val resultado = FloatArray(1)
    // Orden: (lat1, lon1, lat2, lon2, resultado)
    Location.distanceBetween(
        desde.latitud, desde.longitud,
        hasta.latitud, hasta.longitud,
        resultado
    )
    return resultado[0]
}
