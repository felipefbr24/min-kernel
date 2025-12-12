package archivos;

public class EntradaArchivo {
    public final String name;
    public final int ownerPid;
    public int openCount = 0;
    public Integer openedByPid = null;

    public EntradaArchivo(String name, int ownerPid) {
        this.name = name;
        this.ownerPid = ownerPid;
    }
}
