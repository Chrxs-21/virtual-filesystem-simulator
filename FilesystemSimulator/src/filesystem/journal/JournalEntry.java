package filesystem.journal;

/**
 * Representa una entrada individual en el journal del sistema.
 *
 * Cada operacion sobre el sistema de archivos genera una entrada que se
 * registra como PENDING antes de ejecutarse y se marca como CONFIRMED tras
 * completarse exitosamente.
 *
 * En caso de fallo, las entradas PENDING se marcan ROLLED_BACK. No usa ninguna
 * clase del Java Collections Framework.
 */
public class JournalEntry {

    /**
     * Identificador unico de la entrada.
     */
    private final int id;

    /**
     * Tipo de operacion registrada.
     */
    private final OperationType operationType;

    /**
     * Nombre del archivo o directorio afectado.
     */
    private final String targetName;

    /**
     * Ruta del directorio donde ocurre la operacion.
     */
    private final String targetPath;

    /**
     * Datos anteriores al cambio, usados para el UNDO. Puede ser el contenido
     * anterior, nombre anterior, etc.
     */
    private final String previousData;

    /**
     * Datos nuevos que la operacion intenta escribir.
     */
    private final String newData;

    /**
     * Estado actual de esta entrada en el journal.
     */
    private JournalStatus status;

    /**
     * Marca de tiempo de cuando se registro la entrada.
     */
    private final String timestamp;

    /**
     * Contador para generar IDs unicos automaticamente.
     */
    private static int idCounter = 0;

    // ─── ENUM DE OPERACIONES ────────────────────────────────────────────────
    /**
     * Tipos de operacion que el journal puede registrar.
     */
    public enum OperationType {
        CREATE_FILE,
        DELETE_FILE,
        UPDATE_FILE,
        RENAME_FILE,
        CREATE_DIRECTORY,
        DELETE_DIRECTORY
    }

    /**
     * Crea una nueva entrada en el journal con estado PENDING.
     *
     * @param operationType Tipo de operacion a registrar.
     * @param targetName Nombre del archivo o directorio afectado.
     * @param targetPath Ruta del directorio contenedor.
     * @param previousData Datos anteriores (para UNDO). Null si es creacion.
     * @param newData Datos nuevos (para REDO). Null si es eliminacion.
     */
    public JournalEntry(OperationType operationType,
            String targetName,
            String targetPath,
            String previousData,
            String newData) {
        this.id = ++idCounter;
        this.operationType = operationType;
        this.targetName = targetName;
        this.targetPath = targetPath;
        this.previousData = previousData;
        this.newData = newData;
        this.status = JournalStatus.PENDING;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }

    // ─── GETTERS ────────────────────────────────────────────────────────────
    public int getId() {
        return id;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public String getPreviousData() {
        return previousData;
    }

    public String getNewData() {
        return newData;
    }

    public JournalStatus getStatus() {
        return status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isPending() {
        return status == JournalStatus.PENDING;
    }

    public boolean isConfirmed() {
        return status == JournalStatus.CONFIRMED;
    }

    public boolean isRolledBack() {
        return status == JournalStatus.ROLLED_BACK;
    }

    // ─── SETTERS ────────────────────────────────────────────────────────────
    public void setStatus(JournalStatus status) {
        this.status = status;
    }

    /**
     * Reinicia el contador de IDs. Util al cargar desde JSON.
     */
    public static void resetIdCounter() {
        idCounter = 0;
    }

    @Override
    public String toString() {
        return "JournalEntry[id=" + id
                + " op=" + operationType
                + " target=" + targetName
                + " status=" + status
                + " time=" + timestamp + "]";
    }
}
