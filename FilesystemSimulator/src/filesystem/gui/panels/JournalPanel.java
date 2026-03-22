package filesystem.gui.panels;

import filesystem.datastructures.Node;
import filesystem.journal.JournalEntry;
import filesystem.journal.JournalManager;
import filesystem.journal.JournalStatus;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Panel que muestra el estado del journal del sistema.
 * Colorea las entradas segun su estado:
 * verde = CONFIRMED, amarillo = PENDING, rojo = ROLLED_BACK.
 */
public class JournalPanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable table;

    private static final String[] COLUMNS = {
        "ID", "Operacion", "Archivo", "Estado", "Timestamp"
    };

    public JournalPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Journal"));

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setRowHeight(20);
        table.getTableHeader().setReorderingAllowed(false);

        // Colorear filas segun estado
        table.setDefaultRenderer(Object.class,
            new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(
                        JTable t, Object value, boolean isSelected,
                        boolean hasFocus, int row, int col) {
                    Component c = super.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, col
                    );
                    String status = (String) t.getValueAt(row, 3);
                    if (!isSelected) {
                        if ("CONFIRMED".equals(status)) {
                            c.setBackground(new Color(234, 243, 222));
                        } else if ("PENDING".equals(status)) {
                            c.setBackground(new Color(250, 238, 218));
                        } else if ("ROLLED_BACK".equals(status)) {
                            c.setBackground(new Color(252, 235, 235));
                        } else {
                            c.setBackground(Color.WHITE);
                        }
                    }
                    return c;
                }
            }
        );

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /**
     * Reconstruye la tabla con todas las entradas del journal.
     * @param journal El gestor del journal a mostrar.
     */
    public void refresh(JournalManager journal) {
        tableModel.setRowCount(0);
        Node<JournalEntry> current = journal.getEntries().getHead();
        while (current != null) {
            JournalEntry e = current.data;
            tableModel.addRow(new Object[]{
                e.getId(),
                e.getOperationType().toString(),
                e.getTargetName(),
                e.getStatus().toString(),
                e.getTimestamp().substring(0, 19)
            });
            current = current.next;
        }
    }
}