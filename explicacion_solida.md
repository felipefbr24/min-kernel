# Explicación sólida del simulador de kernel

Esta guía recorre cada clase del proyecto, describiendo su rol, los algoritmos que aplica y cómo interactúa con el resto del simulador.

## Aplicacion
- `Aplicacion.main` arranca la interfaz gráfica en el hilo de eventos de Swing mediante `SwingUtilities.invokeLater`, instanciando y mostrando `InterfazKernel`. Esto evita bloqueos de la UI y centraliza el punto de entrada. 

## SimuladorKernel (coordinador principal)
- Mantiene gestores especializados: `GestorProcesos`, `GestorMemoria` con 256 KB, `SistemaArchivosSim` y `GestorES`.
- `crearProceso` normaliza el nombre, crea el PCB, intenta asignar memoria con *first fit* y encola el proceso en **LISTO**; si falla la asignación revierte la creación.
- `avanzarTick` ejecuta un ciclo de CPU delegando en el planificador Round Robin, libera memoria y archivos cuando un proceso termina y devuelve un `ResultadoTick` con el PID ejecutado o inactividad.
- `solicitarES` bloquea un proceso válido y encola una `SolicitudES`; `completarES` saca la más antigua, reanuda el proceso en **LISTO** y reporta la interrupción.
- API de archivos: `crearArchivo`, `abrirArchivo` (mapea los estados `EXITO`, `EN_USO`, `NO_ENCONTRADO`), `cerrarArchivo` (con estados `EXITO`, `NO_PROPIETARIO`, `SIN_APERTURA`, `NO_ENCONTRADO`) y `forzarTerminacion` que libera memoria, cierra archivos y marca el proceso como terminado.
- Getters de listas/estadísticas y `reiniciar` reinician todos los gestores.

## Procesos
- **BloqueControlProceso**: PCB con PID, nombre, ráfaga original, memoria solicitada, tiempo restante, estado y conjunto de archivos abiertos. Arranca en `NUEVO` con `tiempoRestante` igual a la ráfaga solicitada.
- **EstadoProceso**: enum con el ciclo de vida `NUEVO`, `LISTO`, `EJECUTANDO`, `BLOQUEADO`, `TERMINADO`.
- **GestorProcesos**:
  - Estructuras: lista de PCBs, cola de listos (`ArrayDeque`), referencia al proceso en ejecución, contador de cuanto restante, cuanto configurable (default 2), próximo PID y último PID ejecutado.
  - `crearProceso` y `encolarListo` crean PCBs y los marcan `LISTO` salvo que estén terminados.
  - `avanzarCiclo` aplica Round Robin: si no hay proceso, despacha el siguiente; si hay, marca **EJECUTANDO**, descuenta `tiempoRestante` y `tiempoCuantoRestante`, termina el proceso al llegar a 0 (liberando el CPU) o lo reencola al agotarse el cuanto.
  - `despacharSiNecesario` selecciona el siguiente de la cola de listos y reinicia el cuanto; no hace nada si ya hay uno ejecutando.
  - `bloquearPorES` mueve un proceso activo a **BLOQUEADO** retirándolo del CPU o de la cola; `reanudarPorES` lo vuelve a encolar en **LISTO**.
  - `terminar` marca **TERMINADO** y limpia referencias; `eliminarProceso` borra el PCB por completo.
  - Utilidades: `buscar`, `existeActivo`, `obtenerProcesos`, `obtenerPidsActivos`, `procesoActual`, `obtenerUltimoPidEjecutado`.
  - Configuración de quantum: `obtenerCuanto` y `configurarCuanto` con límite inferior 1, ajustando el cuanto restante del proceso en curso si es necesario.
  - Asociación de archivos: `asociarArchivo`, `desasociarArchivo`, `limpiarArchivos` para reflejar aperturas/cierres en el PCB.
  - `reiniciar` limpia todas las estructuras y contadores.

## Memoria
- **BloqueMemoriaInterno** (mutable) y **BloqueMemoria** (inmutable para la UI) modelan inicio, tamaño y PID asignado; un PID nulo indica bloque libre.
- **GestorMemoria**:
  - Constructor inicializa una lista con un bloque libre del tamaño total.
  - `asignar` aplica *first fit*: recorre secuencialmente los bloques, toma el primero libre que quepa, lo divide si sobra espacio y marca el PID. Devuelve `true/false` según éxito.
  - `liberarPorPid` marca como libres los bloques del PID y llama a `fusionarBloquesLibres` para reducir fragmentación externa.
  - `fusionarBloquesLibres` ordena por inicio y une bloques adyacentes libres; si todo se elimina accidentalmente, reestablece un bloque libre total.
  - Lecturas: `obtenerBloques` (ordenados para la UI), `obtenerTamanoTotal`, `obtenerTamanoUsado` (suma bloques no libres), `obtenerTamanoLibre` (total menos usado).
  - `reiniciar` restaura un único bloque libre del tamaño total.

## Archivos
- **EntradaArchivo**: metadatos (nombre, PID propietario, contador de aperturas, PID que lo tiene abierto actualmente).
- **EstadoApertura** y **EstadoCierre**: enums que describen resultados al abrir/cerrar (éxito, en uso, no encontrado, no abierto, no propietario).
- **SistemaArchivosSim**:
  - Estructuras: `LinkedHashMap` de entradas para preservar orden de creación y mapa `openByPid` para aperturas activas.
  - `crearArchivo` añade entrada si no existe.
  - `abrirArchivo` comprueba existencia y exclusividad: solo un PID puede tener un archivo abierto a la vez; marca la apertura y asocia en `openByPid`.
  - `cerrarArchivo` valida existencia, que esté abierto y que el solicitante sea el mismo PID que lo abrió; ajusta contadores y limpia mapas.
  - `cerrarTodoPorPid` cierra en cascada todas las aperturas pertenecientes a un PID (útil al terminar procesos).
  - `obtenerArchivos`, `reiniciar` y `abiertoPor` exponen el estado actual a la UI.

## Entrada/Salida (E/S)
- **SolicitudES**: DTO con PID, nombre del dispositivo y detalle de la petición.
- **GestorES**: mantiene una cola FIFO (`ArrayDeque`), con `solicitar` para encolar, `completarSiguiente` para despachar la solicitud más antigua, `obtenerCola` para la UI y `reiniciar` para limpiar la cola.

## DTO de CPU
- **ResultadoTick**: agrupa mensaje de log, PID y nombre del proceso ejecutado, y una bandera de inactividad (PID -1) para que la UI represente los ciclos de CPU correctamente.

## Interfaz gráfica (InterfazKernel)
- Ventana principal Swing que compone controles superiores (ejecutar tick, modo automático con temporizador, reset, ajuste de cuanto), pestañas de procesos/memoria/archivos+E/S y un log inferior.
- Conecta botones y formularios a las operaciones del `SimuladorKernel`; refresca tablas, combos y la barra visual de planificación (`PanelPlan`) tras cada acción o tick.
- El temporizador de auto-tick invoca `avanzarTick` periódicamente usando el cuanto configurado; el log antepone hora y ajusta scroll automáticamente.
- Utiliza utilidades para estilizar botones y tarjetas y helpers para seleccionar PIDs desde tablas y combos.
