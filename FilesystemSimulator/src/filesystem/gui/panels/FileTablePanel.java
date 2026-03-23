package filesystem.gui.panels;

import filesystem.datastructures.Node;
import filesystem.model.DirectoryEntry;
import filesystem.model.FileEntry;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Panel que muestra la tabla de asignacion de archivos con JTable.
 * Columnas: Color, Nombre, Bloques, Primer bloque, Tamano, Solo lectura.
 */
public class FileTablePanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable table;

    // Mismos colores que DiskPanel para consistencia visual
    public static final Color[] FILE_COLORS = {
        new Color(127, 119, 221),
        new Color(29,  158, 117),
        new Color(239, 159, 39),
        new Color(212, 83,  126),
        new Color(55,  138, 221),
        new Color(216, 90,  48),
        new Color(99,  153, 34),
        new Color(226, 75,  74),
    };

    private static final String[] COLUMNS = {
        "Color", "Nombre", "Bloques", "1er Bloque",
        "Tamano (B)", "Solo lectura"
    };

    public FileTablePanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(
            "Tabla de asignacion de archivos"
        ));

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 0 ? Color.class : String.class;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(24);

        // Columna de color: mostrar rectangulo de color
        table.getColumnModel().getColumn(0)
            .setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(
                        JTable t, Object value, boolean isSelected,
                        boolean hasFocus, int row, int col) {
                    JPanel colorCell = new JPanel();
                    if (value instanceof Color c) {
                        colorCell.setBackground(c);
                        colorCell.setOpaque(true);
                    }
                    return colorCell;
                }
            });

        // Ancho fijo para la columna de color
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(0).setMinWidth(40);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /**
     * Reconstruye la tabla con todos los archivos del sistema.
     * @param root Directorio raiz del sistema de archivos.
     */
    public void refresh(DirectoryEntry root) {
        tableModel.setRowCount(0);
        collectFiles(root, new int[]{0});
    }

    private void collectFiles(DirectoryEntry dir,
                              int[] colorIndexRef) {
        Node<FileEntry> currentFile = dir.getFiles().getHead();
        while (currentFile != null) {
            FileEntry f = currentFile.data;
            Color color = FILE_COLORS[
                colorIndexRef[0] % FILE_COLORS.length
            ];
            colorIndexRef[0]++;
            tableModel.addRow(new Object[]{
                color,
                f.getName(),
                f.getBlockCount(),
                f.getFirstBlock(),
                f.getSize(),
                f.isReadOnly() ? "Si" : "No"
            });
            currentFile = currentFile.next;
        }

        Node<DirectoryEntry> currentDir =
            dir.getSubDirectories().getHead();
        while (currentDir != null) {
            collectFiles(currentDir.data, colorIndexRef);
            currentDir = currentDir.next;
        }
    }

    public JTable getTable() { return table; }
}