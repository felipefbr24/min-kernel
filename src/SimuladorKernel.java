import dto.ResultadoTick;
import archivos.*;
import es.GestorES;
import es.SolicitudES;
import memoria.BloqueMemoria;
import memoria.GestorMemoria;
import procesos.*;

import java.util.List;
import java.util.Optional;

public class SimuladorKernel {
    private final GestorProcesos gestorProcesos = new GestorProcesos();
    private final GestorMemoria gestorMemoria = new GestorMemoria(256);
    private final SistemaArchivosSim sistemaArchivos = new SistemaArchivosSim();
    private final GestorES gestorES = new GestorES();

    public String crearProceso(String nombre, int rafaga, int memoria) {
        if (nombre.isEmpty()) {
            nombre = "Proceso " + (gestorProcesos.tamano() + 1);
        }
        BloqueControlProceso pcb = gestorProcesos.crearProceso(nombre, rafaga, memoria);
        boolean asignado = gestorMemoria.asignar(pcb.pid, memoria);
        if (!asignado) {
            gestorProcesos.eliminarProceso(pcb.pid);
            return "No hay memoria suficiente para " + nombre;
        }
        gestorProcesos.encolarListo(pcb);
        return "Creado " + nombre + " (PID " + pcb.pid + "), memoria asignada: " + memoria + " KB";
    }

    public ResultadoTick avanzarTick() {
        StringBuilder resultado = new StringBuilder();
        BloqueControlProceso finalizado = gestorProcesos.avanzarCiclo();
        if (finalizado != null) {
            gestorMemoria.liberarPorPid(finalizado.pid);
            sistemaArchivos.cerrarTodoPorPid(finalizado.pid);
            gestorProcesos.limpiarArchivos(finalizado.pid);
            resultado.append("Proceso PID ").append(finalizado.pid).append(" terminado, memoria liberada");
        } else {
            resultado.append("Ciclo ejecutado. En ejecución: ").append(gestorProcesos.procesoActual());
        }
        int pidEjecutado = gestorProcesos.obtenerUltimoPidEjecutado();
        String nombreEjecutado = pidEjecutado == -1 ? "Inactivo" :
                Optional.ofNullable(gestorProcesos.buscar(pidEjecutado)).map(p -> p.nombre).orElse("PID " + pidEjecutado);
        return new ResultadoTick(resultado.toString(), pidEjecutado, nombreEjecutado, pidEjecutado == -1);
    }

    public String solicitarES(int pid, String dispositivo, String detalle) {
        BloqueControlProceso pcb = gestorProcesos.buscar(pid);
        if (pcb == null) {
            return "PID no válido para E/S";
        }
        if (pcb.estado == EstadoProceso.TERMINADO) {
            return "El proceso ya terminó";
        }
        if (!gestorProcesos.bloquearPorES(pid)) {
            return "El proceso no está listo/ejecutando";
        }
        gestorES.solicitar(new SolicitudES(pid, dispositivo, detalle));
        return "PID " + pid + " solicita E/S en " + dispositivo;
    }

    public String completarES() {
        SolicitudES solicitud = gestorES.completarSiguiente();
        if (solicitud == null) {
            return "No hay solicitudes de E/S pendientes";
        }
        gestorProcesos.reanudarPorES(solicitud.pid);
        return "Interrupción de E/S completada para PID " + solicitud.pid + " en " + solicitud.dispositivo;
    }

    public String crearArchivo(int pid, String nombre) {
        if (nombre.isEmpty()) {
            return "Nombre de archivo vacío";
        }
        if (!gestorProcesos.existeActivo(pid)) {
            return "PID no válido para crear archivo";
        }
        if (sistemaArchivos.crearArchivo(pid, nombre)) {
            return "Archivo " + nombre + " creado por PID " + pid;
        }
        return "El archivo ya existe";
    }

    public String abrirArchivo(int pid, String nombre) {
        if (!gestorProcesos.existeActivo(pid)) {
            return "PID no válido para abrir archivo";
        }
        EstadoApertura estado = sistemaArchivos.abrirArchivo(pid, nombre);
        switch (estado) {
            case EXITO:
                gestorProcesos.asociarArchivo(pid, nombre);
                return "PID " + pid + " abre " + nombre;
            case EN_USO:
                Integer actual = sistemaArchivos.abiertoPor(nombre);
                return "El archivo ya está abierto por PID " + (actual == null ? "desconocido" : actual);
            case NO_ENCONTRADO:
            default:
                return "El archivo no existe";
        }
    }

    public String cerrarArchivo(int pid, String nombre) {
        EstadoCierre estado = sistemaArchivos.cerrarArchivo(pid, nombre);
        switch (estado) {
            case EXITO:
                gestorProcesos.desasociarArchivo(pid, nombre);
                return "Archivo " + nombre + " cerrado por PID " + pid;
            case NO_PROPIETARIO:
                return "El archivo está abierto por otro PID";
            case SIN_APERTURA:
                return "El archivo no está abierto";
            case NO_ENCONTRADO:
            default:
                return "El archivo no existe";
        }
    }

    public String forzarTerminacion(int pid) {
        BloqueControlProceso pcb = gestorProcesos.buscar(pid);
        if (pcb == null) {
            return "PID no encontrado";
        }
        gestorMemoria.liberarPorPid(pid);
        sistemaArchivos.cerrarTodoPorPid(pid);
        gestorProcesos.terminar(pid);
        gestorProcesos.limpiarArchivos(pid);
        return "PID " + pid + " terminado manualmente";
    }

    public List<BloqueControlProceso> obtenerProcesos() { return gestorProcesos.obtenerProcesos(); }
    public List<BloqueMemoria> obtenerBloquesMemoria() { return gestorMemoria.obtenerBloques(); }
    public List<EntradaArchivo> obtenerArchivos() { return sistemaArchivos.obtenerArchivos(); }
    public List<SolicitudES> obtenerColaES() { return gestorES.obtenerCola(); }
    public List<Integer> obtenerPidsSeleccionables() { return gestorProcesos.obtenerPidsActivos(); }
    public int obtenerMemoriaTotal() { return gestorMemoria.obtenerTamanoTotal(); }
    public int obtenerMemoriaUsada() { return gestorMemoria.obtenerTamanoUsado(); }
    public int obtenerMemoriaLibre() { return gestorMemoria.obtenerTamanoLibre(); }
    public int obtenerCuanto() { return gestorProcesos.obtenerCuanto(); }
    public void configurarCuanto(int cuanto) { gestorProcesos.configurarCuanto(cuanto); }

    public void reiniciar() {
        gestorProcesos.reiniciar();
        gestorMemoria.reiniciar();
        sistemaArchivos.reiniciar();
        gestorES.reiniciar();
    }
}
