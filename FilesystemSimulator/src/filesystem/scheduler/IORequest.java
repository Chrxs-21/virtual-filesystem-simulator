package filesystem.scheduler;

/**
 * Representa una solicitud de E/S hacia el disco simulado.
 * Contiene la posición del cilindro que necesita el proceso,
 * el tipo de operación y el estado actual de la solicitud.
 *
 * No usa ninguna clase del Java Collections Framework.
 */
public class IORequest {

    /** Identificador único de la solicitud. */
    private final int id;

    /** Nombre del proceso que genera la solicitud. */
    private final String processName;

    /** Posición del cilindro en el disco (0 a MAX_CYLINDER). */
    private final int cylinderPosition;

    /** Tipo de operación solicitada. */
    private final OperationType operationType;

    /** Estado actual de la solicitud. */
    private RequestStatus status;

    /** Contador para generar IDs únicos automáticamente. */
    private static int idCounter = 0;

    // ─── ENUMS INTERNOS ─────────────────────────────────────────────────────

    /**
     * Tipos de operación posibles en una solicitud de E/S.
     */
    public enum OperationType {
        READ,
        WRITE
    }

    /**
     * Estados posibles de una solicitud de E/S.
     */
    public enum RequestStatus {
        WAITING,
        IN_PROGRESS,
        COMPLETED
    }

    /**
     * Crea una nueva solicitud de E/S.
     * @param processName      Nombre del proceso solicitante.
     * @param cylinderPosition Posición del cilindro requerido.
     * @param operationType    Tipo de operación (READ o WRITE).
     */
    public IORequest(String processName,
                     int cylinderPosition,
                     OperationType operationType) {
        this.id                = ++idCounter;
        this.processName       = processName;
        this.cylinderPosition  = cylinderPosition;
        this.operationType     = operationType;
        this.status            = RequestStatus.WAITING;
    }

    // ─── GETTERS ────────────────────────────────────────────────────────────

    public int getId()                  { return id; }
    public String getProcessName()      { return processName; }
    public int getCylinderPosition()    { return cylinderPosition; }
    public OperationType getOperationType() { return operationType; }
    public RequestStatus getStatus()    { return status; }

    // ─── SETTERS ────────────────────────────────────────────────────────────

    public void setStatus(RequestStatus status) { this.status = status; }

    /** Reinicia el contador de IDs. Útil al cargar estado desde JSON. */
    public static void resetIdCounter() { idCounter = 0; }

    @Override
    public String toString() {
        return "IORequest[id=" + id
             + " proc=" + processName
             + " cyl=" + cylinderPosition
             + " op=" + operationType
             + " status=" + status + "]";
    }
}