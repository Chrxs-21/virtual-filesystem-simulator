package filesystem.scheduler;

import filesystem.datastructures.LinkedList;
import filesystem.datastructures.Node;
import filesystem.datastructures.ProcessQueue;

/**
 * Planificador de disco que implementa 4 políticas de scheduling:
 * FIFO, SSTF, SCAN y C-SCAN.
 *
 * Gestiona una cola de solicitudes de E/S usando estructuras propias.
 * No usa ninguna clase del Java Collections Framework.
 */
public class DiskScheduler {

    /** Posición máxima de cilindro del disco simulado. */
    public static final int MAX_CYLINDER = 199;

    /** Posición actual del cabezal del disco. */
    private int headPosition;

    /** Política de planificación activa. */
    private SchedulerPolicy policy;

    /**
     * Cola de solicitudes pendientes.
     * Usa ProcessQueue propia, no java.util.Queue.
     */
    private final ProcessQueue<IORequest> requestQueue;

    /**
     * Historial de solicitudes atendidas en orden.
     * Usa LinkedList propia, no ArrayList.
     */
    private final LinkedList<IORequest> attendedRequests;

    /**
     * Registro del movimiento total del cabezal
     * para análisis comparativo entre políticas.
     */
    private int totalHeadMovement;

    /**
     * Dirección actual del cabezal para SCAN y C-SCAN.
     * true = hacia cilindros mayores, false = hacia menores.
     */
    private boolean movingUp;

    /**
     * Crea un planificador con posición inicial del cabezal
     * y política configurables.
     *
     * @param initialHeadPosition Posición inicial del cabezal (0 a MAX_CYLINDER).
     * @param policy              Política de planificación a usar.
     */
    public DiskScheduler(int initialHeadPosition, SchedulerPolicy policy) {
        validateCylinder(initialHeadPosition);
        this.headPosition      = initialHeadPosition;
        this.policy            = policy;
        this.requestQueue      = new ProcessQueue<>();
        this.attendedRequests  = new LinkedList<>();
        this.totalHeadMovement = 0;
        this.movingUp          = true;
    }

    // ─── GESTIÓN DE SOLICITUDES ─────────────────────────────────────────────

    /**
     * Agrega una nueva solicitud de E/S a la cola.
     * @param request La solicitud a encolar.
     */
    public void addRequest(IORequest request) {
        requestQueue.enqueue(request);
    }

    /**
     * Procesa la siguiente solicitud según la política activa.
     * Mueve el cabezal, actualiza el historial y retorna
     * la solicitud atendida.
     *
     * @return La solicitud que fue atendida.
     * @throws IllegalStateException si no hay solicitudes pendientes.
     */
    public IORequest processNext() {
        if (requestQueue.isEmpty()) {
            throw new IllegalStateException(
                "No hay solicitudes pendientes en la cola."
            );
        }

        IORequest next = switch (policy) {
            case FIFO   -> processFIFO();
            case SSTF   -> processSSTF();
            case SCAN   -> processSCAN();
            case C_SCAN -> processCSTF();
        };

        // Mover el cabezal y registrar movimiento
        int movement = Math.abs(next.getCylinderPosition() - headPosition);
        totalHeadMovement += movement;
        headPosition = next.getCylinderPosition();

        // Actualizar estado y registrar en historial
        next.setStatus(IORequest.RequestStatus.COMPLETED);
        attendedRequests.addLast(next);

        return next;
    }

    /**
     * Procesa todas las solicitudes pendientes de una vez.
     * @return Lista con el orden en que fueron atendidas.
     */
    public LinkedList<IORequest> processAll() {
        while (!requestQueue.isEmpty()) {
            processNext();
        }
        return attendedRequests;
    }

    // ─── IMPLEMENTACIONES DE POLÍTICAS ──────────────────────────────────────

    /**
     * FIFO: atiende en orden de llegada.
     * Extrae directamente del frente de la cola.
     */
    private IORequest processFIFO() {
        return requestQueue.dequeue();
    }

    /**
     * SSTF: atiende la solicitud más cercana al cabezal actual.
     * Recorre toda la cola para encontrar la de menor distancia.
     */
    private IORequest processSSTF() {
        // Convertir la cola a una lista temporal para poder buscar
        LinkedList<IORequest> temp = queueToList();

        IORequest closest = null;
        int minDistance = Integer.MAX_VALUE;

        Node<IORequest> current = temp.getHead();
        while (current != null) {
            int distance = Math.abs(
                current.data.getCylinderPosition() - headPosition
            );
            if (distance < minDistance) {
                minDistance = distance;
                closest = current.data;
            }
            current = current.next;
        }

        // Reconstruir la cola sin la solicitud elegida
        rebuildQueue(temp, closest);
        return closest;
    }

