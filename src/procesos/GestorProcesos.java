package procesos;

import java.util.*;

public class GestorProcesos {
    private final List<BloqueControlProceso> processes = new ArrayList<>();
    private final Deque<BloqueControlProceso> readyQueue = new ArrayDeque<>();
    private BloqueControlProceso running;
    private int timeSlice = 0;
    private int quantum = 2;
    private int nextPid = 1;
    private int lastExecutedPid = -1;

    public BloqueControlProceso createProcess(String name, int burstTime, int memoryNeeded) {
        BloqueControlProceso pcb = new BloqueControlProceso(nextPid++, name, burstTime, memoryNeeded);
        processes.add(pcb);
        return pcb;
    }

    public void enqueueReady(BloqueControlProceso pcb) {
        if (pcb.state == EstadoProceso.TERMINATED) {
            return;
        }
        pcb.state = EstadoProceso.READY;
        readyQueue.add(pcb);
    }

    public BloqueControlProceso tick() {
        dispatchIfNeeded();
        lastExecutedPid = running == null ? -1 : running.pid;
        if (running == null) {
            return null;
        }
        running.state = EstadoProceso.RUNNING;
        running.remainingTime--;
        timeSlice--;
        if (running.remainingTime <= 0) {
            running.state = EstadoProceso.TERMINATED;
            BloqueControlProceso finished = running;
            running = null;
            timeSlice = 0;
            dispatchIfNeeded();
            return finished;
        }
        if (timeSlice <= 0) {
            running.state = EstadoProceso.READY;
            readyQueue.add(running);
            running = null;
            dispatchIfNeeded();
        }
        return null;
    }

    private void dispatchIfNeeded() {
        if (running != null) {
            return;
        }
        BloqueControlProceso next = readyQueue.poll();
        if (next != null) {
            running = next;
            timeSlice = quantum;
            running.state = EstadoProceso.RUNNING;
        }
    }

    public boolean blockForIO(int pid) {
        BloqueControlProceso pcb = find(pid);
        if (pcb == null || pcb.state == EstadoProceso.WAITING || pcb.state == EstadoProceso.TERMINATED) {
            return false;
        }
        if (pcb == running) {
            running = null;
            timeSlice = 0;
        } else {
            readyQueue.remove(pcb);
        }
        pcb.state = EstadoProceso.WAITING;
        return true;
    }

    public boolean resumeFromIO(int pid) {
        BloqueControlProceso pcb = find(pid);
        if (pcb == null || pcb.state != EstadoProceso.WAITING) {
            return false;
        }
        enqueueReady(pcb);
        return true;
    }

    public void terminate(int pid) {
        BloqueControlProceso pcb = find(pid);
        if (pcb == null) {
            return;
        }
        pcb.state = EstadoProceso.TERMINATED;
        readyQueue.remove(pcb);
        if (pcb == running) {
            running = null;
            timeSlice = 0;
        }
    }

    public void removeProcess(int pid) {
        BloqueControlProceso pcb = find(pid);
        if (pcb == null) {
            return;
        }
        readyQueue.remove(pcb);
        if (pcb == running) {
            running = null;
        }
        processes.remove(pcb);
    }

    public BloqueControlProceso find(int pid) {
        for (BloqueControlProceso pcb : processes) {
            if (pcb.pid == pid) {
                return pcb;
            }
        }
        return null;
    }

    public boolean existsActive(int pid) {
        BloqueControlProceso pcb = find(pid);
        return pcb != null && pcb.state != EstadoProceso.TERMINATED;
    }

    public List<BloqueControlProceso> getProcesses() {
        return new ArrayList<>(processes);
    }

    public List<Integer> getActivePids() {
        List<Integer> pids = new ArrayList<>();
        for (BloqueControlProceso pcb : processes) {
            if (pcb.state != EstadoProceso.TERMINATED) {
                pids.add(pcb.pid);
            }
        }
        return pids;
    }

    public int size() {
        return processes.size();
    }

    public String currentProcess() {
        if (running == null) {
            return "Ninguno";
        }
        return running.name + " (PID " + running.pid + ")";
    }

    public int getLastExecutedPid() {
        return lastExecutedPid;
    }

    public int getQuantum() {
        return quantum;
    }

    public void setQuantum(int quantum) {
        this.quantum = Math.max(1, quantum);
        if (running != null && timeSlice > this.quantum) {
            timeSlice = this.quantum;
        }
    }

    public void attachFile(int pid, String name) {
        BloqueControlProceso pcb = find(pid);
        if (pcb != null) {
            pcb.openFiles.add(name);
        }
    }

    public void detachFile(int pid, String name) {
        BloqueControlProceso pcb = find(pid);
        if (pcb != null) {
            pcb.openFiles.remove(name);
        }
    }

    public void clearFiles(int pid) {
        BloqueControlProceso pcb = find(pid);
        if (pcb != null) {
            pcb.openFiles.clear();
        }
    }

    public void reset() {
        processes.clear();
        readyQueue.clear();
        running = null;
        timeSlice = 0;
        nextPid = 1;
        lastExecutedPid = -1;
    }
}
