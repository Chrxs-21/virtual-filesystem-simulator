package filesystem.persistence;

import filesystem.journal.JournalEntry;
import filesystem.journal.JournalManager;
import filesystem.journal.JournalStatus;
import filesystem.model.DirectoryEntry;
import filesystem.model.DiskBlock;
import filesystem.model.FileEntry;
import filesystem.model.FileSystem;
import filesystem.model.UserMode;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Carga el estado del sistema de archivos desde un archivo JSON. Reconstruye el
 * disco, el arbol de directorios y el journal. No usa ninguna clase del Java
 * Collections Framework.
 */
public class JsonLoader {

    /**
     * Carga el estado del sistema desde el archivo indicado.
     *
     * @param fileSystem El sistema de archivos a restaurar.
     * @param journal El journal a restaurar.
     * @param filePath Ruta del archivo .json a leer.
     * @throws IOException si no se puede leer el archivo.
     */
    public static void load(FileSystem fileSystem,
            JournalManager journal,
            String filePath) throws IOException {
        String content = new String(
                Files.readAllBytes(Paths.get(filePath))
        );
        JSONObject root = new JSONObject(content);

        // Restaurar modo del sistema
        String mode = root.getString("currentMode");
        fileSystem.setCurrentMode(
                UserMode.valueOf(mode)
        );

        // Restaurar disco bloque por bloque
        loadDisk(root.getJSONArray("disk"), fileSystem.getDisk());

        // Restaurar arbol de directorios
        loadDirectory(
                root.getJSONObject("root"),
                fileSystem.getRoot(),
                null
        );

        // Restaurar journal
        loadJournal(root.getJSONArray("journal"), journal);

        System.out.println("[JSON] Estado cargado desde: " + filePath);
    }

    // ─── CARGA DEL DISCO ────────────────────────────────────────────────────
    private static void loadDisk(JSONArray diskArray,
            DiskBlock[] disk) {
        // Limpiar bitmap antes de restaurar
        for (DiskBlock block : disk) {
            block.clear();
        }

        for (int i = 0; i < diskArray.length(); i++) {
            JSONObject blockObj = diskArray.getJSONObject(i);
            int index = blockObj.getInt("index");
            DiskBlock block = disk[index];

            if (!blockObj.isNull("ownerFile")) {
                block.setOwnerFile(blockObj.getString("ownerFile"));
            }
            block.setContent(blockObj.getString("content"));
            block.setNextBlock(blockObj.getInt("nextBlock"));
        }
    }

    // ─── CARGA DEL ÁRBOL ────────────────────────────────────────────────────
    private static void loadDirectory(JSONObject dirObj,
            DirectoryEntry dir,
            DirectoryEntry parent) {
        // Limpiar directorio antes de restaurar
        dir.getFiles().clear();
        dir.getSubDirectories().clear();

        // Restaurar archivos
        JSONArray filesArray = dirObj.getJSONArray("files");
        for (int i = 0; i < filesArray.length(); i++) {
            JSONObject fileObj = filesArray.getJSONObject(i);
            FileEntry file = new FileEntry(
                    fileObj.getString("name"),
                    fileObj.getBoolean("readOnly"),
                    fileObj.getString("createdAt")
            );
            file.setFirstBlock(fileObj.getInt("firstBlock"));
            file.setSize(fileObj.getInt("size"));
            file.setBlockCount(fileObj.getInt("blockCount"));
            dir.addFile(file);
        }

        // Restaurar subdirectorios recursivamente
        JSONArray subDirsArray = dirObj.getJSONArray("subDirectories");
        for (int i = 0; i < subDirsArray.length(); i++) {
            JSONObject subDirObj = subDirsArray.getJSONObject(i);
            DirectoryEntry subDir = new DirectoryEntry(
                    subDirObj.getString("name"),
                    dir
            );
            dir.addSubDirectory(subDir);
            loadDirectory(subDirObj, subDir, dir);
        }
    }

    // ─── CARGA DEL JOURNAL ──────────────────────────────────────────────────
    private static void loadJournal(JSONArray journalArray,
            JournalManager journal) {
        JournalEntry.resetIdCounter();
        journal.getEntries().clear();

        for (int i = 0; i < journalArray.length(); i++) {
            JSONObject entryObj = journalArray.getJSONObject(i);

            JournalEntry entry = new JournalEntry(
                    JournalEntry.OperationType.valueOf(
                            entryObj.getString("operationType")
                    ),
                    entryObj.getString("targetName"),
                    entryObj.getString("targetPath"),
                    entryObj.isNull("previousData")
                    ? null : entryObj.getString("previousData"),
                    entryObj.isNull("newData")
                    ? null : entryObj.getString("newData")
            );
            entry.setStatus(JournalStatus.valueOf(
                    entryObj.getString("status")
            ));
            journal.getEntries().addLast(entry);
        }
    }
}
