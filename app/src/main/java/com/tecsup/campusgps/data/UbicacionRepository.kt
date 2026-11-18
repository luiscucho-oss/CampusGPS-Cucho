package com.tecsup.campusgps.data

import com.tecsup.campusgps.model.Posicion

interface UbicacionRepository {
    suspend fun obtenerPosicion(): Result<Posicion>
    fun playServicesDisponible(): Boolean
}

// Se lanza cuando el GPS está apagado o Fused no puede determinar la ubicación
class UbicacionNoDisponibleException : Exception("Activa el GPS e intenta de nuevo")
