package archivos;

import java.util.*;

public class SistemaArchivosSim {
    private final Map<String, EntradaArchivo> files = new LinkedHashMap<>();
    private final Map<Integer, Set<String>> openByPid = new HashMap<>();

    public boolean createFile(int pid, String name) {
        if (files.containsKey(name)) {
            return false;
        }
        files.put(name, new EntradaArchivo(name, pid));
        return true;
    }

    public EstadoApertura openFile(int pid, String name) {
        EntradaArchivo entry = files.get(name);
        if (entry == null) {
            return EstadoApertura.NOT_FOUND;
        }
        if (entry.openedByPid != null) {
            return EstadoApertura.IN_USE;
        }
        entry.openedByPid = pid;
        entry.openCount = 1;
        openByPid.computeIfAbsent(pid, k -> new HashSet<>()).add(name);
        return EstadoApertura.SUCCESS;
    }

    public EstadoCierre closeFile(int pid, String name) {
        EntradaArchivo entry = files.get(name);
        Set<String> opened = openByPid.get(pid);
        if (entry == null) {
            return EstadoCierre.NOT_FOUND;
        }
        if (entry.openedByPid == null) {
            return EstadoCierre.NO_OPEN;
        }
        if (!Objects.equals(entry.openedByPid, pid)) {
            return EstadoCierre.NOT_OWNER;
        }
        entry.openCount = 0;
        entry.openedByPid = null;
        if (opened != null) {
            opened.remove(name);
            if (opened.isEmpty()) {
                openByPid.remove(pid);
            }
        }
        return EstadoCierre.SUCCESS;
    }

    public void closeAllForPid(int pid) {
        Set<String> opened = openByPid.remove(pid);
        if (opened == null) {
            return;
        }
        for (String name : opened) {
            EntradaArchivo entry = files.get(name);
            if (entry != null && entry.openCount > 0 && Objects.equals(entry.openedByPid, pid)) {
                entry.openCount = 0;
                entry.openedByPid = null;
            }
        }
    }

    public List<EntradaArchivo> getFiles() {
        return new ArrayList<>(files.values());
    }

    public void reset() {
        files.clear();
        openByPid.clear();
    }

    public Integer openedBy(String name) {
        EntradaArchivo entry = files.get(name);
        return entry == null ? null : entry.openedByPid;
    }
}
