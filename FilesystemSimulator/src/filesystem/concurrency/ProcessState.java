package filesystem.concurrency;

/**
 * Estados posibles en el ciclo de vida de un proceso dentro del sistema de
 * control de concurrencia.
 */
public enum ProcessState {
    NEW,
    READY,
    RUNNING,
    BLOCKED,
    TERMINATED
}
