package archivos;

import java.util.*;

public class SistemaArchivosSim {
    private final Map<String, EntradaArchivo> archivos = new LinkedHashMap<>();
    private final Map<Integer, Set<String>> abiertosPorPid = new HashMap<>();

    public boolean crearArchivo(int pid, String nombre) {
        if (archivos.containsKey(nombre)) {
            return false;
        }
        archivos.put(nombre, new EntradaArchivo(nombre, pid));
        return true;
    }

    public EstadoApertura abrirArchivo(int pid, String nombre) {
        EntradaArchivo entrada = archivos.get(nombre);
        if (entrada == null) {
            return EstadoApertura.NO_ENCONTRADO;
        }
        if (entrada.abiertoPorPid != null) {
            return EstadoApertura.EN_USO;
        }
        entrada.abiertoPorPid = pid;
        entrada.cantidadAperturas = 1;
        abiertosPorPid.computeIfAbsent(pid, k -> new HashSet<>()).add(nombre);
        return EstadoApertura.EXITO;
    }

    public EstadoCierre cerrarArchivo(int pid, String nombre) {
        EntradaArchivo entrada = archivos.get(nombre);
        Set<String> abiertos = abiertosPorPid.get(pid);
        if (entrada == null) {
            return EstadoCierre.NO_ENCONTRADO;
        }
        if (entrada.abiertoPorPid == null) {
            return EstadoCierre.SIN_APERTURA;
        }
        if (!Objects.equals(entrada.abiertoPorPid, pid)) {
            return EstadoCierre.NO_PROPIETARIO;
        }
        entrada.cantidadAperturas = 0;
        entrada.abiertoPorPid = null;
        if (abiertos != null) {
            abiertos.remove(nombre);
            if (abiertos.isEmpty()) {
                abiertosPorPid.remove(pid);
            }
        }
        return EstadoCierre.EXITO;
    }

    public void cerrarTodoPorPid(int pid) {
        Set<String> abiertos = abiertosPorPid.remove(pid);
        if (abiertos == null) {
            return;
        }
        for (String nombre : abiertos) {
            EntradaArchivo entrada = archivos.get(nombre);
            if (entrada != null && entrada.cantidadAperturas > 0 && Objects.equals(entrada.abiertoPorPid, pid)) {
                entrada.cantidadAperturas = 0;
                entrada.abiertoPorPid = null;
            }
        }
    }

    public List<EntradaArchivo> obtenerArchivos() {
        return new ArrayList<>(archivos.values());
    }

    public void reiniciar() {
        archivos.clear();
        abiertosPorPid.clear();
    }

    public Integer abiertoPor(String nombre) {
        EntradaArchivo entrada = archivos.get(nombre);
        return entrada == null ? null : entrada.abiertoPorPid;
    }
}
