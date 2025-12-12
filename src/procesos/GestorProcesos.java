package procesos;

import java.util.*;

public class GestorProcesos {
    private final List<BloqueControlProceso> procesos = new ArrayList<>();
    private final Deque<BloqueControlProceso> colaListos = new ArrayDeque<>();
    private BloqueControlProceso ejecutando;
    private int tiempoCuantoRestante = 0;
    private int cuanto = 2;
    private int siguientePid = 1;
    private int ultimoPidEjecutado = -1;

    public BloqueControlProceso crearProceso(String nombre, int rafaga, int memoriaNecesaria) {
        BloqueControlProceso pcb = new BloqueControlProceso(siguientePid++, nombre, rafaga, memoriaNecesaria);
        procesos.add(pcb);
        return pcb;
    }

    public void encolarListo(BloqueControlProceso pcb) {
        if (pcb.estado == EstadoProceso.TERMINADO) {
            return;
        }
        pcb.estado = EstadoProceso.LISTO;
        colaListos.add(pcb);
    }

    public BloqueControlProceso avanzarCiclo() {
        despacharSiNecesario();
        ultimoPidEjecutado = ejecutando == null ? -1 : ejecutando.pid;
        if (ejecutando == null) {
            return null;
        }
        ejecutando.estado = EstadoProceso.EJECUTANDO;
        ejecutando.tiempoRestante--;
        tiempoCuantoRestante--;
        if (ejecutando.tiempoRestante <= 0) {
            ejecutando.estado = EstadoProceso.TERMINADO;
            BloqueControlProceso finalizado = ejecutando;
            ejecutando = null;
            tiempoCuantoRestante = 0;
            despacharSiNecesario();
            return finalizado;
        }
        if (tiempoCuantoRestante <= 0) {
            ejecutando.estado = EstadoProceso.LISTO;
            colaListos.add(ejecutando);
            ejecutando = null;
            despacharSiNecesario();
        }
        return null;
    }

    private void despacharSiNecesario() {
        if (ejecutando != null) {
            return;
        }
        BloqueControlProceso siguiente = colaListos.poll();
        if (siguiente != null) {
            ejecutando = siguiente;
            tiempoCuantoRestante = cuanto;
            ejecutando.estado = EstadoProceso.EJECUTANDO;
        }
    }

    public boolean bloquearPorES(int pid) {
        BloqueControlProceso pcb = buscar(pid);
        if (pcb == null || pcb.estado == EstadoProceso.BLOQUEADO || pcb.estado == EstadoProceso.TERMINADO) {
            return false;
        }
        if (pcb == ejecutando) {
            ejecutando = null;
            tiempoCuantoRestante = 0;
        } else {
            colaListos.remove(pcb);
        }
        pcb.estado = EstadoProceso.BLOQUEADO;
        return true;
    }

    public boolean reanudarPorES(int pid) {
        BloqueControlProceso pcb = buscar(pid);
        if (pcb == null || pcb.estado != EstadoProceso.BLOQUEADO) {
            return false;
        }
        encolarListo(pcb);
        return true;
    }

    public void terminar(int pid) {
        BloqueControlProceso pcb = buscar(pid);
        if (pcb == null) {
            return;
        }
        pcb.estado = EstadoProceso.TERMINADO;
        colaListos.remove(pcb);
        if (pcb == ejecutando) {
            ejecutando = null;
            tiempoCuantoRestante = 0;
        }
    }

    public void eliminarProceso(int pid) {
        BloqueControlProceso pcb = buscar(pid);
        if (pcb == null) {
            return;
        }
        colaListos.remove(pcb);
        if (pcb == ejecutando) {
            ejecutando = null;
        }
        procesos.remove(pcb);
    }

    public BloqueControlProceso buscar(int pid) {
        for (BloqueControlProceso pcb : procesos) {
            if (pcb.pid == pid) {
                return pcb;
            }
        }
        return null;
    }

    public boolean existeActivo(int pid) {
        BloqueControlProceso pcb = buscar(pid);
        return pcb != null && pcb.estado != EstadoProceso.TERMINADO;
    }

    public List<BloqueControlProceso> obtenerProcesos() {
        return new ArrayList<>(procesos);
    }

    public List<Integer> obtenerPidsActivos() {
        List<Integer> pids = new ArrayList<>();
        for (BloqueControlProceso pcb : procesos) {
            if (pcb.estado != EstadoProceso.TERMINADO) {
                pids.add(pcb.pid);
            }
        }
        return pids;
    }

    public int tamano() {
        return procesos.size();
    }

    public String procesoActual() {
        if (ejecutando == null) {
            return "Ninguno";
        }
        return ejecutando.nombre + " (PID " + ejecutando.pid + ")";
    }

    public int obtenerUltimoPidEjecutado() {
        return ultimoPidEjecutado;
    }

    public int obtenerCuanto() {
        return cuanto;
    }

    public void configurarCuanto(int cuanto) {
        this.cuanto = Math.max(1, cuanto);
        if (ejecutando != null && tiempoCuantoRestante > this.cuanto) {
            tiempoCuantoRestante = this.cuanto;
        }
    }

    public void asociarArchivo(int pid, String nombre) {
        BloqueControlProceso pcb = buscar(pid);
        if (pcb != null) {
            pcb.archivosAbiertos.add(nombre);
        }
    }

    public void desasociarArchivo(int pid, String nombre) {
        BloqueControlProceso pcb = buscar(pid);
        if (pcb != null) {
            pcb.archivosAbiertos.remove(nombre);
        }
    }

    public void limpiarArchivos(int pid) {
        BloqueControlProceso pcb = buscar(pid);
        if (pcb != null) {
            pcb.archivosAbiertos.clear();
        }
    }

    public void reiniciar() {
        procesos.clear();
        colaListos.clear();
        ejecutando = null;
        tiempoCuantoRestante = 0;
        siguientePid = 1;
        ultimoPidEjecutado = -1;
    }
}
