package dto;

public class ResultadoTick {
    public final String message;
    public final int pid;
    public final String name;
    public final boolean idle;

    public ResultadoTick(String message, int pid, String name, boolean idle) {
        this.message = message;
        this.pid = pid;
        this.name = name;
        this.idle = idle;
    }
}
