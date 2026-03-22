package filesystem.gui.panels;

import filesystem.datastructures.Node;
import filesystem.model.DirectoryEntry;
import filesystem.model.FileEntry;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Panel que muestra la tabla de asignacion de archivos con JTable.
 * Columnas: Nombre, Bloques, Primer bloque, Tamano, Solo lectura.
 */
public class FileTablePanel extends JPanel {

    private final DefaultTableModel tableModel;
    private final JTable table;

    private static final String[] COLUMNS = {
        "Nombre", "Bloques", "1er Bloque", "Tamano (B)", "Solo lectura"
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
        };

        table = new JTable(tableModel);
        table.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowHeight(22);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /**
     * Reconstruye la tabla con todos los archivos del sistema.
     * @param root Directorio raiz del sistema de archivos.
     */
    public void refresh(DirectoryEntry root) {
        tableModel.setRowCount(0);
        collectFiles(root);
    }

    private void collectFiles(DirectoryEntry dir) {
        Node<FileEntry> currentFile = dir.getFiles().getHead();
        while (currentFile != null) {
            FileEntry f = currentFile.data;
            tableModel.addRow(new Object[]{
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
            collectFiles(currentDir.data);
            currentDir = currentDir.next;
        }
    }

    public JTable getTable() { return table; }
}