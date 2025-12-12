package memoria;

class BloqueMemoriaInterno {
    int inicio;
    int tamano;
    Integer pid; // null significa libre

    BloqueMemoriaInterno(int inicio, int tamano, Integer pid) {
        this.inicio = inicio;
        this.tamano = tamano;
        this.pid = pid;
    }

    boolean estaLibre() {
        return pid == null;
    }
}
