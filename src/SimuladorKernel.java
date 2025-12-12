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
    private final GestorProcesos processManager = new GestorProcesos();
    private final GestorMemoria memoryManager = new GestorMemoria(256);
    private final SistemaArchivosSim fileSystem = new SistemaArchivosSim();
    private final GestorES ioManager = new GestorES();

    public String createProcess(String name, int burstTime, int memory) {
        if (name.isEmpty()) {
            name = "Proceso " + (processManager.size() + 1);
        }
        BloqueControlProceso pcb = processManager.createProcess(name, burstTime, memory);
        boolean allocated = memoryManager.allocate(pcb.pid, memory);
        if (!allocated) {
            processManager.removeProcess(pcb.pid);
            return "No hay memoria suficiente para " + name;
        }
        processManager.enqueueReady(pcb);
        return "Creado " + name + " (PID " + pcb.pid + "), memoria asignada: " + memory + " KB";
    }

    public ResultadoTick tick() {
        StringBuilder result = new StringBuilder();
        BloqueControlProceso finished = processManager.tick();
        if (finished != null) {
            memoryManager.freeByPid(finished.pid);
            fileSystem.closeAllForPid(finished.pid);
            processManager.clearFiles(finished.pid);
            result.append("Proceso PID ").append(finished.pid).append(" terminado, memoria liberada");
        } else {
            result.append("Ciclo ejecutado. En ejecución: ").append(processManager.currentProcess());
        }
        int executedPid = processManager.getLastExecutedPid();
        String executedName = executedPid == -1 ? "Idle" :
                Optional.ofNullable(processManager.find(executedPid)).map(p -> p.name).orElse("PID " + executedPid);
        return new ResultadoTick(result.toString(), executedPid, executedName, executedPid == -1);
    }

    public String requestIO(int pid, String device, String detail) {
        BloqueControlProceso pcb = processManager.find(pid);
        if (pcb == null) {
            return "PID no válido para E/S";
        }
        if (pcb.state == EstadoProceso.TERMINATED) {
            return "El proceso ya terminó";
        }
        if (!processManager.blockForIO(pid)) {
            return "El proceso no está listo/ejecutando";
        }
        ioManager.request(new SolicitudES(pid, device, detail));
        return "PID " + pid + " solicita E/S en " + device;
    }

    public String completeIO() {
        SolicitudES req = ioManager.completeNext();
        if (req == null) {
            return "No hay solicitudes de E/S pendientes";
        }
        processManager.resumeFromIO(req.pid);
        return "Interrupción de E/S completada para PID " + req.pid + " en " + req.device;
    }

    public String createFile(int pid, String name) {
        if (name.isEmpty()) {
            return "Nombre de archivo vacío";
        }
        if (!processManager.existsActive(pid)) {
            return "PID no válido para crear archivo";
        }
        if (fileSystem.createFile(pid, name)) {
            return "Archivo " + name + " creado por PID " + pid;
        }
        return "El archivo ya existe";
    }

    public String openFile(int pid, String name) {
        if (!processManager.existsActive(pid)) {
            return "PID no válido para abrir archivo";
        }
        EstadoApertura status = fileSystem.openFile(pid, name);
        switch (status) {
            case SUCCESS:
                processManager.attachFile(pid, name);
                return "PID " + pid + " abre " + name;
            case IN_USE:
                Integer holder = fileSystem.openedBy(name);
                return "El archivo ya está abierto por PID " + (holder == null ? "desconocido" : holder);
            case NOT_FOUND:
            default:
                return "El archivo no existe";
        }
    }

    public String closeFile(int pid, String name) {
        EstadoCierre status = fileSystem.closeFile(pid, name);
        switch (status) {
            case SUCCESS:
                processManager.detachFile(pid, name);
                return "Archivo " + name + " cerrado por PID " + pid;
            case NOT_OWNER:
                return "El archivo está abierto por otro PID";
            case NO_OPEN:
                return "El archivo no está abierto";
            case NOT_FOUND:
            default:
                return "El archivo no existe";
        }
    }

    public String forceTerminate(int pid) {
        BloqueControlProceso pcb = processManager.find(pid);
        if (pcb == null) {
            return "PID no encontrado";
        }
        memoryManager.freeByPid(pid);
        fileSystem.closeAllForPid(pid);
        processManager.terminate(pid);
        processManager.clearFiles(pid);
        return "PID " + pid + " terminado manualmente";
    }

    public List<BloqueControlProceso> getProcesses() { return processManager.getProcesses(); }
    public List<BloqueMemoria> getMemoryBlocks() { return memoryManager.getBlocks(); }
    public List<EntradaArchivo> getFiles() { return fileSystem.getFiles(); }
    public List<SolicitudES> getIoQueue() { return ioManager.getQueue(); }
    public List<Integer> getSelectablePids() { return processManager.getActivePids(); }
    public int getTotalMemory() { return memoryManager.getTotalSize(); }
    public int getUsedMemory() { return memoryManager.getUsedSize(); }
    public int getFreeMemory() { return memoryManager.getFreeSize(); }
    public int getQuantum() { return processManager.getQuantum(); }
    public void setQuantum(int quantum) { processManager.setQuantum(quantum); }

    public void reset() {
        processManager.reset();
        memoryManager.reset();
        fileSystem.reset();
        ioManager.reset();
    }
}
