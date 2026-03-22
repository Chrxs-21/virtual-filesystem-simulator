package filesystem;

import filesystem.journal.JournalManager;
import filesystem.journal.JournalStatus;
import filesystem.model.DirectoryEntry;
import filesystem.model.FileSystem;

public class Main {
    public static void main(String[] args) {

        FileSystem fs = new FileSystem();
        JournalManager journal = new JournalManager(fs);
        DirectoryEntry root = fs.getRoot();

        // ── Operaciones normales (con commit automatico) ───────────────
        System.out.println("── Operaciones normales ──");
        journal.createFile("notas.txt", "Contenido inicial", root, false);
        journal.createFile("config.txt", "version=1.0", root, false);
        journal.updateFile("notas.txt", "Contenido actualizado", root);
        journal.printJournal();
        System.out.println("Confirmadas: "
            + journal.countByStatus(JournalStatus.CONFIRMED));
        System.out.println(fs.getDiskInfo());

        // ── Simular crash antes del commit ─────────────────────────────
        System.out.println("\n── Simulando crash ──");
        journal.simulateCrash();
        journal.createFile("temporal.txt", "datos temporales", root, false);
        journal.deleteFile("config.txt", root);
        journal.printJournal();
        System.out.println("Pendientes: "
            + journal.countByStatus(JournalStatus.PENDING));

        // Verificar que los archivos NO cambiaron (crash antes del commit)
        System.out.println("temporal.txt existe: "
            + (root.findFile("temporal.txt") != null));
        System.out.println("config.txt existe: "
            + (root.findFile("config.txt") != null));

        // ── Recuperacion tras el crash ─────────────────────────────────
        System.out.println("\n── Recuperacion ──");
        int revertidas = journal.recover();
        System.out.println("Operaciones revertidas: " + revertidas);
        journal.printJournal();

        // Verificar estado consistente tras el UNDO
        System.out.println("\nEstado tras recuperacion:");
        System.out.println("temporal.txt existe: "
            + (root.findFile("temporal.txt") != null));
        System.out.println("config.txt existe: "
            + (root.findFile("config.txt") != null));
        System.out.println(fs.getDiskInfo());

        System.out.println("\nHito 6 completado correctamente.");
    }
}