    /**
 * SCAN: el cabezal se mueve en una dirección atendiendo solicitudes,
 * llega hasta la última solicitud en esa dirección (no necesariamente
 * el extremo), y regresa en dirección contraria.
 */
private IORequest processSCAN() {
    LinkedList<IORequest> temp = queueToList();

    IORequest best = findNextInDirection(temp, movingUp);

    if (best == null) {
        movingUp = !movingUp;
        best = findNextInDirection(temp, movingUp);
    }

    rebuildQueue(temp, best);
    return best;
}

    /**
 * C-SCAN: el cabezal se mueve solo en dirección ascendente.
 * Cuando no hay más solicitudes hacia arriba, salta directamente
 * al cilindro más bajo disponible sin contar ese salto como
 * movimiento de cabezal real.
 */
private IORequest processCSTF() {
    LinkedList<IORequest> temp = queueToList();

    // Buscar la siguiente solicitud en dirección ascendente
    IORequest best = findNextInDirection(temp, true);

    if (best == null) {
        // Saltar al inicio sin sumar movimiento
        headPosition = 0;
        best = findNextInDirection(temp, true);
    }

    if (best == null && temp.getHead() != null) {
        best = temp.getHead().data;
    }

    rebuildQueue(temp, best);
    return best;
}

    // ─── MÉTODOS AUXILIARES ─────────────────────────────────────────────────

    /**
     * Encuentra la solicitud más cercana al cabezal
     * en la dirección especificada.
     *
     * @param list    Lista de solicitudes disponibles.
     * @param goingUp true = buscar en cilindros mayores,
     *                false = buscar en cilindros menores.
     * @return La solicitud más cercana en esa dirección, o null si no hay.
     */
    private IORequest findNextInDirection(LinkedList<IORequest> list,
                                          boolean goingUp) {
        IORequest best = null;
        int bestDistance = Integer.MAX_VALUE;

        Node<IORequest> current = list.getHead();
        while (current != null) {
            int cyl = current.data.getCylinderPosition();
            boolean inDirection = goingUp
                ? cyl >= headPosition
                : cyl <= headPosition;

            if (inDirection) {
                int distance = Math.abs(cyl - headPosition);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = current.data;
                }
            }
            current = current.next;
        }
        return best;
    }

    /**
     * Convierte la cola de solicitudes a una lista enlazada temporal
     * para poder recorrerla y buscar sin destruirla.
     * Vacía la cola en el proceso.
     *
     * @return Lista con todas las solicitudes de la cola.
     */
    private LinkedList<IORequest> queueToList() {
        LinkedList<IORequest> temp = new LinkedList<>();
        while (!requestQueue.isEmpty()) {
            temp.addLast(requestQueue.dequeue());
        }
        return temp;
    }

    /**
     * Reconstruye la cola con todas las solicitudes de la lista
     * excepto la elegida para ser atendida.
     *
     * @param list    Lista temporal con todas las solicitudes.
     * @param exclude La solicitud que fue elegida y no debe volver.
     */
    private void rebuildQueue(LinkedList<IORequest> list,
                               IORequest exclude) {
        Node<IORequest> current = list.getHead();
        while (current != null) {
            if (current.data != exclude) {
                requestQueue.enqueue(current.data);
            }
            current = current.next;
        }
    }

    /**
     * Valida que una posición de cilindro sea válida.
     * @param position Posición a validar.
     * @throws IllegalArgumentException si está fuera de rango.
     */
    private void validateCylinder(int position) {
        if (position < 0 || position > MAX_CYLINDER) {
            throw new IllegalArgumentException(
                "Posición de cilindro inválida: " + position
                + ". Rango válido: 0 a " + MAX_CYLINDER
            );
        }
    }

    // ─── GETTERS ────────────────────────────────────────────────────────────

    public int getHeadPosition()          { return headPosition; }
    public SchedulerPolicy getPolicy()    { return policy; }
    public int getTotalHeadMovement()     { return totalHeadMovement; }
    public boolean isQueueEmpty()         { return requestQueue.isEmpty(); }
    public int getPendingCount()          { return requestQueue.size(); }
    public LinkedList<IORequest> getAttendedRequests() { return attendedRequests; }

    /**
     * Cambia la política de planificación y reinicia el estado.
     * @param policy Nueva política a aplicar.
     */
    public void setPolicy(SchedulerPolicy policy) {
        this.policy            = policy;
        this.totalHeadMovement = 0;
        this.movingUp          = true;
        attendedRequests.clear();
    }

    /**
     * Reinicia el planificador completamente.
     * @param newHeadPosition Nueva posición inicial del cabezal.
     */
    public void reset(int newHeadPosition) {
        validateCylinder(newHeadPosition);
        this.headPosition      = newHeadPosition;
        this.totalHeadMovement = 0;
        this.movingUp          = true;
        attendedRequests.clear();
        requestQueue.clear();
    }
}