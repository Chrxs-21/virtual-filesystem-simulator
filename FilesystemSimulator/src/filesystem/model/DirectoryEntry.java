package filesystem.model;

import filesystem.datastructures.LinkedList;

/**
 * Representa un directorio dentro del sistema de archivos.
 * Contiene subdirectorios y archivos usando listas enlazadas propias.
 * No usa java.util.ArrayList ni ninguna colección del JCF.
 */
public class DirectoryEntry {

    /** Nombre del directorio. */
    private String name;

    /** Referencia al directorio padre. null si es la raíz. */
    private DirectoryEntry parent;

    /**
     * Subdirectorios contenidos en este directorio.
     * Usa LinkedList propia, no ArrayList.
     */
    private final LinkedList<DirectoryEntry> subDirectories;

    /**
     * Archivos contenidos en este directorio.
     * Usa LinkedList propia, no ArrayList.
     */
    private final LinkedList<FileEntry> files;

    /**
     * Crea un directorio vacío.
     * @param name   Nombre del directorio.
     * @param parent Directorio padre, null si es raíz.
     */
    public DirectoryEntry(String name, DirectoryEntry parent) {
        this.name           = name;
        this.parent         = parent;
        this.subDirectories = new LinkedList<>();
        this.files          = new LinkedList<>();
    }

    // ─── ARCHIVOS ───────────────────────────────────────────────────────────

    /**
     * Agrega un archivo a este directorio.
     * @param file El archivo a agregar.
     */
    public void addFile(FileEntry file) {
        files.addLast(file);
    }

    /**
     * Elimina un archivo de este directorio por nombre.
     * @param fileName Nombre del archivo a eliminar.
     * @return true si fue encontrado y eliminado.
     */
    public boolean removeFile(String fileName) {
        FileEntry target = findFile(fileName);
        if (target == null) return false;
        return files.remove(target);
    }

    /**
     * Busca un archivo por nombre en este directorio. O(n).
     * @param fileName Nombre a buscar.
     * @return El FileEntry encontrado, o null si no existe.
     */
    public FileEntry findFile(String fileName) {
        filesystem.datastructures.Node<FileEntry> current = files.getHead();
        while (current != null) {
            if (current.data.getName().equals(fileName)) {
                return current.data;
            }
            current = current.next;
        }
        return null;
    }

    // ─── SUBDIRECTORIOS ─────────────────────────────────────────────────────

    /**
     * Agrega un subdirectorio a este directorio.
     * @param dir El subdirectorio a agregar.
     */
    public void addSubDirectory(DirectoryEntry dir) {
        subDirectories.addLast(dir);
    }

    /**
     * Elimina un subdirectorio por nombre.
     * @param dirName Nombre del subdirectorio.
     * @return true si fue encontrado y eliminado.
     */
    public boolean removeSubDirectory(String dirName) {
        DirectoryEntry target = findSubDirectory(dirName);
        if (target == null) return false;
        return subDirectories.remove(target);
    }

    /**
     * Busca un subdirectorio por nombre. O(n).
     * @param dirName Nombre a buscar.
     * @return El DirectoryEntry encontrado, o null si no existe.
     */
    public DirectoryEntry findSubDirectory(String dirName) {
        filesystem.datastructures.Node<DirectoryEntry> current =
            subDirectories.getHead();
        while (current != null) {
            if (current.data.getName().equals(dirName)) {
                return current.data;
            }
            current = current.next;
        }
        return null;
    }

    // ─── GETTERS ────────────────────────────────────────────────────────────

    public String getName()    { return name; }
    public DirectoryEntry getParent() { return parent; }
    public LinkedList<FileEntry> getFiles() { return files; }
    public LinkedList<DirectoryEntry> getSubDirectories() {
        return subDirectories;
    }

    /** @return true si no tiene archivos ni subdirectorios. */
    public boolean isEmpty() {
        return files.isEmpty() && subDirectories.isEmpty();
    }

    // ─── SETTERS ────────────────────────────────────────────────────────────

    public void setName(String name)     { this.name = name; }
    public void setParent(DirectoryEntry parent) { this.parent = parent; }

    /**
     * Retorna la ruta completa desde la raíz. Ej: /home/documentos
     */
    public String getFullPath() {
        if (parent == null) return "/" + name;
        return parent.getFullPath() + "/" + name;
    }

    @Override
    public String toString() { return name; }
}