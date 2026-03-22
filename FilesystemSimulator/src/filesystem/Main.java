package filesystem;

import filesystem.scheduler.DiskScheduler;
import filesystem.scheduler.IORequest;
import filesystem.scheduler.IORequest.OperationType;
import filesystem.scheduler.SchedulerPolicy;
import filesystem.datastructures.Node;

public class Main {
    public static void main(String[] args) {

        // Solicitudes del ejemplo clásico del PDF
        int[] cylinders = {98, 183, 37, 122, 14, 124, 65, 67};
        int initialHead = 53;

        SchedulerPolicy[] policies = {
            SchedulerPolicy.FIFO,
            SchedulerPolicy.SSTF,
            SchedulerPolicy.SCAN,
            SchedulerPolicy.C_SCAN
        };

        for (SchedulerPolicy policy : policies) {
            IORequest.resetIdCounter();
            DiskScheduler scheduler = new DiskScheduler(initialHead, policy);

            // Cargar las solicitudes
            for (int cyl : cylinders) {
                scheduler.addRequest(new IORequest(
                    "Proceso-" + cyl, cyl, OperationType.READ
                ));
            }

            // Procesar todas
            scheduler.processAll();

            // Mostrar resultados
            System.out.println("\n── " + policy + " ──");
            System.out.print("Orden atendido: " + initialHead);
            Node<IORequest> current =
                scheduler.getAttendedRequests().getHead();
            while (current != null) {
                System.out.print(" → " + current.data.getCylinderPosition());
                current = current.next;
            }
            System.out.println();
            System.out.println("Movimiento total: "
                + scheduler.getTotalHeadMovement() + " cilindros");
        }
    }
}