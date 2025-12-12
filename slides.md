# Simulador de Kernel - Guía en Diapositivas

---
## Diapositiva 1 · Visión general
- Mini-kernel educativo que integra planificación, memoria, archivos y E/S sobre una UI Swing.
- Entrada: `App` crea `KernelUI` en el hilo de eventos; la UI orquesta acciones sobre `SimuladorKernel`.
- Estados visibles: listas de procesos, bloques de memoria, tabla de archivos y cola de E/S.

---
## Diapositiva 2 · Arquitectura por módulos
- **SimuladorKernel** coordina gestores especializados y expone métodos para la UI.
- **GestorProcesos**: planificador Round Robin con `readyQueue`, `running` y `quantum` configurable.
- **GestorMemoria**: asignación **First-Fit**, división de bloques y fusión adyacente al liberar.
- **SistemaArchivosSim**: entradas con propietario y apertura exclusiva por PID.
- **GestorES**: cola FIFO de solicitudes que simula interrupciones de E/S.

---
## Diapositiva 3 · Ciclo de CPU (Round Robin)
- Cada `tick`:
  - Si no hay `running`, despacha el siguiente PID de `readyQueue` y reinicia el `timeSlice`.
  - Ejecución: decrementa ráfaga restante y `timeSlice`.
  - Si la ráfaga llega a 0 → estado `TERMINATED`, libera memoria/archivos.
  - Si `timeSlice` llega a 0 → preempción: pasa a `READY` y encola al final.
- El quantum puede ajustarse desde la UI; al subirlo se extiende el `timeSlice` del proceso actual.

---
## Diapositiva 4 · Gestión de memoria
- Memoria total inicial: 256 KB en un bloque libre.
- **First-Fit** recorre secuencialmente los bloques libres hasta hallar uno que quepa:
  - Si coincide exacto → se marca con el PID.
  - Si sobra espacio → divide el bloque en ocupado + libre.
- Al liberar por PID se marcan libres y se fusionan contiguos para reducir fragmentación externa.
- La UI muestra barra de uso y listado textual (inicio, fin, tamaño, PID/libre).

---
## Diapositiva 5 · Sistema de archivos simulado
- Estructura: `LinkedHashMap` que preserva orden de creación y mapa de aperturas por PID.
- **Crear**: agrega entrada si el nombre no existe.
- **Abrir**: solo si no está abierto; registra `openedByPid` y aumenta contador de aperturas.
- **Cerrar**: valida existencia, que esté abierto y que el solicitante sea el dueño de la apertura.
- **Forzado**: al terminar un proceso se cierran todos sus archivos abiertos automáticamente.

---
## Diapositiva 6 · Entrada/Salida
- `requestIO(pid, dispositivo, detalle)`: cambia el estado del proceso a `WAITING` y encola la solicitud.
- `completeIO()`: toma la solicitud más antigua y reencola el PID a `READY`, simulando la interrupción.
- La cola FIFO se muestra en la pestaña de Archivos y E/S para seguir el orden de atención.

---
## Diapositiva 7 · Interfaz gráfica
- Swing con pestañas para **Procesos**, **Memoria** y **Archivos/E/S**.
- Controles clave:
  - Botón de `tick` manual y modo automático con `Timer` configurable.
  - Formularios para crear procesos (nombre, ráfaga, memoria) y solicitar E/S.
  - Ajuste del quantum y reinicio global del simulador.
- Panel visual (`PanelPlan`) pinta barras por PID para ver el historial de ejecución.

---

1. Ajustar quantum si se desea.
2. Crear uno o más procesos con ráfaga y memoria requerida.
3. Ejecutar `tick` manual o activar auto-`tick` para observar Round Robin.
4. Lanzar solicitudes de E/S para ver bloqueo/reanudación y completar con `Completar E/S`.
5. Crear/abrir/cerrar archivos para probar control de exclusión por PID.
6. Terminar procesos o reiniciar para repetir la simulación.

---

- Añadir prioridad y envejecimiento al planificador.
- Exponer fallos de asignación de memoria con compactación opcional.
- Simular buffers de disco o tiempos de servicio variables en E/S.
- Persistir el log o exportar métricas de uso.
