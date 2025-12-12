package es;

public class SolicitudES {
    public final int pid;
    public final String dispositivo;
    public final String detalle;

    public SolicitudES(int pid, String dispositivo, String detalle) {
        this.pid = pid;
        this.dispositivo = dispositivo;
        this.detalle = detalle;
    }
}
