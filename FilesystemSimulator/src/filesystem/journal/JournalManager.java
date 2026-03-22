package filesystem.journal;

import filesystem.datastructures.LinkedList;
import filesystem.datastructures.Node;
import filesystem.model.DirectoryEntry;
import filesystem.model.FileSystem;

/**
 * Gestor del journal del sistema de archivos.
 *
 * Coordina el registro de operaciones, su confirmacion y la
 * recuperacion ante fallos mediante UNDO de entradas PENDING.
 *
 * Usa LinkedList propia para almacenar las entradas del journal.
 * No usa ninguna clase del Java Collections Framework.
 */
public class JournalManager {

    /**
     * Lista de todas las entradas del journal en orden cronologico.
     * Usa LinkedList propia, no ArrayList.
     */
    private final LinkedList<JournalEntry> entries;

    /**
     * Referencia al sistema de archivos para ejecutar operaciones UNDO.
     */
    private final FileSystem fileSystem;

    /**
     * Indica si el sistema esta en estado de fallo simulado.
     * Cuando es true, las operaciones no se confirman automaticamente.
     */
    private boolean crashSimulated;

    /**
     * Crea un gestor de journal vinculado al sistema de archivos.
     * @param fileSystem El sistema de archivos a gestionar.
     */
    public JournalManager(FileSystem fileSystem) {
        this.fileSystem     = fileSystem;
        this.entries        = new LinkedList<>();
        this.crashSimulated = false;
    }

    // ─── REGISTRO DE OPERACIONES ────────────────────────────────────────────

    /**
     * Registra una operacion CREATE_FILE como PENDING y la ejecuta.
     * Si no hay crash simulado, la confirma automaticamente.
     *
     * @param fileName  Nombre del archivo a crear.
     * @param content   Contenido del archivo.
     * @param directory Directorio destino.
     * @param readOnly  true si es de solo lectura.
     * @return La entrada del journal creada.
     */
    public JournalEntry createFile(String fileName,
                                   String content,
                                   DirectoryEntry directory,
                                   boolean readOnly) {
        JournalEntry entry = new JournalEntry(
            JournalEntry.OperationType.CREATE_FILE,
            fileName,
            directory.getFullPath(),
            null,
            content
        );
        entries.addLast(entry);

        if (!crashSimulated) {
            fileSystem.createFile(fileName, content, directory, readOnly);
            entry.setStatus(JournalStatus.CONFIRMED);
        }

        return entry;
    }

    /**
     * Registra una operacion DELETE_FILE como PENDING y la ejecuta.
     *
     * @param fileName  Nombre del archivo a eliminar.
     * @param directory Directorio contenedor.
     * @return La entrada del journal creada.
     */
    public JournalEntry deleteFile(String fileName,
                                   DirectoryEntry directory) {
        // Guardar contenido actual para el UNDO
        String previousContent = "";
        if (directory.findFile(fileName) != null) {
            previousContent = fileSystem.readFile(
                directory.findFile(fileName)
            );
        }

        JournalEntry entry = new JournalEntry(
            JournalEntry.OperationType.DELETE_FILE,
            fileName,
            directory.getFullPath(),
            previousContent,
            null
        );
        entries.addLast(entry);

        if (!crashSimulated) {
            fileSystem.deleteFile(fileName, directory);
            entry.setStatus(JournalStatus.CONFIRMED);
        }

        return entry;
    }

    /**
     * Registra una operacion UPDATE_FILE como PENDING y la ejecuta.
     *
     * @param fileName   Nombre del archivo a actualizar.
     * @param newContent Nuevo contenido.
     * @param directory  Directorio contenedor.
     * @return La entrada del journal creada.
     */
    public JournalEntry updateFile(String fileName,
                                   String newContent,
                                   DirectoryEntry directory) {
        String previousContent = "";
        if (directory.findFile(fileName) != null) {
            previousContent = fileSystem.readFile(
                directory.findFile(fileName)
            );
        }

        JournalEntry entry = new JournalEntry(
            JournalEntry.OperationType.UPDATE_FILE,
            fileName,
            directory.getFullPath(),
            previousContent,
            newContent
        );
        entries.addLast(entry);

        if (!crashSimulated) {
            fileSystem.updateFile(fileName, newContent, directory);
            entry.setStatus(JournalStatus.CONFIRMED);
        }

        return entry;
    }

    /**
     * Registra una operacion RENAME_FILE como PENDING y la ejecuta.
     *
     * @param oldName   Nombre actual del archivo.
     * @param newName   Nuevo nombre.
     * @param directory Directorio contenedor.
     * @return La entrada del journal creada.
     */
    public JournalEntry renameFile(String oldName,
                                   String newName,
                                   DirectoryEntry directory) {
        JournalEntry entry = new JournalEntry(
            JournalEntry.OperationType.RENAME_FILE,
            oldName,
            directory.getFullPath(),
            oldName,
            newName
        );
        entries.addLast(entry);

        if (!crashSimulated) {
            fileSystem.renameFile(oldName, newName, directory);
            entry.setStatus(JournalStatus.CONFIRMED);
        }

        return entry;
    }

    // ─── CONTROL DE FALLO ───────────────────────────────────────────────────

