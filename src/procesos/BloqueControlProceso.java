package procesos;

import java.util.HashSet;
import java.util.Set;

public class BloqueControlProceso {
    public final int pid;
    public final String nombre;
    public final int rafaga;
    public final int memoriaNecesaria;
    public int tiempoRestante;
    public EstadoProceso estado = EstadoProceso.NUEVO;
    public final Set<String> archivosAbiertos = new HashSet<>();

    public BloqueControlProceso(int pid, String nombre, int rafaga, int memoriaNecesaria) {
        this.pid = pid;
        this.nombre = nombre;
        this.rafaga = rafaga;
        this.tiempoRestante = rafaga;
        this.memoriaNecesaria = memoriaNecesaria;
    }
}
