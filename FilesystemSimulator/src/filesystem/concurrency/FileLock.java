package filesystem.concurrency;

import filesystem.datastructures.LinkedList;
import filesystem.datastructures.Node;

/**
 * Controla el acceso concurrente a un archivo individual.
 *
 * Implementa el protocolo de locks compartido/exclusivo:
 * - Lock compartido: multiples lectores simultaneos permitidos.
 * - Lock exclusivo: un solo escritor, bloquea a todos los demas.
 *
 * Los procesos bloqueados se encolan en una lista de espera
 * usando LinkedList propia. No usa java.util.* de colecciones.
 */
public class FileLock {

    /** Nombre del archivo que este lock protege. */
    private final String fileName;

    /** Tipo de lock actualmente activo sobre el archivo. */
    private LockType currentLockType;

    /** Numero de lectores activos en este momento. */
    private int activeReaders;

    /** Nombre del proceso que tiene el lock exclusivo activo. */
    private String exclusiveOwner;

    /**
     * Cola de procesos esperando acceso al archivo.
     * Usa LinkedList propia, no ArrayList ni Queue.
     */
    private final LinkedList<WaitingProcess> waitingQueue;

    // ─── ENUMS ──────────────────────────────────────────────────────────────

    /**
     * Tipos de lock disponibles.
     */
    public enum LockType {
        NONE,
        SHARED,
        EXCLUSIVE
    }

    /**
     * Representa un proceso esperando en la cola de este lock.
     */
    public static class WaitingProcess {
        public final String processName;
        public final LockType requestedType;
        public ProcessState state;

        public WaitingProcess(String processName, LockType requestedType) {
            this.processName   = processName;
            this.requestedType = requestedType;
            this.state         = ProcessState.BLOCKED;
        }

        @Override
        public String toString() {
            return processName + " esperando lock " + requestedType;
        }
    }

    /**
     * Crea un lock para el archivo especificado.
     * Inicialmente sin ningun lock activo.
     *
     * @param fileName Nombre del archivo a proteger.
     */
    public FileLock(String fileName) {
        this.fileName        = fileName;
        this.currentLockType = LockType.NONE;
        this.activeReaders   = 0;
        this.exclusiveOwner  = null;
        this.waitingQueue    = new LinkedList<>();
    }

    // ─── ADQUIRIR LOCK ──────────────────────────────────────────────────────

    /**
     * Intenta adquirir un lock compartido (lectura) para un proceso.
     * Se otorga si no hay un lock exclusivo activo.
     * Si no se puede otorgar, el proceso queda en cola de espera.
     *
     * @param processName Nombre del proceso solicitante.
     * @return true si el lock fue otorgado, false si quedo bloqueado.
     */
    public boolean acquireShared(String processName) {
        if (currentLockType == LockType.EXCLUSIVE) {
            // Hay un escritor activo, el proceso debe esperar
            waitingQueue.addLast(
                new WaitingProcess(processName, LockType.SHARED)
            );
            return false;
        }

        // Se puede otorgar: ninguno o ya hay lectores activos
        currentLockType = LockType.SHARED;
        activeReaders++;
        return true;
    }

    /**
     * Intenta adquirir un lock exclusivo (escritura) para un proceso.
     * Se otorga solo si no hay ningun lock activo.
     * Si no se puede otorgar, el proceso queda en cola de espera.
     *
     * @param processName Nombre del proceso solicitante.
     * @return true si el lock fue otorgado, false si quedo bloqueado.
     */
    public boolean acquireExclusive(String processName) {
        if (currentLockType != LockType.NONE) {
            // Hay lectores o un escritor, el proceso debe esperar
            waitingQueue.addLast(
                new WaitingProcess(processName, LockType.EXCLUSIVE)
            );
            return false;
        }

        // Se puede otorgar
        currentLockType = LockType.EXCLUSIVE;
        exclusiveOwner  = processName;
        return true;
    }

    // ─── LIBERAR LOCK ───────────────────────────────────────────────────────

    /**
     * Libera el lock compartido de un lector.
     * Si era el ultimo lector, intenta despertar procesos en espera.
     *
     * @param processName Nombre del proceso que libera el lock.
     * @throws IllegalStateException si el proceso no tenia lock compartido.
     */
    public void releaseShared(String processName) {
        if (currentLockType != LockType.SHARED || activeReaders == 0) {
            throw new IllegalStateException(
                "El proceso '" + processName
                + "' no tiene lock compartido sobre " + fileName
            );
        }

        activeReaders--;
        if (activeReaders == 0) {
            currentLockType = LockType.NONE;
            wakeUpNext();
        }
    }

    /**
     * Libera el lock exclusivo del escritor activo.
     * Intenta despertar el siguiente proceso en espera.
     *
     * @param processName Nombre del proceso que libera el lock.
     * @throws IllegalStateException si el proceso no es el dueno del lock.
     */
    public void releaseExclusive(String processName) {
        if (currentLockType != LockType.EXCLUSIVE
                || !processName.equals(exclusiveOwner)) {
            throw new IllegalStateException(
                "El proceso '" + processName
                + "' no tiene lock exclusivo sobre " + fileName
            );
        }

        currentLockType = LockType.NONE;
        exclusiveOwner  = null;
        wakeUpNext();
    }

    // ─── DESPERTAR PROCESOS EN ESPERA ───────────────────────────────────────

    /**
     * Intenta otorgar el lock al siguiente proceso en la cola de espera.
     * Si el siguiente pide compartido, despierta a todos los lectores
     * consecutivos en la cola.
     */
    private void wakeUpNext() {
        if (waitingQueue.isEmpty()) return;

        WaitingProcess next = waitingQueue.getFirst();

        if (next.requestedType == LockType.EXCLUSIVE) {
            // Despertar solo al escritor
            waitingQueue.removeFirst();
            currentLockType = LockType.EXCLUSIVE;
            exclusiveOwner  = next.processName;
            next.state      = ProcessState.RUNNING;

        } else {
            // Despertar a todos los lectores consecutivos al frente
            while (!waitingQueue.isEmpty()
                    && waitingQueue.getFirst().requestedType
                       == LockType.SHARED) {
                WaitingProcess reader = waitingQueue.removeFirst();
                reader.state = ProcessState.RUNNING;
                activeReaders++;
            }
            if (activeReaders > 0) {
                currentLockType = LockType.SHARED;
            }
        }
    }

    // ─── GETTERS ────────────────────────────────────────────────────────────

    public String getFileName()         { return fileName; }
    public LockType getCurrentLockType(){ return currentLockType; }
    public int getActiveReaders()       { return activeReaders; }
    public String getExclusiveOwner()   { return exclusiveOwner; }
    public boolean isLocked()           { return currentLockType != LockType.NONE; }
    public int getWaitingCount()        { return waitingQueue.size(); }

    /**
     * Retorna la cola de espera para mostrar en la GUI.
     * @return LinkedList de procesos en espera.
     */
    public LinkedList<WaitingProcess> getWaitingQueue() {
        return waitingQueue;
    }

    @Override
    public String toString() {
        return "FileLock[" + fileName
             + " type=" + currentLockType
             + " readers=" + activeReaders
             + " waiting=" + waitingQueue.size() + "]";
    }
}