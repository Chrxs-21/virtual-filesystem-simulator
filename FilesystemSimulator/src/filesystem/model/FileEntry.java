package filesystem.model;

/**
 * Representa un archivo dentro del sistema de archivos simulado.
 * No almacena los bloques directamente, sino el índice del primer
 * bloque de su cadena. El resto se recorre desde el disco.
 */
public class FileEntry {

    /** Nombre del archivo incluyendo extensión. */
    private String name;

    /**
     * Índice del primer bloque de este archivo en el disco.
     * -1 si el archivo está vacío o no tiene bloques asignados.
     */
    private int firstBlock;

    /** Tamaño del archivo en bytes simulados. */
    private int size;

    /** Modo de acceso: true = solo lectura, false = lectura y escritura. */
    private boolean readOnly;

    /** Fecha de creación (texto simple para la GUI). */
    private String createdAt;

    /** Número de bloques ocupados por este archivo. */
    private int blockCount;

    /**
     * Crea un archivo vacío sin bloques asignados todavía.
     * @param name      Nombre del archivo.
     * @param readOnly  true si es de solo lectura.
     * @param createdAt Fecha de creación.
     */
    public FileEntry(String name, boolean readOnly, String createdAt) {
        this.name       = name;
        this.firstBlock = -1;
        this.size       = 0;
        this.readOnly   = readOnly;
        this.createdAt  = createdAt;
        this.blockCount = 0;
    }

    // ─── GETTERS ────────────────────────────────────────────────────────────

    public String getName()      { return name; }
    public int getFirstBlock()   { return firstBlock; }
    public int getSize()         { return size; }
    public boolean isReadOnly()  { return readOnly; }
    public String getCreatedAt() { return createdAt; }
    public int getBlockCount()   { return blockCount; }

    // ─── SETTERS ────────────────────────────────────────────────────────────

    public void setName(String name)         { this.name = name; }
    public void setFirstBlock(int firstBlock){ this.firstBlock = firstBlock; }
    public void setSize(int size)            { this.size = size; }
    public void setReadOnly(boolean readOnly){ this.readOnly = readOnly; }
    public void setBlockCount(int count)     { this.blockCount = count; }

    @Override
    public String toString() {
        return "File[" + name + "] blocks=" + blockCount
             + " firstBlock=" + firstBlock + " size=" + size + "B";
    }
}