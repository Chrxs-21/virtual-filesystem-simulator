package filesystem.persistence;

import filesystem.datastructures.Node;
import filesystem.journal.JournalEntry;
import filesystem.journal.JournalManager;
import filesystem.model.DirectoryEntry;
import filesystem.model.DiskBlock;
import filesystem.model.FileEntry;
import filesystem.model.FileSystem;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Guarda el estado completo del sistema de archivos en un archivo JSON.
 * Serializa el disco, el arbol de directorios y el journal. No usa ninguna
 * clase del Java Collections Framework.
 */
public class JsonSaver {

    /**
     * Guarda el estado completo del sistema en el archivo indicado.
     *
     * @param fileSystem El sistema de archivos a guardar.
     * @param journal El journal a guardar.
     * @param filePath Ruta del archivo .json de destino.
     * @throws IOException si no se puede escribir el archivo.
     */
    public static void save(FileSystem fileSystem,
            JournalManager journal,
            String filePath) throws IOException {
        JSONObject root = new JSONObject();

        // Guardar configuracion del disco
        root.put("totalBlocks", FileSystem.TOTAL_BLOCKS);
        root.put("currentMode", fileSystem.getCurrentMode().toString());

        // Guardar estado del disco bloque por bloque
        root.put("disk", saveDisk(fileSystem.getDisk()));

        // Guardar arbol de directorios
        root.put("root", saveDirectory(fileSystem.getRoot()));

        // Guardar journal
        root.put("journal", saveJournal(journal));

        // Escribir en disco con indentacion de 2 espacios
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(root.toString(2));
        }

        System.out.println("[JSON] Estado guardado en: " + filePath);
    }

    // ─── SERIALIZACIÓN DEL DISCO ────────────────────────────────────────────
    private static JSONArray saveDisk(DiskBlock[] disk) {
        JSONArray diskArray = new JSONArray();
        for (DiskBlock block : disk) {
            JSONObject blockObj = new JSONObject();
            blockObj.put("index", block.getIndex());
            blockObj.put("ownerFile", block.getOwnerFile() == null
                    ? JSONObject.NULL : block.getOwnerFile());
            blockObj.put("content", block.getContent());
            blockObj.put("nextBlock", block.getNextBlock());
            diskArray.put(blockObj);
        }
        return diskArray;
    }

    // ─── SERIALIZACIÓN DEL ÁRBOL ────────────────────────────────────────────
    private static JSONObject saveDirectory(DirectoryEntry dir) {
        JSONObject dirObj = new JSONObject();
        dirObj.put("name", dir.getName());

        // Guardar archivos del directorio
        JSONArray filesArray = new JSONArray();
        Node<FileEntry> currentFile = dir.getFiles().getHead();
        while (currentFile != null) {
            filesArray.put(saveFile(currentFile.data));
            currentFile = currentFile.next;
        }
        dirObj.put("files", filesArray);

        // Guardar subdirectorios recursivamente
        JSONArray subDirsArray = new JSONArray();
        Node<DirectoryEntry> currentDir
                = dir.getSubDirectories().getHead();
        while (currentDir != null) {
            subDirsArray.put(saveDirectory(currentDir.data));
            currentDir = currentDir.next;
        }
        dirObj.put("subDirectories", subDirsArray);

        return dirObj;
    }

    private static JSONObject saveFile(FileEntry file) {
        JSONObject fileObj = new JSONObject();
        fileObj.put("name", file.getName());
        fileObj.put("firstBlock", file.getFirstBlock());
        fileObj.put("size", file.getSize());
        fileObj.put("readOnly", file.isReadOnly());
        fileObj.put("createdAt", file.getCreatedAt());
        fileObj.put("blockCount", file.getBlockCount());
        return fileObj;
    }

    // ─── SERIALIZACIÓN DEL JOURNAL ──────────────────────────────────────────
    private static JSONArray saveJournal(JournalManager journal) {
        JSONArray journalArray = new JSONArray();
        Node<JournalEntry> current = journal.getEntries().getHead();
        while (current != null) {
            JournalEntry entry = current.data;
            JSONObject entryObj = new JSONObject();
            entryObj.put("id", entry.getId());
            entryObj.put("operationType", entry.getOperationType().toString());
            entryObj.put("targetName", entry.getTargetName());
            entryObj.put("targetPath", entry.getTargetPath());
            entryObj.put("previousData", entry.getPreviousData() == null
                    ? JSONObject.NULL : entry.getPreviousData());
            entryObj.put("newData", entry.getNewData() == null
                    ? JSONObject.NULL : entry.getNewData());
            entryObj.put("status", entry.getStatus().toString());
            entryObj.put("timestamp", entry.getTimestamp());
            journalArray.put(entryObj);
            current = current.next;
        }
        return journalArray;
    }
}
