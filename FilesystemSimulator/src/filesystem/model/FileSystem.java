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

    if (file.isReadOnly() && currentMode == UserMode.USER) {
        throw new IllegalStateException(
            "Permiso denegado. El archivo es de solo lectura."
        );
    }

    // Liberar bloques usando el método reutilizable
    freeFileBlocks(file);

    // Eliminar del directorio
    directory.removeFile(fileName);
}
    // ─── OPERACIONES DE DIRECTORIO ──────────────────────────────────────────

/**
 * Crea un nuevo subdirectorio dentro del directorio dado.
 *
 * @param dirName   Nombre del directorio a crear.
 * @param parent    Directorio padre donde se creará.
 * @return El DirectoryEntry creado.
 * @throws IllegalArgumentException si ya existe un directorio con ese nombre.
 * @throws IllegalStateException    si el modo actual es USUARIO.
 */
public DirectoryEntry createDirectory(String dirName,
                                      DirectoryEntry parent) {
    // Solo el administrador puede crear directorios
    if (currentMode == UserMode.USER) {
        throw new IllegalStateException(
            "Permiso denegado. Solo el administrador puede "
            + "crear directorios."
        );
    }

    // Validar que no exista otro directorio con el mismo nombre
    if (parent.findSubDirectory(dirName) != null) {
        throw new IllegalArgumentException(
            "Ya existe un directorio llamado '" + dirName
            + "' en esta ubicación."
        );
    }

    DirectoryEntry newDir = new DirectoryEntry(dirName, parent);
    parent.addSubDirectory(newDir);
    return newDir;
}

/**
 * Elimina un directorio y todo su contenido de forma recursiva.
 * Libera todos los bloques de disco de los archivos contenidos.
 *
 * @param dirName Nombre del directorio a eliminar.
 * @param parent  Directorio padre que contiene al directorio.
 * @throws IllegalArgumentException si el directorio no existe.
 * @throws IllegalStateException    si el modo actual es USUARIO.
 */
public void deleteDirectory(String dirName, DirectoryEntry parent) {
    if (currentMode == UserMode.USER) {
        throw new IllegalStateException(
            "Permiso denegado. Solo el administrador puede "
            + "eliminar directorios."
        );
    }

    DirectoryEntry target = parent.findSubDirectory(dirName);
    if (target == null) {
        throw new IllegalArgumentException(
            "El directorio '" + dirName + "' no existe."
        );
    }

    // Eliminar recursivamente todo el contenido
    deleteDirectoryRecursive(target);

    // Remover del padre
    parent.removeSubDirectory(dirName);
}

/**
 * Método auxiliar que elimina recursivamente el contenido
 * de un directorio: primero sus subdirectorios, luego sus archivos.
 *
 * @param dir El directorio a vaciar y eliminar.
 */
private void deleteDirectoryRecursive(DirectoryEntry dir) {
    // Primero eliminar todos los subdirectorios recursivamente
    filesystem.datastructures.Node<DirectoryEntry> currentDir =
        dir.getSubDirectories().getHead();
    while (currentDir != null) {
        deleteDirectoryRecursive(currentDir.data);
        currentDir = currentDir.next;
    }

    // Luego liberar los bloques de todos los archivos del directorio
    filesystem.datastructures.Node<FileEntry> currentFile =
        dir.getFiles().getHead();
    while (currentFile != null) {
        freeFileBlocks(currentFile.data);
        currentFile = currentFile.next;
    }

    // Vaciar las listas del directorio
    dir.getFiles().clear();
    dir.getSubDirectories().clear();
}

/**
 * Libera todos los bloques de disco ocupados por un archivo.
 * Método interno reutilizado por deleteFile y deleteDirectoryRecursive.
 *
 * @param file El archivo cuyos bloques se van a liberar.
 */
private void freeFileBlocks(FileEntry file) {
    int currentIndex = file.getFirstBlock();
    while (currentIndex != -1) {
        DiskBlock block = disk[currentIndex];
        int nextIndex = block.getNextBlock();
        block.clear();
        bitmap.free(currentIndex);
        currentIndex = nextIndex;
    }
    file.setFirstBlock(-1);
    file.setBlockCount(0);
    file.setSize(0);
}

// ─── OPERACIONES CRUD ADICIONALES ───────────────────────────────────────

/**
 * Renombra un archivo existente en un directorio.
 *
 * @param oldName   Nombre actual del archivo.
 * @param newName   Nuevo nombre del archivo.
 * @param directory Directorio que contiene el archivo.
 * @throws IllegalArgumentException si el archivo no existe,
 *                                  o si ya existe uno con el nuevo nombre.
 * @throws IllegalStateException    si el archivo es de solo lectura
 *                                  y el modo es USUARIO.
 */
