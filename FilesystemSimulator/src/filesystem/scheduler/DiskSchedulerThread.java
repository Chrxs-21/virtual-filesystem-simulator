package filesystem.scheduler;

import filesystem.datastructures.Node;
import filesystem.gui.panels.LogPanel;
import filesystem.gui.panels.ProcessQueuePanel;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hilo dedicado para el procesamiento de solicitudes de E/S.
 *
 * Procesa las solicitudes del planificador en segundo plano, permitiendo que la
 * GUI siga respondiendo mientras se ejecuta.
 *
 * Usa un Semaphore para garantizar acceso exclusivo al disco durante cada
 * operacion, simulando el comportamiento real de un controlador de disco en un
 * sistema operativo.
 *
 * Patron de uso: 1. Crear el hilo con el planificador y los paneles de la GUI
 * 2. Llamar start() para iniciar el procesamiento 3. Llamar stop() para
 * detenerlo limpiamente
 */
public class DiskSchedulerThread extends Thread {

    /**
     * Planificador de disco a ejecutar.
     */
    private final DiskScheduler scheduler;

    /**
     * Semaforo que controla el acceso exclusivo al disco. Solo un proceso puede
     * operar el disco a la vez. Se inicializa con 1 permiso (mutex binario).
     */
    private final Semaphore diskSemaphore;

    /**
     * Panel del log para reportar eventos en tiempo real.
     */
    private final LogPanel logPanel;

    /**
     * Panel de la cola para actualizarse tras cada operacion.
     */
    private final ProcessQueuePanel processQueuePanel;

    /**
     * Bandera atomica para detener el hilo limpiamente. AtomicBoolean garantiza
     * visibilidad entre hilos sin necesidad de synchronized.
     */
    private final AtomicBoolean running;

    /**
     * Delay en milisegundos entre cada solicitud procesada. Permite visualizar
     * el movimiento del cabezal en tiempo real.
     */
    private int delayMs;

    /**
     * Callback que se ejecuta en el EDT de Swing tras cada paso.
     */
    private Runnable onStepCallback;

    /**
     * Crea el hilo del planificador.
     *
     * @param scheduler El planificador con las solicitudes.
     * @param diskSemaphore Semaforo de acceso al disco.
     * @param logPanel Panel del log de la GUI.
     * @param processQueuePanel Panel de la cola de la GUI.
     * @param delayMs Milisegundos entre cada operacion.
     */
    public DiskSchedulerThread(DiskScheduler scheduler,
            Semaphore diskSemaphore,
            LogPanel logPanel,
            ProcessQueuePanel processQueuePanel,
            int delayMs) {
        this.scheduler = scheduler;
        this.diskSemaphore = diskSemaphore;
        this.logPanel = logPanel;
        this.processQueuePanel = processQueuePanel;
        this.delayMs = delayMs;
        this.running = new AtomicBoolean(false);

        // Nombre descriptivo para identificar el hilo en depuracion
        setName("DiskScheduler-Thread");

        // Hilo daemon: se detiene automaticamente al cerrar la app
        setDaemon(true);
    }

    /**
     * Logica principal del hilo. Procesa solicitudes una por una con delay
     * entre cada una, adquiriendo y liberando el semaforo en cada operacion.
     */
    @Override
    public void run() {
        running.set(true);
        logFromThread("Hilo del planificador iniciado. "
                + "Politica: " + scheduler.getPolicy());

        while (running.get() && !scheduler.isQueueEmpty()) {
            try {
                // Notificar que se intenta adquirir el semaforo
                logFromThread("Semaforo: solicitando acceso al disco...");

                // Adquirir el semaforo antes de operar el disco
                diskSemaphore.acquire();
                logFromThread("Semaforo adquirido. Procesando solicitud...");

                try {
                    if (!scheduler.isQueueEmpty()) {
                        IORequest attended = scheduler.processNext();
                        reportStep(attended);
                    }
                } finally {
                    // Siempre liberar el semaforo
                    diskSemaphore.release();
                    logFromThread("Semaforo liberado.");
                }

                // Actualizar GUI desde el EDT de Swing
                updateGUI();

                // Pausa para visualizacion en tiempo real
                Thread.sleep(delayMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logFromThread("Error en planificador: "
                        + e.getMessage());
            }
        }

        running.set(false);
        logFromThread("Hilo del planificador finalizado. "
                + "Movimiento total: "
                + scheduler.getTotalHeadMovement()
                + " cilindros.");

        updateGUI();
    }

    // ─── METODOS DE SOPORTE ─────────────────────────────────────────────────
    /**
     * Reporta el resultado de procesar una solicitud.
     */
    private void reportStep(IORequest attended) {
        String msg = "Atendido: cilindro "
                + attended.getCylinderPosition()
                + " (" + attended.getOperationType() + ")"
                + " | Cabezal en: " + scheduler.getHeadPosition()
                + " | Movimiento total: "
                + scheduler.getTotalHeadMovement();
        logFromThread(msg);
    }

    /**
     * Envia un mensaje al log desde el hilo de fondo. Usa
     * SwingUtilities.invokeLater para respetar el EDT.
     */
    private void logFromThread(String message) {
        javax.swing.SwingUtilities.invokeLater(()
                -> logPanel.logOK(message)
        );
    }

    /**
     * Actualiza los paneles de la GUI desde el EDT de Swing. NUNCA actualizar
     * componentes Swing desde un hilo de fondo directamente, siempre usar
     * invokeLater.
     */
    private void updateGUI() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            processQueuePanel.refresh(scheduler);
            if (onStepCallback != null) {
                onStepCallback.run();
            }
        });
    }

    // ─── CONTROL DEL HILO ───────────────────────────────────────────────────
    /**
     * Detiene el hilo limpiamente al terminar la solicitud actual. No
     * interrumpe abruptamente, espera el ciclo actual.
     */
    public void stopGracefully() {
        running.set(false);
        logFromThread("Hilo del planificador detenido.");
    }

    /**
     * @return true si el hilo esta procesando activamente.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Configura un callback que se ejecuta en el EDT despues de cada solicitud
     * procesada. Util para actualizar otros paneles de la GUI.
     *
     * @param callback El Runnable a ejecutar tras cada paso.
     */
    public void setOnStepCallback(Runnable callback) {
        this.onStepCallback = callback;
    }

    /**
     * @return El delay actual entre operaciones en ms.
     */
    public int getDelayMs() {
        return delayMs;
    }

    /**
     * Cambia el delay entre operaciones en tiempo real.
     *
     * @param delayMs Nuevo delay en milisegundos.
     */
    public void setDelayMs(int delayMs) {
        this.delayMs = delayMs;
    }
}
