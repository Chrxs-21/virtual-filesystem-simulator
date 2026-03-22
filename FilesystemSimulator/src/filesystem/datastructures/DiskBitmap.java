package filesystem.datastructures;

/**
 * Bitmap del disco simulado.
 *
 * Gestiona cuáles bloques del disco están libres u ocupados
 * usando un arreglo primitivo boolean[], el cual está permitido
 * por el enunciado al no ser parte del Collections Framework.
 *
 * Reemplaza a HashMap<Integer, Boolean>.
 *
 * Convención: true = libre, false = ocupado.
 */
public class DiskBitmap {

    /** Estado de cada bloque. true = libre, false = ocupado. */
    private final boolean[] blocks;

    /** Número total de bloques del disco simulado. */
    private final int totalBlocks;

    /** Cantidad de bloques actualmente libres. */
    private int freeCount;

    /**
     * Crea un disco completamente libre.
     * @param totalBlocks Capacidad total del disco en bloques.
     * @throws IllegalArgumentException si totalBlocks es menor o igual a 0.
     */
    public DiskBitmap(int totalBlocks) {
        if (totalBlocks <= 0) {
            throw new IllegalArgumentException(
                "El disco debe tener al menos 1 bloque."
            );
        }
        this.totalBlocks = totalBlocks;
        this.blocks = new boolean[totalBlocks];
        this.freeCount = totalBlocks;
        for (int i = 0; i < totalBlocks; i++) {
            blocks[i] = true;
        }
    }

    // ─── ASIGNACIÓN ─────────────────────────────────────────────────────────

    /**
     * Busca y reserva el primer bloque libre. O(n).
     * @return Índice del bloque asignado.
     * @throws IllegalStateException si el disco está lleno.
     */
    public int allocate() {
        for (int i = 0; i < totalBlocks; i++) {
            if (blocks[i]) {
                blocks[i] = false;
                freeCount--;
                return i;
            }
        }
        throw new IllegalStateException(
            "Disco lleno. No hay bloques disponibles."
        );
    }

    /**
     * Libera un bloque previamente ocupado. O(1).
     * @param blockIndex Índice del bloque a liberar.
     * @throws IllegalArgumentException si el índice es inválido.
     * @throws IllegalStateException si el bloque ya estaba libre.
     */
    public void free(int blockIndex) {
        validateIndex(blockIndex);
        if (blocks[blockIndex]) {
            throw new IllegalStateException(
                "El bloque " + blockIndex + " ya estaba libre."
            );
        }
        blocks[blockIndex] = true;
        freeCount++;
    }

    // ─── CONSULTAS ──────────────────────────────────────────────────────────

    /**
     * Consulta si un bloque está libre. O(1).
     * @param blockIndex Índice a consultar.
     * @return true si está libre.
     */
    public boolean isFree(int blockIndex) {
        validateIndex(blockIndex);
        return blocks[blockIndex];
    }

    /** @return Número de bloques libres disponibles. */
    public int getFreeCount() { return freeCount; }

    /** @return Número de bloques ocupados. */
    public int getUsedCount() { return totalBlocks - freeCount; }

    /** @return Capacidad total del disco en bloques. */
    public int getTotalBlocks() { return totalBlocks; }

    /**
     * Retorna una copia del bitmap para la GUI.
     * Se devuelve copia para proteger el estado interno.
     * @return Arreglo donde true = libre, false = ocupado.
     */
    public boolean[] getSnapshot() {
        boolean[] copy = new boolean[totalBlocks];
        for (int i = 0; i < totalBlocks; i++) {
            copy[i] = blocks[i];
        }
        return copy;
    }

    // ─── VALIDACIÓN INTERNA ─────────────────────────────────────────────────

    /**
     * Valida que un índice de bloque esté dentro del rango permitido.
     * @param index Índice a validar.
     * @throws IllegalArgumentException si está fuera de rango.
     */
    private void validateIndex(int index) {
        if (index < 0 || index >= totalBlocks) {
            throw new IllegalArgumentException(
                "Bloque " + index + " inválido. Rango válido: 0 a "
                + (totalBlocks - 1)
            );
        }
    }
}