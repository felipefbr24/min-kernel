package dto;

public class ResultadoTick {
    public final String mensaje;
    public final int pid;
    public final String nombre;
    public final boolean inactivo;

    public ResultadoTick(String mensaje, int pid, String nombre, boolean inactivo) {
        this.mensaje = mensaje;
        this.pid = pid;
        this.nombre = nombre;
        this.inactivo = inactivo;
    }
}
