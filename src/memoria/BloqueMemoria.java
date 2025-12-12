package memoria;

public class BloqueMemoria {
    public final int inicio;
    public final int tamano;
    public final Integer pid;

    public BloqueMemoria(int inicio, int tamano, Integer pid) {
        this.inicio = inicio;
        this.tamano = tamano;
        this.pid = pid;
    }

    public static BloqueMemoria desdeBloque(BloqueMemoriaInterno bloque) {
        return new BloqueMemoria(bloque.inicio, bloque.tamano, bloque.pid);
    }

    public boolean estaLibre() {
        return pid == null;
    }
}
