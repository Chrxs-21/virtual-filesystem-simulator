package filesystem.journal;

/**
 * Estados posibles de una entrada del journal.
 */
public enum JournalStatus {
    /** Operacion registrada pero aun no completada. */
    PENDING,
    /** Operacion completada y confirmada exitosamente. */
    CONFIRMED,
    /** Operacion revertida tras recuperacion de un fallo. */
    ROLLED_BACK
}