package filesystem.model;

import filesystem.datastructures.DiskBitmap;

/**
 * Sistema de archivos simulado.
 * Coordina la asignación de bloques en disco, la estructura de
 * directorios y todas las operaciones sobre archivos.
 *
 * Usa DiskBitmap propio y arreglo DiskBlock[] como disco simulado.
 * No usa ninguna clase del Java Collections Framework.
 */
public class FileSystem {

    // ─── CONFIGURACIÓN DEL DISCO ────────────────────────────────────────────

    /** Número total de bloques del disco simulado. */
    public static final int TOTAL_BLOCKS = 64;

    /** Bloques necesarios por cada 512 bytes de contenido. */
    public static final int BLOCK_SIZE = DiskBlock.BLOCK_SIZE;

    // ─── ESTADO INTERNO ─────────────────────────────────────────────────────

    /** Representación física del disco: arreglo de bloques. */
    private final DiskBlock[] disk;

    /** Bitmap que rastrea qué bloques están libres. */
    private final DiskBitmap bitmap;

    /** Directorio raíz del sistema de archivos. */
    private final DirectoryEntry root;

    /** Modo actual del sistema: administrador o usuario. */
    private UserMode currentMode;

    /**
     * Inicializa el sistema de archivos con el disco vacío
     * y el directorio raíz creado.
     */
    public FileSystem() {
        this.disk        = new DiskBlock[TOTAL_BLOCKS];
        this.bitmap      = new DiskBitmap(TOTAL_BLOCKS);
        this.root        = new DirectoryEntry("root", null);
        this.currentMode = UserMode.ADMINISTRATOR;

        // Inicializar cada bloque del disco
        for (int i = 0; i < TOTAL_BLOCKS; i++) {
            disk[i] = new DiskBlock(i);
        }
    }

    // ─── OPERACIONES DE ARCHIVO ─────────────────────────────────────────────

    /**
     * Crea un nuevo archivo en el directorio dado y le asigna
     * los bloques necesarios en el disco mediante asignación encadenada.
     *
     * @param fileName  Nombre del archivo a crear.
     * @param content   Contenido del archivo.
     * @param directory Directorio destino.
     * @param readOnly  true si el archivo es de solo lectura.
     * @return El FileEntry creado.
     * @throws IllegalArgumentException si el nombre ya existe en ese dir.
     * @throws IllegalStateException    si no hay espacio en disco.
     */
    public FileEntry createFile(String fileName,
                                String content,
                                DirectoryEntry directory,
                                boolean readOnly) {

        // Validar que no exista otro archivo con el mismo nombre
        if (directory.findFile(fileName) != null) {
            throw new IllegalArgumentException(
                "Ya existe un archivo llamado '" + fileName
                + "' en este directorio."
            );
        }

        // Calcular cuántos bloques necesita el archivo
        int blocksNeeded = calculateBlocksNeeded(content);

        // Verificar que hay espacio suficiente
        if (bitmap.getFreeCount() < blocksNeeded) {
            throw new IllegalStateException(
                "Espacio insuficiente. Necesita " + blocksNeeded
                + " bloques, disponibles: " + bitmap.getFreeCount()
            );
        }

        // Crear la entrada del archivo
        FileEntry file = new FileEntry(
            fileName, readOnly,
            java.time.LocalDateTime.now().toString()
        );

        // Asignar bloques encadenados
        int firstBlockIndex = -1;
        int previousBlockIndex = -1;

        for (int i = 0; i < blocksNeeded; i++) {
            int blockIndex = bitmap.allocate();
            DiskBlock block = disk[blockIndex];
            block.setOwnerFile(fileName);

            // Distribuir el contenido entre los bloques
            int start = i * BLOCK_SIZE;
            int end   = Math.min(start + BLOCK_SIZE, content.length());
            if (start < content.length()) {
                block.setContent(content.substring(start, end));
            }

            // Encadenar con el bloque anterior
            if (previousBlockIndex != -1) {
                disk[previousBlockIndex].setNextBlock(blockIndex);
            } else {
                firstBlockIndex = blockIndex;
            }
            previousBlockIndex = blockIndex;
        }

        // El último bloque ya tiene nextBlock = -1 por defecto
        file.setFirstBlock(firstBlockIndex);
        file.setSize(content.length());
        file.setBlockCount(blocksNeeded);

        // Registrar en el directorio
        directory.addFile(file);

        return file;
    }

    /**
     * Lee y reconstruye el contenido completo de un archivo
     * recorriendo su cadena de bloques en el disco.
     *
     * @param file El archivo a leer.
     * @return El contenido completo como String.
     * @throws IllegalArgumentException si el archivo no tiene bloques.
     */
    public String readFile(FileEntry file) {
        if (file.getFirstBlock() == -1) return "";

        StringBuilder content = new StringBuilder();
        int currentIndex = file.getFirstBlock();

        while (currentIndex != -1) {
            DiskBlock block = disk[currentIndex];
            content.append(block.getContent());
            currentIndex = block.getNextBlock();
        }

        return content.toString();
    }

    /**
     * Elimina un archivo del directorio y libera todos sus bloques
     * en el disco recorriendo la cadena completa.
     *
     * @param fileName  Nombre del archivo a eliminar.
     * @param directory Directorio donde está el archivo.
     * @throws IllegalArgumentException si el archivo no existe.
     * @throws IllegalStateException    si el archivo es de solo lectura
     *                                  y el modo actual es USUARIO.
     */
    public void deleteFile(String fileName, DirectoryEntry directory) {
        FileEntry file = directory.findFile(fileName);

        if (file == null) {
            throw new IllegalArgumentException(
                "El archivo '" + fileName + "' no existe en este directorio."
            );
        }

        // Solo el administrador puede eliminar archivos de solo lectura
        if (file.isReadOnly() && currentMode == UserMode.USER) {
            throw new IllegalStateException(
                "Permiso denegado. El archivo es de solo lectura."
            );
        }

        // Liberar todos los bloques de la cadena
        int currentIndex = file.getFirstBlock();
        while (currentIndex != -1) {
            DiskBlock block = disk[currentIndex];
            int nextIndex = block.getNextBlock();
            block.clear();
            bitmap.free(currentIndex);
            currentIndex = nextIndex;
        }

        // Eliminar del directorio
        directory.removeFile(fileName);
    }

    // ─── UTILIDADES ─────────────────────────────────────────────────────────

    /**
     * Calcula cuántos bloques necesita un contenido dado.
     * Mínimo 1 bloque aunque el contenido esté vacío.
     * @param content Contenido del archivo.
     * @return Número de bloques necesarios.
     */
    private int calculateBlocksNeeded(String content) {
        if (content == null || content.isEmpty()) return 1;
        return (int) Math.ceil((double) content.length() / BLOCK_SIZE);
    }

    // ─── GETTERS ────────────────────────────────────────────────────────────

    public DirectoryEntry getRoot()    { return root; }
    public DiskBitmap getBitmap()      { return bitmap; }
    public DiskBlock[] getDisk()       { return disk; }
    public UserMode getCurrentMode()   { return currentMode; }
    public void setCurrentMode(UserMode mode) { this.currentMode = mode; }

    /**
     * Información general del disco para la GUI.
     */
    public String getDiskInfo() {
        return "Disco: " + bitmap.getUsedCount() + "/"
             + TOTAL_BLOCKS + " bloques usados ("
             + bitmap.getFreeCount() + " libres)";
    }
}