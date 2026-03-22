package filesystem.datastructures;

/**
 * Cola FIFO de procesos implementada sobre LinkedList propia.
 *
 * Reemplaza a java.util.Queue y a java.util.LinkedList usada como cola.
 * Solo permite insertar por el final (enqueue) y extraer por el frente
 * (dequeue), garantizando el orden de llegada.
 *
 * Usada para: cola de E/S del planificador y procesos bloqueados
 * esperando un lock.
 *
 * @param <T> Tipo de proceso o solicitud almacenada.
 */
public class ProcessQueue<T> {

    /** Lista interna que implementa la semántica FIFO. */
    private final LinkedList<T> list;

    /** Crea una cola vacía. */
    public ProcessQueue() {
        this.list = new LinkedList<>();
    }

    // ─── OPERACIONES PRINCIPALES ────────────────────────────────────────────

    /**
     * Inserta un elemento al final de la cola. O(1).
     * @param data El proceso o solicitud a encolar.
     */
    public void enqueue(T data) {
        list.addLast(data);
    }

    /**
     * Extrae y retorna el elemento del frente de la cola. O(1).
     * @return El proceso más antiguo en espera.
     * @throws IllegalStateException si la cola está vacía.
     */
    public T dequeue() {
        if (list.isEmpty()) {
            throw new IllegalStateException(
                "La cola de procesos está vacía."
            );
        }
        return list.removeFirst();
    }

    /**
     * Consulta el frente de la cola sin extraerlo. O(1).
     * @return El primer elemento en espera.
     * @throws IllegalStateException si la cola está vacía.
     */
    public T peek() {
        if (list.isEmpty()) {
            throw new IllegalStateException(
                "La cola de procesos está vacía."
            );
        }
        return list.getFirst();
    }

    // ─── UTILIDADES ─────────────────────────────────────────────────────────

    /** @return true si la cola no tiene elementos. */
    public boolean isEmpty() { return list.isEmpty(); }

    /** @return Cantidad de elementos en la cola. */
    public int size() { return list.size(); }

    /**
     * Referencia al head interno para iteración en la GUI
     * sin necesidad de extraer los elementos.
     * @return El primer nodo, o null si la cola está vacía.
     */
    public Node<T> getHead() { return list.getHead(); }

    /**
     * Elimina todos los elementos de la cola.
     */
    public void clear() { list.clear(); }

    @Override
    public String toString() { return list.toString(); }
}