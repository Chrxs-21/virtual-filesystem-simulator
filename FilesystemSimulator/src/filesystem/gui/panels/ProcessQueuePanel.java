package filesystem.gui.panels;

import filesystem.datastructures.Node;
import filesystem.scheduler.DiskScheduler;
import filesystem.scheduler.IORequest;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Panel que muestra la cola de procesos de E/S pendientes
 * y el historial de solicitudes atendidas por el planificador.
 */
public class ProcessQueuePanel extends JPanel {

    private final DefaultTableModel queueModel;
    private final DefaultTableModel historyModel;
    private final JLabel movementLabel;

    private static final String[] QUEUE_COLS = {
        "ID", "Proceso", "Cilindro", "Operacion", "Estado"
    };
    private static final String[] HISTORY_COLS = {
        "ID", "Proceso", "Cilindro", "Operacion"
    };

    public ProcessQueuePanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(
            "Cola de procesos E/S"
        ));

        // Panel superior: cola pendiente
        queueModel = new DefaultTableModel(QUEUE_COLS, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable queueTable = new JTable(queueModel);
        queueTable.setRowHeight(20);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder(
            "Pendientes"
        ));
        topPanel.add(new JScrollPane(queueTable), BorderLayout.CENTER);

        // Panel inferior: historial atendido
        historyModel = new DefaultTableModel(HISTORY_COLS, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable historyTable = new JTable(historyModel);
        historyTable.setRowHeight(20);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder(
            "Atendidos"
        ));
        bottomPanel.add(
            new JScrollPane(historyTable), BorderLayout.CENTER
        );

        // Label de movimiento total
        movementLabel = new JLabel(
            "Movimiento total del cabezal: 0 cilindros"
        );
        movementLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        movementLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JSplitPane split = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel
        );
        split.setResizeWeight(0.5);

        add(split, BorderLayout.CENTER);
        add(movementLabel, BorderLayout.SOUTH);
    }

    /**
     * Actualiza los paneles con el estado actual del planificador.
     * @param scheduler El planificador de disco activo.
     */
    public void refresh(DiskScheduler scheduler) {
        // Cola pendiente
        queueModel.setRowCount(0);
        Node<IORequest> pending = scheduler.getAttendedRequests().getHead();

        // Historial atendido
        historyModel.setRowCount(0);
        Node<IORequest> attended =
            scheduler.getAttendedRequests().getHead();
        while (attended != null) {
            IORequest r = attended.data;
            historyModel.addRow(new Object[]{
                r.getId(),
                r.getProcessName(),
                r.getCylinderPosition(),
                r.getOperationType()
            });
            attended = attended.next;
        }

        movementLabel.setText(
            "Movimiento total del cabezal: "
            + scheduler.getTotalHeadMovement() + " cilindros"
        );
    }
}