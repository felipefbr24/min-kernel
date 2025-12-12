package es;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class GestorES {
    private final Deque<SolicitudES> cola = new ArrayDeque<>();

    public void solicitar(SolicitudES solicitud) {
        cola.add(solicitud);
    }

    public SolicitudES completarSiguiente() {
        return cola.poll();
    }

    public List<SolicitudES> obtenerCola() {
        return new ArrayList<>(cola);
    }

    public void reiniciar() {
        cola.clear();
    }
}
