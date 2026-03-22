package filesystem;

import filesystem.concurrency.FileLock;
import filesystem.concurrency.LockManager;
import filesystem.datastructures.Node;

public class Main {
    public static void main(String[] args) {

        LockManager manager = new LockManager();

        // ── Multiples lectores simultaneos ─────────────────────────────
        System.out.println("── Prueba de locks compartidos ──");
        boolean r1 = manager.acquireSharedLock("documento.txt", "Proceso-A");
        boolean r2 = manager.acquireSharedLock("documento.txt", "Proceso-B");
        boolean r3 = manager.acquireSharedLock("documento.txt", "Proceso-C");
        System.out.println("Proceso-A lock lectura: " + (r1 ? "OTORGADO" : "BLOQUEADO"));
        System.out.println("Proceso-B lock lectura: " + (r2 ? "OTORGADO" : "BLOQUEADO"));
        System.out.println("Proceso-C lock lectura: " + (r3 ? "OTORGADO" : "BLOQUEADO"));

        FileLock lock = manager.getLock("documento.txt");
        System.out.println("Lectores activos: " + lock.getActiveReaders());

        // ── Escritor bloqueado por lectores activos ────────────────────
        System.out.println("\n── Escritor intenta acceder ──");
        boolean w1 = manager.acquireExclusiveLock("documento.txt", "Proceso-W");
        System.out.println("Proceso-W lock escritura: " + (w1 ? "OTORGADO" : "BLOQUEADO"));
        System.out.println("Procesos en espera: " + lock.getWaitingCount());

        // ── Lectores liberan sus locks ─────────────────────────────────
        System.out.println("\n── Lectores liberan locks ──");
        manager.releaseSharedLock("documento.txt", "Proceso-A");
        System.out.println("Proceso-A libero lock. Lectores activos: "
            + lock.getActiveReaders());
        manager.releaseSharedLock("documento.txt", "Proceso-B");
        System.out.println("Proceso-B libero lock. Lectores activos: "
            + lock.getActiveReaders());
        manager.releaseSharedLock("documento.txt", "Proceso-C");
        System.out.println("Proceso-C libero lock. Lectores activos: "
            + lock.getActiveReaders());

        // ── Escritor despierta automaticamente ────────────────────────
        System.out.println("\n── Estado del lock tras liberar lectores ──");
        System.out.println("Tipo de lock activo: " + lock.getCurrentLockType());
        System.out.println("Dueno del lock exclusivo: " + lock.getExclusiveOwner());

        // ── Escritor libera, nuevo lector entra ───────────────────────
        System.out.println("\n── Escritor libera lock ──");
        manager.releaseExclusiveLock("documento.txt", "Proceso-W");
        System.out.println("Tipo de lock activo: " + lock.getCurrentLockType());

        // ── Prueba con dos archivos distintos ─────────────────────────
        System.out.println("\n── Locks independientes por archivo ──");
        manager.acquireExclusiveLock("archivo1.txt", "Proceso-X");
        manager.acquireSharedLock("archivo2.txt", "Proceso-Y");
        System.out.println("Locks activos en el sistema: "
            + manager.getActiveLocks().size());

        System.out.println("\nHito 5 completado correctamente.");
    }
}