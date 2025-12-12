package archivos;

public class EntradaArchivo {
    public final String nombre;
    public final int pidPropietario;
    public int cantidadAperturas = 0;
    public Integer abiertoPorPid = null;

    public EntradaArchivo(String nombre, int pidPropietario) {
        this.nombre = nombre;
        this.pidPropietario = pidPropietario;
    }
}
