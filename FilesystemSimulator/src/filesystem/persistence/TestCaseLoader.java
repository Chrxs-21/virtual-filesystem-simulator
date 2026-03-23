package filesystem.persistence;

import filesystem.model.DirectoryEntry;
import filesystem.model.FileSystem;
import filesystem.scheduler.DiskScheduler;
import filesystem.scheduler.IORequest;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Carga casos de prueba desde el formato JSON especificado en el PDF.
 * Permite a las preparadoras cargar los casos P1, P2, P3, P4 y J1
 * directamente desde un archivo con el formato oficial del enunciado.
 *
 * Formato esperado:
 * {
 *   "test_id": "P1",
 *   "initial_head": 50,
 *   "requests": [ {"pos": 95, "op": "READ"}, ... ],
 *   "system_files": {
 *     "95": { "name": "config.sys", "blocks": 4 }, ...
 *   }
 * }
 */
public class TestCaseLoader {

    /**
     * Resultado de cargar un caso de prueba.
     * Contiene el ID del caso, la posicion inicial del cabezal
     * y el planificador ya configurado con las solicitudes.
     */
    public static class TestCaseResult {
        public final String testId;
        public final int initialHead;
        public final DiskScheduler scheduler;

        public TestCaseResult(String testId,
                              int initialHead,
                              DiskScheduler scheduler) {
            this.testId      = testId;
            this.initialHead = initialHead;
            this.scheduler   = scheduler;
        }
    }

    /**
     * Carga un caso de prueba desde un archivo JSON con el formato
     * oficial del PDF y lo aplica al sistema.
     *
     * @param fileSystem Sistema de archivos donde crear los archivos.
     * @param filePath   Ruta del archivo JSON del caso de prueba.
     * @return El resultado con el planificador configurado.
     * @throws IOException si no se puede leer el archivo.
     */
    public static TestCaseResult load(FileSystem fileSystem,
                                       String filePath)
            throws IOException {
        String content = new String(
            Files.readAllBytes(Paths.get(filePath))
        );
        JSONObject root = new JSONObject(content);

        String testId    = root.getString("test_id");
        int initialHead  = root.getInt("initial_head");

        // Crear archivos del sistema en el disco
        loadSystemFiles(root, fileSystem);

        // Cargar solicitudes en el planificador
        DiskScheduler scheduler = loadRequests(root, initialHead);

        System.out.println("[TEST] Caso " + testId
            + " cargado. Cabezal inicial: " + initialHead);

        return new TestCaseResult(testId, initialHead, scheduler);
    }

    // ─── CARGA DE ARCHIVOS DEL SISTEMA ──────────────────────────────────────

    /**
     * Crea en el sistema de archivos los archivos definidos
     * en el campo "system_files" del JSON del caso de prueba.
     * Cada archivo se crea con el numero de bloques especificado.
     */
    private static void loadSystemFiles(JSONObject root,
                                         FileSystem fileSystem) {
        if (!root.has("system_files")) return;

        JSONObject systemFiles = root.getJSONObject("system_files");
        DirectoryEntry rootDir = fileSystem.getRoot();

        // Iterar sobre cada cilindro como clave
        for (String cylinderKey : systemFiles.keySet()) {
            JSONObject fileInfo =
                systemFiles.getJSONObject(cylinderKey);
            String fileName  = fileInfo.getString("name");
            int blocksNeeded = fileInfo.getInt("blocks");

            // Verificar que no exista ya
            if (rootDir.findFile(fileName) != null) continue;

            // Crear contenido simulado del tamanio correcto
            String simulatedContent = "x".repeat(
                blocksNeeded * FileSystem.BLOCK_SIZE
            );

            try {
                fileSystem.createFile(
                    fileName,
                    simulatedContent,
                    rootDir,
                    false
                );
                System.out.println("[TEST] Archivo creado: "
                    + fileName + " (" + blocksNeeded + " bloques)");
            } catch (Exception e) {
                System.out.println("[TEST] No se pudo crear "
                    + fileName + ": " + e.getMessage());
            }
        }
    }

    // ─── CARGA DE SOLICITUDES E/S ───────────────────────────────────────────

    /**
     * Crea un planificador configurado con las solicitudes
     * del campo "requests" del JSON del caso de prueba.
     */
    private static DiskScheduler loadRequests(JSONObject root,
                                               int initialHead) {
        // La politica se configurara desde la GUI
        // por defecto iniciamos con FIFO
        DiskScheduler scheduler = new DiskScheduler(
            initialHead,
            filesystem.scheduler.SchedulerPolicy.FIFO
        );

        if (!root.has("requests")) return scheduler;

        JSONArray requests = root.getJSONArray("requests");
        for (int i = 0; i < requests.length(); i++) {
            JSONObject req = requests.getJSONObject(i);
            int pos        = req.getInt("pos");
            String opStr   = req.getString("op");

            IORequest.OperationType op =
                opStr.equals("READ")
                ? IORequest.OperationType.READ
                : IORequest.OperationType.WRITE;

            scheduler.addRequest(new IORequest(
                "Proceso-" + (i + 1), pos, op
            ));
        }
        return scheduler;
    }
}