package filesystem.concurrency;

import filesystem.datastructures.LinkedList;
import filesystem.datastructures.Node;

/**
 * Gestor global de locks del sistema de archivos.
 * Administra un lock por cada archivo que tenga acceso activo.
 *
 * Usa LinkedList propia para almacenar los locks activos.
 * No usa HashMap ni ninguna clase del Collections Framework.
 */
public class LockManager {

    /**
     * Lista de locks activos en el sistema.
     * Cada archivo con acceso concurrente tiene exactamente un FileLock.
     * Usa LinkedList propia, no HashMap.
     */
    private final LinkedList<FileLock> activeLocks;

    /** Crea un gestor de locks sin locks activos. */
    public LockManager() {
        this.activeLocks = new LinkedList<>();
    }

    // ─── ADQUIRIR LOCKS ─────────────────────────────────────────────────────

    /**
     * Solicita un lock compartido (lectura) sobre un archivo.
     *
     * @param fileName    Nombre del archivo a leer.
     * @param processName Nombre del proceso solicitante.
     * @return true si el lock fue otorgado inmediatamente,
     *         false si el proceso quedo bloqueado en espera.
     */
    public boolean acquireSharedLock(String fileName, String processName) {
        FileLock lock = getOrCreateLock(fileName);
        return lock.acquireShared(processName);
    }

    /**
     * Solicita un lock exclusivo (escritura) sobre un archivo.
     *
     * @param fileName    Nombre del archivo a escribir.
     * @param processName Nombre del proceso solicitante.
     * @return true si el lock fue otorgado inmediatamente,
     *         false si el proceso quedo bloqueado en espera.
     */
    public boolean acquireExclusiveLock(String fileName, String processName) {
        FileLock lock = getOrCreateLock(fileName);
        return lock.acquireExclusive(processName);
    }

    // ─── LIBERAR LOCKS ──────────────────────────────────────────────────────

    /**
     * Libera el lock compartido de un proceso sobre un archivo.
     *
     * @param fileName    Nombre del archivo.
     * @param processName Nombre del proceso que libera el lock.
     * @throws IllegalArgumentException si no existe lock para ese archivo.
     */
    public void releaseSharedLock(String fileName, String processName) {
        FileLock lock = findLock(fileName);
        if (lock == null) {
            throw new IllegalArgumentException(
                "No existe lock activo para el archivo '" + fileName + "'"
            );
        }
        lock.releaseShared(processName);
        cleanupIfUnlocked(lock);
    }

    /**
     * Libera el lock exclusivo de un proceso sobre un archivo.
     *
     * @param fileName    Nombre del archivo.
     * @param processName Nombre del proceso que libera el lock.
     * @throws IllegalArgumentException si no existe lock para ese archivo.
     */
    public void releaseExclusiveLock(String fileName, String processName) {
        FileLock lock = findLock(fileName);
        if (lock == null) {
            throw new IllegalArgumentException(
                "No existe lock activo para el archivo '" + fileName + "'"
            );
        }
        lock.releaseExclusive(processName);
        cleanupIfUnlocked(lock);
    }

    // ─── CONSULTAS ──────────────────────────────────────────────────────────

    /**
     * Consulta el estado del lock de un archivo.
     *
     * @param fileName Nombre del archivo.
     * @return El FileLock asociado, o null si no tiene lock activo.
     */
    public FileLock getLock(String fileName) {
        return findLock(fileName);
    }

    /**
     * Verifica si un archivo tiene algun lock activo.
     *
     * @param fileName Nombre del archivo.
     * @return true si tiene lock activo.
     */
    public boolean isLocked(String fileName) {
        FileLock lock = findLock(fileName);
        return lock != null && lock.isLocked();
    }

    /**
     * Retorna todos los locks activos para mostrar en la GUI.
     * @return LinkedList de FileLocks activos.
     */
    public LinkedList<FileLock> getActiveLocks() {
        return activeLocks;
    }

    // ─── METODOS INTERNOS ───────────────────────────────────────────────────

    /**
     * Busca el lock de un archivo en la lista de locks activos. O(n).
     * Se usa busqueda lineal porque no podemos usar HashMap.
     *
     * @param fileName Nombre del archivo a buscar.
     * @return El FileLock encontrado, o null si no existe.
     */
    private FileLock findLock(String fileName) {
        Node<FileLock> current = activeLocks.getHead();
        while (current != null) {
            if (current.data.getFileName().equals(fileName)) {
                return current.data;
            }
            current = current.next;
        }
        return null;
    }

    /**
     * Retorna el lock existente de un archivo, o crea uno nuevo
     * si no existe todavia.
     *
     * @param fileName Nombre del archivo.
     * @return El FileLock correspondiente.
     */
    private FileLock getOrCreateLock(String fileName) {
        FileLock existing = findLock(fileName);
        if (existing != null) return existing;

        FileLock newLock = new FileLock(fileName);
        activeLocks.addLast(newLock);
        return newLock;
    }

    /**
     * Elimina un lock de la lista si ya no tiene actividad.
     * Mantiene la lista limpia de locks innecesarios.
     *
     * @param lock El lock a evaluar para limpieza.
     */
    private void cleanupIfUnlocked(FileLock lock) {
        if (!lock.isLocked() && lock.getWaitingCount() == 0) {
            activeLocks.remove(lock);
        }
    }
}