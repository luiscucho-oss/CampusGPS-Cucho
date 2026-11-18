package com.tecsup.campusgps.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecsup.campusgps.data.LugaresRepository
import com.tecsup.campusgps.data.UbicacionNoDisponibleException
import com.tecsup.campusgps.data.UbicacionRepository
import com.tecsup.campusgps.model.Lugar
import com.tecsup.campusgps.model.LugarConDistancia
import com.tecsup.campusgps.model.Posicion
import com.tecsup.campusgps.model.TipoError
import com.tecsup.campusgps.model.UbicacionUi
import com.tecsup.campusgps.util.distanciaMetros
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapaViewModel(
    private val ubicacionRepo: UbicacionRepository,
    private val lugaresRepo: LugaresRepository
) : ViewModel() {

    private val _estado = MutableStateFlow<UbicacionUi>(UbicacionUi.Inicial)
    val estado: StateFlow<UbicacionUi> = _estado.asStateFlow()

    // Todos los lugares con su distancia (ordenados tras buscar)
    private val _todos = MutableStateFlow(
        lugaresRepo.obtenerLugares().map { LugarConDistancia(it, null) }
    )

    // Mejora 1: filtro por categoría (null = todas)
    val categorias: List<String> = lugaresRepo.obtenerLugares().map { it.categoria }.distinct()
    private val _categoria = MutableStateFlow<String?>(null)
    val categoria: StateFlow<String?> = _categoria.asStateFlow()

    // Lo que ve la pantalla: la lista ya filtrada. El mapa y la lista usan la misma
    val lugares: StateFlow<List<LugarConDistancia>> = combine(_todos, _categoria) { todos, cat ->
        if (cat == null) todos else todos.filter { it.lugar.categoria == cat }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _todos.value)

    // Mejora 2: radio de cercanía en metros (null = sin radio)
    val opcionesRadio: List<Int> = listOf(100, 200, 500)
    private val _radio = MutableStateFlow<Int?>(null)
    val radio: StateFlow<Int?> = _radio.asStateFlow()

    // Ids de los lugares que quedan dentro del radio (se calcula aquí, no en la pantalla)
    val dentroDelRadio: StateFlow<Set<Int>> = combine(_todos, _radio) { todos, r ->
        if (r == null) emptySet()
        else todos.filter { it.metros != null && it.metros <= r }.map { it.lugar.id }.toSet()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val _seleccionado = MutableStateFlow<Int?>(null)
    val seleccionado: StateFlow<Int?> = _seleccionado.asStateFlow()

    fun buscar() {
        // Sin Google Play Services no hay Fused: avisamos sin intentar
        if (!ubicacionRepo.playServicesDisponible()) {
            _estado.value = UbicacionUi.Error(
                "Este dispositivo no tiene Google Play Services",
                TipoError.SIN_PLAY_SERVICES
            )
            return
        }
        _estado.value = UbicacionUi.Buscando
        viewModelScope.launch {
            ubicacionRepo.obtenerPosicion()
                .onSuccess {
                    _estado.value = UbicacionUi.Lista(it)
                    actualizarDistancias(it)
                }
                .onFailure { _estado.value = errorDe(it) }
        }
    }

    fun permisoDenegado() {
        _estado.value = UbicacionUi.Error(
            "Sin permiso de ubicación no puedo mostrar dónde estás",
            TipoError.SIN_PERMISO
        )
    }

    // Calcula la distancia a cada lugar y publica la lista de la más cercana a la más lejana
    private fun actualizarDistancias(pos: Posicion) {
        val ordenados = lugaresRepo.obtenerLugares()
            .map { LugarConDistancia(it, distanciaMetros(pos, it)) }
            .sortedBy { it.metros }
        _todos.value = ordenados
        Log.d(TAG, ordenados.joinToString { "${it.lugar.nombre}=${it.metros?.toInt()} m" })
    }

    fun seleccionar(id: Int?) {
        _seleccionado.value = id
    }

    fun cambiarRadio(metros: Int?) {
        _radio.value = metros
    }

    fun filtrar(cat: String?) {
        _categoria.value = cat
        // Si el seleccionado queda oculto por el filtro, se deselecciona
        val id = _seleccionado.value
        if (cat != null && id != null &&
            _todos.value.none { it.lugar.id == id && it.lugar.categoria == cat }
        ) {
            _seleccionado.value = null
        }
    }

    // Se deriva de lugares: el primero con distancia calculada (null si aún no hay ubicación)
    val masCercano: StateFlow<Lugar?> = lugares
        .map { lista -> lista.firstOrNull { it.metros != null }?.lugar }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private companion object {
        const val TAG = "MapaViewModel"
    }

    private fun errorDe(e: Throwable): UbicacionUi.Error = when (e) {
        is SecurityException ->
            UbicacionUi.Error("Falta el permiso de ubicación", TipoError.SIN_PERMISO)
        is UbicacionNoDisponibleException ->
            UbicacionUi.Error("Activa el GPS e intenta de nuevo", TipoError.GPS_APAGADO)
        else ->
            UbicacionUi.Error(e.message ?: "No se pudo obtener la ubicación", TipoError.OTRO)
    }
}
