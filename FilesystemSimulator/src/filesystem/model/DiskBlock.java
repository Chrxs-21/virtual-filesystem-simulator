package filesystem.model;

/**
 * Representa un bloque físico del disco simulado. Cada archivo es una cadena de
 * estos bloques enlazados. El campo 'nextBlock' apunta al índice del siguiente
 * bloque del mismo archivo. -1 indica que es el último bloque.
 */
public class DiskBlock {

    /**
     * Índice de este bloque en el disco. Inmutable.
     */
    private final int index;

    /**
     * Índice del siguiente bloque del mismo archivo. -1 si es el último bloque
     * de la cadena.
     */
    private int nextBlock;

    /**
     * Nombre del archivo propietario. null si el bloque está libre.
     */
    private String ownerFile;

    /**
     * Datos almacenados en este bloque (contenido del archivo).
     */
    private String content;

    /**
     * Tamaño fijo de cada bloque en bytes simulados.
     */
    public static final int BLOCK_SIZE = 512;

    /**
     * Crea un bloque libre sin propietario.
     *
     * @param index Índice de este bloque en el disco.
     */
    public DiskBlock(int index) {
        this.index = index;
        this.nextBlock = -1;
        this.ownerFile = null;
        this.content = "";
    }

    // ─── GETTERS ────────────────────────────────────────────────────────────
    public int getIndex() {
        return index;
    }

    public int getNextBlock() {
        return nextBlock;
    }

    public String getOwnerFile() {
        return ownerFile;
    }

    public String getContent() {
        return content;
    }

    /**
     * @return true si el bloque no tiene propietario.
     */
    public boolean isFree() {
        return ownerFile == null;
    }

    // ─── SETTERS ────────────────────────────────────────────────────────────
    public void setNextBlock(int nextBlock) {
        this.nextBlock = nextBlock;
    }

    public void setOwnerFile(String owner) {
        this.ownerFile = owner;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Libera el bloque, borrando propietario, contenido y puntero.
     */
    public void clear() {
        this.nextBlock = -1;
        this.ownerFile = null;
        this.content = "";
    }

    @Override
    public String toString() {
        return "Block[" + index + "] owner=" + ownerFile
                + " next=" + nextBlock;
    }
}
