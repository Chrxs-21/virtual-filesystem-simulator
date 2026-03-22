package filesystem.gui.panels;

import filesystem.datastructures.Node;
import filesystem.model.DirectoryEntry;
import filesystem.model.DiskBlock;
import filesystem.model.FileEntry;
import filesystem.model.FileSystem;
import javax.swing.*;
import java.awt.*;

/**
 * Panel que visualiza el estado del disco bloque por bloque.
 * Cada bloque se colorea segun el archivo que lo ocupa.
 * Los bloques libres se muestran en gris claro.
 */
public class DiskPanel extends JPanel {

    private static final int BLOCK_SIZE_PX = 24;
    private static final int BLOCKS_PER_ROW = 16;
    private static final int PADDING = 8;

    private DiskBlock[] disk;
    private boolean[] bitmap;

    // Colores para los archivos (hasta 8 archivos distintos)
    private static final Color[] FILE_COLORS = {
        new Color(127, 119, 221),  // purple
        new Color(29,  158, 117),  // teal
        new Color(239, 159, 39),   // amber
        new Color(212, 83,  126),  // pink
        new Color(55,  138, 221),  // blue
        new Color(216, 90,  48),   // coral
        new Color(99,  153, 34),   // green
        new Color(226, 75,  74),   // red
    };

    private String[] fileNames;
    private int colorIndex;

    public DiskPanel() {
        setBorder(BorderFactory.createTitledBorder(
            "Visualizacion del disco"
        ));
        setPreferredSize(new Dimension(
            BLOCKS_PER_ROW * (BLOCK_SIZE_PX + 2) + PADDING * 2,
            (FileSystem.TOTAL_BLOCKS / BLOCKS_PER_ROW)
            * (BLOCK_SIZE_PX + 2) + PADDING * 2 + 40
        ));
        fileNames  = new String[0];
        colorIndex = 0;
    }

    /**
     * Actualiza el panel con el estado actual del disco.
     *
     * @param disk   Arreglo de bloques del disco.
     * @param bitmap Estado de bloques libres/ocupados.
     * @param root   Raiz del sistema para mapear colores por archivo.
     */
    public void refresh(DiskBlock[] disk,
                        boolean[] bitmap,
                        DirectoryEntry root) {
        this.disk   = disk;
        this.bitmap = bitmap;
        collectFileNames(root);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (disk == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        for (int i = 0; i < disk.length; i++) {
            int col = i % BLOCKS_PER_ROW;
            int row = i / BLOCKS_PER_ROW;
            int x   = PADDING + col * (BLOCK_SIZE_PX + 2);
            int y   = PADDING + 20 + row * (BLOCK_SIZE_PX + 2);

            Color blockColor = getBlockColor(disk[i]);
            g2.setColor(blockColor);
            g2.fillRoundRect(x, y, BLOCK_SIZE_PX, BLOCK_SIZE_PX, 4, 4);

            g2.setColor(blockColor.darker());
            g2.drawRoundRect(x, y, BLOCK_SIZE_PX, BLOCK_SIZE_PX, 4, 4);

            // Numero del bloque
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 7));
            g2.drawString(String.valueOf(i),
                x + 3, y + BLOCK_SIZE_PX - 4);
        }

        // Leyenda
        drawLegend(g2);
    }

    private void drawLegend(Graphics2D g2) {
        int x = PADDING;
        int y = PADDING + 4;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));

        // Libre
        g2.setColor(new Color(220, 220, 220));
        g2.fillRoundRect(x, y, 12, 12, 3, 3);
        g2.setColor(Color.GRAY);
        g2.drawString("libre", x + 16, y + 10);

        // Archivos
        for (int i = 0; i < fileNames.length && i < FILE_COLORS.length; i++) {
            x += 60;
            g2.setColor(FILE_COLORS[i]);
            g2.fillRoundRect(x, y, 12, 12, 3, 3);
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(fileNames[i], x + 16, y + 10);
        }
    }

    /**
     * Determina el color de un bloque segun el archivo propietario.
     */
    private Color getBlockColor(DiskBlock block) {
        if (block.isFree()) {
            return new Color(220, 220, 220);
        }
        String owner = block.getOwnerFile();
        for (int i = 0; i < fileNames.length; i++) {
            if (fileNames[i].equals(owner)) {
                return FILE_COLORS[i % FILE_COLORS.length];
            }
        }
        return Color.LIGHT_GRAY;
    }

    /**
     * Recolecta todos los nombres de archivos del sistema
     * para asignarles colores consistentes.
     */
    private void collectFileNames(DirectoryEntry dir) {
        filesystem.datastructures.LinkedList<String> names =
            new filesystem.datastructures.LinkedList<>();
        collectNamesRecursive(dir, names);

        fileNames = new String[names.size()];
        Node<String> current = names.getHead();
        int i = 0;
        while (current != null) {
            fileNames[i++] = current.data;
            current = current.next;
        }
    }

    private void collectNamesRecursive(
            DirectoryEntry dir,
            filesystem.datastructures.LinkedList<String> names) {
        Node<FileEntry> currentFile = dir.getFiles().getHead();
        while (currentFile != null) {
            names.addLast(currentFile.data.getName());
            currentFile = currentFile.next;
        }
        Node<DirectoryEntry> currentDir =
            dir.getSubDirectories().getHead();
        while (currentDir != null) {
            collectNamesRecursive(currentDir.data, names);
            currentDir = currentDir.next;
        }
    }
}