public void renameFile(String oldName,
                       String newName,
                       DirectoryEntry directory) {
    FileEntry file = directory.findFile(oldName);
    if (file == null) {
        throw new IllegalArgumentException(
            "El archivo '" + oldName + "' no existe."
        );
    }

    if (file.isReadOnly() && currentMode == UserMode.USER) {
        throw new IllegalStateException(
            "Permiso denegado. El archivo es de solo lectura."
        );
    }

    if (directory.findFile(newName) != null) {
        throw new IllegalArgumentException(
            "Ya existe un archivo llamado '" + newName + "'."
        );
    }

    // Actualizar el nombre en el archivo y en todos sus bloques del disco
    file.setName(newName);
    updateBlockOwner(file, oldName, newName);
}

/**
 * Actualiza el nombre del propietario en todos los bloques
 * de un archivo después de renombrarlo.
 *
 * @param file    El archivo renombrado.
 * @param oldName Nombre anterior.
 * @param newName Nombre nuevo.
 */
private void updateBlockOwner(FileEntry file,
                               String oldName,
                               String newName) {
    int currentIndex = file.getFirstBlock();
    while (currentIndex != -1) {
        DiskBlock block = disk[currentIndex];
        if (oldName.equals(block.getOwnerFile())) {
            block.setOwnerFile(newName);
        }
        currentIndex = block.getNextBlock();
    }
}

/**
 * Actualiza el contenido de un archivo existente.
 * Libera los bloques antiguos y asigna bloques nuevos
 * con el contenido actualizado.
 *
 * @param fileName  Nombre del archivo a actualizar.
 * @param newContent Nuevo contenido del archivo.
 * @param directory Directorio que contiene el archivo.
 * @throws IllegalArgumentException si el archivo no existe.
 * @throws IllegalStateException    si es de solo lectura y modo USUARIO,
 *                                  o si no hay espacio suficiente.
 */
public void updateFile(String fileName,
                       String newContent,
                       DirectoryEntry directory) {
    FileEntry file = directory.findFile(fileName);
    if (file == null) {
        throw new IllegalArgumentException(
            "El archivo '" + fileName + "' no existe."
        );
    }

    if (file.isReadOnly() && currentMode == UserMode.USER) {
        throw new IllegalStateException(
            "Permiso denegado. El archivo es de solo lectura."
        );
    }

    // Verificar espacio para el nuevo contenido
    int blocksNeeded = calculateBlocksNeeded(newContent);
    int blocksCurrently = file.getBlockCount();
    int extraNeeded = blocksNeeded - blocksCurrently;

    if (extraNeeded > 0 && bitmap.getFreeCount() < extraNeeded) {
        throw new IllegalStateException(
            "Espacio insuficiente para actualizar el archivo."
        );
    }

    // Liberar bloques actuales
    freeFileBlocks(file);

    // Reasignar bloques con el nuevo contenido
    int firstBlockIndex    = -1;
    int previousBlockIndex = -1;

    for (int i = 0; i < blocksNeeded; i++) {
        int blockIndex = bitmap.allocate();
        DiskBlock block = disk[blockIndex];
        block.setOwnerFile(fileName);

        int start = i * BLOCK_SIZE;
        int end   = Math.min(start + BLOCK_SIZE, newContent.length());
        if (start < newContent.length()) {
            block.setContent(newContent.substring(start, end));
        }

        if (previousBlockIndex != -1) {
            disk[previousBlockIndex].setNextBlock(blockIndex);
        } else {
            firstBlockIndex = blockIndex;
        }
        previousBlockIndex = blockIndex;
    }

    file.setFirstBlock(firstBlockIndex);
    file.setSize(newContent.length());
    file.setBlockCount(blocksNeeded);
}

/**
 * Busca un archivo por nombre dentro de un directorio
 * y todos sus subdirectorios de forma recursiva.
 *
 * @param fileName  Nombre del archivo a buscar.
 * @param directory Directorio desde donde iniciar la búsqueda.
 * @return El FileEntry encontrado, o null si no existe.
 */
public FileEntry searchFile(String fileName,
                             DirectoryEntry directory) {
    // Buscar en el directorio actual
    FileEntry found = directory.findFile(fileName);
    if (found != null) return found;

    // Buscar recursivamente en subdirectorios
    filesystem.datastructures.Node<DirectoryEntry> current =
        directory.getSubDirectories().getHead();
    while (current != null) {
        FileEntry result = searchFile(fileName, current.data);
        if (result != null) return result;
        current = current.next;
    }

    return null;
}

/**
 * Retorna la ruta completa de un archivo buscándolo desde la raíz.
 *
 * @param fileName Nombre del archivo.
 * @return La ruta completa, o null si no se encuentra.
 */
public String getFilePath(String fileName) {
    return getFilePathRecursive(fileName, root, "/root");
}

/**
 * Método auxiliar recursivo para construir la ruta de un archivo.
 */
private String getFilePathRecursive(String fileName,
                                     DirectoryEntry dir,
                                     String currentPath) {
    if (dir.findFile(fileName) != null) {
        return currentPath + "/" + fileName;
    }

    filesystem.datastructures.Node<DirectoryEntry> current =
        dir.getSubDirectories().getHead();
    while (current != null) {
        String path = getFilePathRecursive(
            fileName,
            current.data,
            currentPath + "/" + current.data.getName()
        );
        if (path != null) return path;
        current = current.next;
    }

    return null;
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