    /**
     * Simula un fallo del sistema. A partir de este punto,
     * las operaciones se registran como PENDING pero no se ejecutan
     * ni se confirman, simulando un crash antes del commit.
     */
    public void simulateCrash() {
        this.crashSimulated = true;
        System.out.println("[JOURNAL] Fallo simulado activado. "
            + "Las operaciones quedaran como PENDING.");
    }

    /**
     * Recupera el sistema tras un fallo simulado.
     * Recorre el journal buscando entradas PENDING y ejecuta
     * su UNDO para dejar el disco en estado consistente.
     *
     * @return Cantidad de operaciones revertidas.
     */
    public int recover() {
        this.crashSimulated = false;
        int rolledBack = 0;

        System.out.println("[JOURNAL] Iniciando recuperacion...");

        Node<JournalEntry> current = entries.getHead();
        while (current != null) {
            JournalEntry entry = current.data;
            if (entry.isPending()) {
                executeUndo(entry);
                entry.setStatus(JournalStatus.ROLLED_BACK);
                rolledBack++;
                System.out.println("[JOURNAL] UNDO aplicado a: "
                    + entry.getOperationType()
                    + " --> " + entry.getTargetName());
            }
            current = current.next;
        }

        System.out.println("[JOURNAL] Recuperacion completada. "
            + rolledBack + " operacion(es) revertida(s).");
        return rolledBack;
    }

    // ─── UNDO ───────────────────────────────────────────────────────────────

    /**
     * Ejecuta la operacion inversa de una entrada PENDING.
     * Cada tipo de operacion tiene su propio mecanismo de UNDO.
     *
     * @param entry La entrada a revertir.
     */
    private void executeUndo(JournalEntry entry) {
        DirectoryEntry directory = findDirectory(entry.getTargetPath());
        if (directory == null) return;

        switch (entry.getOperationType()) {
            case CREATE_FILE -> {
                // UNDO de crear = eliminar el archivo si existe
                if (directory.findFile(entry.getTargetName()) != null) {
                    fileSystem.deleteFile(entry.getTargetName(), directory);
                }
            }
            case DELETE_FILE -> {
                // UNDO de eliminar = recrear el archivo con su contenido
                if (directory.findFile(entry.getTargetName()) == null) {
                    fileSystem.createFile(
                        entry.getTargetName(),
                        entry.getPreviousData() != null
                            ? entry.getPreviousData() : "",
                        directory,
                        false
                    );
                }
            }
            case UPDATE_FILE -> {
                // UNDO de actualizar = restaurar contenido anterior
                if (directory.findFile(entry.getTargetName()) != null) {
                    fileSystem.updateFile(
                        entry.getTargetName(),
                        entry.getPreviousData() != null
                            ? entry.getPreviousData() : "",
                        directory
                    );
                }
            }
            case RENAME_FILE -> {
                // UNDO de renombrar = volver al nombre anterior
                if (directory.findFile(entry.getNewData()) != null) {
                    fileSystem.renameFile(
                        entry.getNewData(),
                        entry.getPreviousData(),
                        directory
                    );
                }
            }
            case CREATE_DIRECTORY -> {
                // UNDO de crear directorio = eliminarlo
                if (directory.findSubDirectory(
                        entry.getTargetName()) != null) {
                    fileSystem.deleteDirectory(
                        entry.getTargetName(), directory
                    );
                }
            }
            case DELETE_DIRECTORY -> {
                // UNDO de eliminar directorio = recrearlo vacio
                if (directory.findSubDirectory(
                        entry.getTargetName()) == null) {
                    fileSystem.createDirectory(
                        entry.getTargetName(), directory
                    );
                }
            }
        }
    }

    // ─── UTILIDADES ─────────────────────────────────────────────────────────

    /**
     * Busca un directorio por su ruta completa desde la raiz.
     * Usado durante el UNDO para localizar el directorio afectado.
     *
     * @param fullPath Ruta completa. Ej: /root/documentos
     * @return El DirectoryEntry encontrado, o null si no existe.
     */
    private DirectoryEntry findDirectory(String fullPath) {
        if (fullPath == null) return fileSystem.getRoot();

        // Separar la ruta en segmentos
        String[] parts = fullPath.split("/");
        DirectoryEntry current = fileSystem.getRoot();

        for (String part : parts) {
            if (part.isEmpty() || part.equals("root")) continue;
            DirectoryEntry next = current.findSubDirectory(part);
            if (next == null) return null;
            current = next;
        }
        return current;
    }

    /**
     * Imprime todas las entradas del journal en consola.
     * Util para depuracion y para la GUI.
     */
    public void printJournal() {
        System.out.println("\n── Journal completo ──");
        Node<JournalEntry> current = entries.getHead();
        if (current == null) {
            System.out.println("  (vacio)");
            return;
        }
        while (current != null) {
            System.out.println("  " + current.data);
            current = current.next;
        }
    }

    // ─── GETTERS ────────────────────────────────────────────────────────────

    public LinkedList<JournalEntry> getEntries() { return entries; }
    public boolean isCrashSimulated()            { return crashSimulated; }

    /**
     * Cuenta las entradas por estado.
     * @param status Estado a contar.
     * @return Cantidad de entradas con ese estado.
     */
    public int countByStatus(JournalStatus status) {
        int count = 0;
        Node<JournalEntry> current = entries.getHead();
        while (current != null) {
            if (current.data.getStatus() == status) count++;
            current = current.next;
        }
        return count;
    }
}