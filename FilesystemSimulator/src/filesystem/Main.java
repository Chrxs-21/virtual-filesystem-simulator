package filesystem;

import filesystem.model.DirectoryEntry;
import filesystem.model.FileEntry;
import filesystem.model.FileSystem;
import filesystem.model.UserMode;

public class Main {
    public static void main(String[] args) {

        FileSystem fs = new FileSystem();
        DirectoryEntry root = fs.getRoot();

        // ── Crear directorios ──────────────────────────────────────────
        DirectoryEntry documentos = fs.createDirectory("documentos", root);
        DirectoryEntry imagenes   = fs.createDirectory("imagenes", root);
        System.out.println("Directorios creados: documentos, imagenes");

        // ── Crear archivos ─────────────────────────────────────────────
        FileEntry archivo1 = fs.createFile(
            "notas.txt", "Apuntes de sistemas operativos", documentos, false
        );
        FileEntry archivo2 = fs.createFile(
            "resumen.txt", "Resumen del proyecto 2", documentos, false
        );
        fs.createFile("foto.png", "datos de imagen", imagenes, true);
        System.out.println("\nArchivos creados:");
        System.out.println("  " + archivo1);
        System.out.println("  " + archivo2);
        System.out.println(fs.getDiskInfo());

        // ── Leer archivo ───────────────────────────────────────────────
        System.out.println("\nLeyendo notas.txt: "
            + fs.readFile(archivo1));

        // ── Renombrar archivo ──────────────────────────────────────────
        fs.renameFile("notas.txt", "apuntes.txt", documentos);
        System.out.println("\nRenombrado: notas.txt → apuntes.txt");

        // ── Actualizar contenido ───────────────────────────────────────
        fs.updateFile("resumen.txt",
            "Resumen actualizado del proyecto 2 - Hito 3", documentos);
        System.out.println("Actualizado: resumen.txt");
        System.out.println("Nuevo contenido: "
            + fs.readFile(documentos.findFile("resumen.txt")));

        // ── Buscar archivo ─────────────────────────────────────────────
        FileEntry encontrado = fs.searchFile("apuntes.txt", root);
        System.out.println("\nBúsqueda de apuntes.txt: "
            + (encontrado != null ? "encontrado" : "no encontrado"));
        System.out.println("Ruta: " + fs.getFilePath("apuntes.txt"));

        // ── Probar permisos de usuario ─────────────────────────────────
        fs.setCurrentMode(UserMode.USER);
        System.out.println("\nCambiando a modo USUARIO...");
        try {
            fs.deleteFile("foto.png", imagenes); // solo lectura
        } catch (IllegalStateException e) {
            System.out.println("Bloqueado correctamente: " + e.getMessage());
        }
        try {
            fs.createDirectory("nueva", root); // solo admin
        } catch (IllegalStateException e) {
            System.out.println("Bloqueado correctamente: " + e.getMessage());
        }

        // ── Eliminar directorio recursivo (volvemos a admin) ──────────
        fs.setCurrentMode(UserMode.ADMINISTRATOR);
        System.out.println("\nEliminando directorio 'documentos'...");
        fs.deleteDirectory("documentos", root);
        System.out.println(fs.getDiskInfo());

        System.out.println("\nHito 3 completado correctamente.");
    }
}