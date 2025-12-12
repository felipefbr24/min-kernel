package memoria;

import java.util.*;

public class GestorMemoria {
    private final List<BloqueMemoriaInterno> bloques = new ArrayList<>();
    private final int tamanoTotal;

    public GestorMemoria(int tamanoTotal) {
        this.tamanoTotal = tamanoTotal;
        bloques.add(new BloqueMemoriaInterno(0, tamanoTotal, null));
    }

    public boolean asignar(int pid, int tamano) {
        for (int i = 0; i < bloques.size(); i++) {
            BloqueMemoriaInterno bloque = bloques.get(i);
            if (bloque.estaLibre() && bloque.tamano >= tamano) {
                BloqueMemoriaInterno usado = new BloqueMemoriaInterno(bloque.inicio, tamano, pid);
                if (bloque.tamano == tamano) {
                    bloques.set(i, usado);
                } else {
                    bloque.inicio += tamano;
                    bloque.tamano -= tamano;
                    bloques.add(i, usado);
                }
                return true;
            }
        }
        return false;
    }

    public void liberarPorPid(int pid) {
        for (BloqueMemoriaInterno bloque : bloques) {
            if (Objects.equals(bloque.pid, pid)) {
                bloque.pid = null;
            }
        }
        fusionarBloquesLibres();
    }

    private void fusionarBloquesLibres() {
        Collections.sort(bloques, Comparator.comparingInt(b -> b.inicio));
        for (int i = 0; i < bloques.size() - 1; ) {
            BloqueMemoriaInterno actual = bloques.get(i);
            BloqueMemoriaInterno siguiente = bloques.get(i + 1);
            if (actual.estaLibre() && siguiente.estaLibre()) {
                actual.tamano += siguiente.tamano;
                bloques.remove(i + 1);
            } else {
                i++;
            }
        }
        if (bloques.isEmpty()) {
            bloques.add(new BloqueMemoriaInterno(0, tamanoTotal, null));
        }
    }

    public List<BloqueMemoria> obtenerBloques() {
        Collections.sort(bloques, Comparator.comparingInt(b -> b.inicio));
        List<BloqueMemoria> lista = new ArrayList<>();
        for (BloqueMemoriaInterno bloque : bloques) {
            lista.add(BloqueMemoria.desdeBloque(bloque));
        }
        return lista;
    }

    public int obtenerTamanoTotal() {
        return tamanoTotal;
    }

    public int obtenerTamanoUsado() {
        int usado = 0;
        for (BloqueMemoriaInterno bloque : bloques) {
            if (!bloque.estaLibre()) {
                usado += bloque.tamano;
            }
        }
        return usado;
    }

    public int obtenerTamanoLibre() {
        return tamanoTotal - obtenerTamanoUsado();
    }

    public void reiniciar() {
        bloques.clear();
        bloques.add(new BloqueMemoriaInterno(0, tamanoTotal, null));
    }
}
