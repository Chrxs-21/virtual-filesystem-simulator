package filesystem.scheduler;

import filesystem.datastructures.LinkedList;
import filesystem.datastructures.Node;
import filesystem.datastructures.ProcessQueue;
import java.util.concurrent.Semaphore;

/**
 * Planificador de disco que implementa 4 politicas de scheduling:
 * FIFO, SSTF, SCAN y C-SCAN.
 *
 * Gestiona una cola de solicitudes de E/S usando estructuras propias.
 * Usa un Semaphore para garantizar acceso exclusivo al disco
 * durante cada operacion, coordinado con DiskSchedulerThread.
 * No usa ninguna clase del Java Collections Framework.
 */
public class DiskScheduler {

    /** Posicion maxima de cilindro del disco simulado. */
    public static final int MAX_CYLINDER = 199;

    /** Posicion actual del cabezal del disco. */
    private int headPosition;

    /** Politica de planificacion activa. */
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
     * para analisis comparativo entre politicas.
     */
    private int totalHeadMovement;

    /**
     * Direccion actual del cabezal para SCAN y C-SCAN.
     * true = hacia cilindros mayores, false = hacia menores.
     */
    private boolean movingUp;

    /**
     * Semaforo de acceso al disco.
     * Inicializado con 1 permiso para comportamiento de mutex.
     * Accesible por DiskSchedulerThread para coordinar el acceso.
     */
    private final Semaphore diskSemaphore;

    /**
     * Crea un planificador con posicion inicial del cabezal
     * y politica configurables.
     *
     * @param initialHeadPosition Posicion inicial (0 a MAX_CYLINDER).
     * @param policy              Politica de planificacion a usar.
     */
    public DiskScheduler(int initialHeadPosition,
                         SchedulerPolicy policy) {
        validateCylinder(initialHeadPosition);
        this.headPosition      = initialHeadPosition;
        this.policy            = policy;
        this.requestQueue      = new ProcessQueue<>();
        this.attendedRequests  = new LinkedList<>();
        this.totalHeadMovement = 0;
        this.movingUp          = true;
        this.diskSemaphore     = new Semaphore(1);
    }

    // ─── GESTION DE SOLICITUDES ─────────────────────────────────────────────

    /**
     * Agrega una nueva solicitud de E/S a la cola.
     * @param request La solicitud a encolar.
     */
    public void addRequest(IORequest request) {
        requestQueue.enqueue(request);
    }

    /**
     * Procesa la siguiente solicitud segun la politica activa.
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
        int movement = Math.abs(
            next.getCylinderPosition() - headPosition
        );
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

    // ─── IMPLEMENTACIONES DE POLITICAS ──────────────────────────────────────

    /**
     * FIFO: atiende en orden de llegada.
     * Extrae directamente del frente de la cola.
     */
    private IORequest processFIFO() {
        return requestQueue.dequeue();
    }

    /**
     * SSTF: atiende la solicitud mas cercana al cabezal actual.
     * Recorre toda la cola para encontrar la de menor distancia.
     */
    private IORequest processSSTF() {
        LinkedList<IORequest> temp = queueToList();

        IORequest closest    = null;
        int minDistance      = Integer.MAX_VALUE;

        Node<IORequest> current = temp.getHead();
        while (current != null) {
            int distance = Math.abs(
                current.data.getCylinderPosition() - headPosition
            );
            if (distance < minDistance) {
                minDistance = distance;
                closest     = current.data;
            }
            current = current.next;
        }

        rebuildQueue(temp, closest);
        return closest;
    }

    /**
     * SCAN: el cabezal se mueve en una direccion atendiendo
     * solicitudes, llega a la ultima en esa direccion
     * y regresa en direccion contraria.
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
     * C-SCAN: igual que SCAN pero al llegar al extremo regresa
     * al inicio sin atender solicitudes en el camino de vuelta.
     */
    private IORequest processCSTF() {
        LinkedList<IORequest> temp = queueToList();

        IORequest best = findNextInDirection(temp, true);

        if (best == null) {
            headPosition = 0;
            best = findNextInDirection(temp, true);
        }

        if (best == null && temp.getHead() != null) {
            best = temp.getHead().data;
        }

        rebuildQueue(temp, best);
        return best;
    }

    // ─── METODOS AUXILIARES ─────────────────────────────────────────────────

    /**
     * Encuentra la solicitud mas cercana al cabezal
     * en la direccion especificada.
     *
     * @param list    Lista de solicitudes disponibles.
     * @param goingUp true = buscar en cilindros mayores,
     *                false = buscar en cilindros menores.
     * @return La solicitud mas cercana en esa direccion,
     *         o null si no hay ninguna.
     */
    private IORequest findNextInDirection(
            LinkedList<IORequest> list,
            boolean goingUp) {
        IORequest best    = null;
        int bestDistance  = Integer.MAX_VALUE;

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
                    best         = current.data;
                }
            }
            current = current.next;
        }
        return best;
    }

    /**
     * Convierte la cola de solicitudes a una lista enlazada
     * temporal para poder recorrerla sin destruirla.
     * Vacia la cola en el proceso.
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
     * @param exclude La solicitud elegida que no debe volver.
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
     * Valida que una posicion de cilindro sea valida.
     * @param position Posicion a validar.
     * @throws IllegalArgumentException si esta fuera de rango.
     */
    private void validateCylinder(int position) {
        if (position < 0 || position > MAX_CYLINDER) {
            throw new IllegalArgumentException(
                "Posicion de cilindro invalida: " + position
                + ". Rango valido: 0 a " + MAX_CYLINDER
            );
        }
    }

    // ─── GETTERS ────────────────────────────────────────────────────────────

    public int getHeadPosition()       { return headPosition; }
    public SchedulerPolicy getPolicy() { return policy; }
    public int getTotalHeadMovement()  { return totalHeadMovement; }
    public boolean isQueueEmpty()      { return requestQueue.isEmpty(); }
    public int getPendingCount()       { return requestQueue.size(); }
    public boolean isMovingUp()        { return movingUp; }

    public LinkedList<IORequest> getAttendedRequests() {
        return attendedRequests;
    }

    /**
     * Retorna el semaforo del disco para uso del hilo planificador.
     * @return El Semaphore de acceso exclusivo al disco.
     */
    public Semaphore getDiskSemaphore() { return diskSemaphore; }

    // ─── SETTERS ────────────────────────────────────────────────────────────

    /**
     * Cambia la politica de planificacion conservando las
     * solicitudes pendientes en la cola.
     * Solo reinicia el historial y el movimiento total.
     *
     * @param policy Nueva politica a aplicar.
     */
    public void setPolicy(SchedulerPolicy policy) {
        this.policy            = policy;
        this.totalHeadMovement = 0;
        this.movingUp          = true;
        this.attendedRequests.clear();
    }

    /**
     * Establece la direccion inicial del cabezal.
     * Usado por la GUI para configurar SCAN y C-SCAN.
     * @param movingUp true = hacia cilindros mayores.
     */
    public void setMovingUp(boolean movingUp) {
        this.movingUp = movingUp;
    }

    /**
     * Reinicia el planificador completamente.
     * @param newHeadPosition Nueva posicion inicial del cabezal.
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