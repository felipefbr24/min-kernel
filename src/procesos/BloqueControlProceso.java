package procesos;

import java.util.HashSet;
import java.util.Set;

public class BloqueControlProceso {
    public final int pid;
    public final String name;
    public final int burstTime;
    public final int memoryNeeded;
    public int remainingTime;
    public EstadoProceso state = EstadoProceso.NEW;
    public final Set<String> openFiles = new HashSet<>();

    public BloqueControlProceso(int pid, String name, int burstTime, int memoryNeeded) {
        this.pid = pid;
        this.name = name;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.memoryNeeded = memoryNeeded;
    }
}
