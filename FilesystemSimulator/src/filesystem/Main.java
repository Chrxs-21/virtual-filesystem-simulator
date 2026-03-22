package filesystem;

import filesystem.model.DirectoryEntry;
import filesystem.model.FileEntry;
import filesystem.model.FileSystem;

public class Main {
    public static void main(String[] args) {

        FileSystem fs = new FileSystem();
        DirectoryEntry root = fs.getRoot();

        // Crear un archivo
        FileEntry file = fs.createFile(
            "documento.txt",
            "Hola mundo desde el simulador",
            root,
            false
        );

        System.out.println("Archivo creado: " + file);
        System.out.println(fs.getDiskInfo());

        // Leer el archivo
        String content = fs.readFile(file);
        System.out.println("Contenido: " + content);

        // Eliminar el archivo
        fs.deleteFile("documento.txt", root);
        System.out.println("Archivo eliminado.");
        System.out.println(fs.getDiskInfo());
    }
}