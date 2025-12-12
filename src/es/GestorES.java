package es;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class GestorES {
    private final Deque<SolicitudES> queue = new ArrayDeque<>();

    public void request(SolicitudES request) {
        queue.add(request);
    }

    public SolicitudES completeNext() {
        return queue.poll();
    }

    public List<SolicitudES> getQueue() {
        return new ArrayList<>(queue);
    }

    public void reset() {
        queue.clear();
    }
}
