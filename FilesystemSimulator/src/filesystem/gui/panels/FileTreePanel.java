package filesystem.gui.panels;

import filesystem.datastructures.Node;
import filesystem.model.DirectoryEntry;
import filesystem.model.FileEntry;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

/**
 * Panel que muestra el arbol de directorios usando JTree.
 * Se actualiza cada vez que cambia el sistema de archivos.
 */
public class FileTreePanel extends JPanel {

    private final JTree tree;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultTreeModel treeModel;

    public FileTreePanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Arbol de directorios"));

        rootNode  = new DefaultMutableTreeNode("root");
        treeModel = new DefaultTreeModel(rootNode);
        tree      = new JTree(treeModel);

        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);

        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    /**
     * Reconstruye el JTree a partir del estado actual
     * del arbol de directorios del sistema de archivos.
     *
     * @param root Directorio raiz del sistema.
     */
    public void refresh(DirectoryEntry root) {
        rootNode.removeAllChildren();
        buildTree(rootNode, root);
        treeModel.reload();

        // Expandir todos los nodos
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    /**
     * Construye recursivamente el JTree desde el arbol de directorios.
     */
    private void buildTree(DefaultMutableTreeNode treeNode,
                           DirectoryEntry dir) {
        // Agregar subdirectorios
        Node<DirectoryEntry> currentDir =
            dir.getSubDirectories().getHead();
        while (currentDir != null) {
            DefaultMutableTreeNode dirNode =
                new DefaultMutableTreeNode(
                    "[DIR] " + currentDir.data.getName()
                );
            treeNode.add(dirNode);
            buildTree(dirNode, currentDir.data);
            currentDir = currentDir.next;
        }

        // Agregar archivos
        Node<FileEntry> currentFile = dir.getFiles().getHead();
        while (currentFile != null) {
            DefaultMutableTreeNode fileNode =
                new DefaultMutableTreeNode(
                    currentFile.data.getName()
                    + (currentFile.data.isReadOnly() ? " [R]" : "")
                );
            treeNode.add(fileNode);
            currentFile = currentFile.next;
        }
    }

    public JTree getTree() { return tree; }
}