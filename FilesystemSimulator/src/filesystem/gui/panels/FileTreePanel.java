package filesystem.gui.panels;

import filesystem.datastructures.Node;
import filesystem.model.DirectoryEntry;
import filesystem.model.FileEntry;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

/**
 * Panel que muestra el arbol de directorios usando JTree. Usa iconos nativos de
 * Swing para diferenciar carpetas y archivos.
 */
public class FileTreePanel extends JPanel {

    private final JTree tree;
    private final DefaultMutableTreeNode rootNode;
    private final DefaultTreeModel treeModel;

    public FileTreePanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Arbol de directorios"));

        rootNode = new DefaultMutableTreeNode(
                new TreeNodeData("Sistema de archivos", true)
        );
        treeModel = new DefaultTreeModel(rootNode);
        tree = new JTree(treeModel);

        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);

        // Renderer personalizado con iconos nativos
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(
                    JTree tree, Object value, boolean selected,
                    boolean expanded, boolean leaf,
                    int row, boolean hasFocus) {

                super.getTreeCellRendererComponent(
                        tree, value, selected,
                        expanded, leaf, row, hasFocus
                );

                if (value instanceof DefaultMutableTreeNode node) {
                    Object userObj = node.getUserObject();
                    if (userObj instanceof TreeNodeData data) {
                        setText(data.name
                                + (data.isReadOnly ? " [R]" : ""));
                        if (data.isDirectory) {
                            // Icono de carpeta nativo de Swing
                            setIcon(expanded
                                    ? getOpenIcon()
                                    : getClosedIcon()
                            );
                        } else {
                            // Icono de archivo nativo de Swing
                            setIcon(getLeafIcon());
                        }
                    }
                }
                return this;
            }
        });

        add(new JScrollPane(tree), BorderLayout.CENTER);
    }

    /**
     * Reconstruye el JTree a partir del estado actual del arbol de directorios
     * del sistema de archivos.
     *
     * @param root Directorio raiz del sistema.
     */
    public void refresh(DirectoryEntry root) {
        rootNode.removeAllChildren();
        rootNode.setUserObject(
                new TreeNodeData("Sistema de archivos", true)
        );
        buildTree(rootNode, root);
        treeModel.reload();

        // Expandir todos los nodos
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    /**
     * Construye recursivamente el JTree desde el arbol de directorios. Primero
     * agrega subdirectorios, luego archivos dentro de cada uno.
     */
    private void buildTree(DefaultMutableTreeNode treeNode,
            DirectoryEntry dir) {
        // Primero los subdirectorios
        Node<DirectoryEntry> currentDir
                = dir.getSubDirectories().getHead();
        while (currentDir != null) {
            DefaultMutableTreeNode dirNode
                    = new DefaultMutableTreeNode(
                            new TreeNodeData(
                                    currentDir.data.getName(), true
                            )
                    );
            treeNode.add(dirNode);
            // Recursion: archivos y subdirs dentro de esta carpeta
            buildTree(dirNode, currentDir.data);
            currentDir = currentDir.next;
        }

        // Luego los archivos del directorio actual
        Node<FileEntry> currentFile = dir.getFiles().getHead();
        while (currentFile != null) {
            DefaultMutableTreeNode fileNode
                    = new DefaultMutableTreeNode(
                            new TreeNodeData(
                                    currentFile.data.getName(),
                                    false,
                                    currentFile.data.isReadOnly()
                            )
                    );
            treeNode.add(fileNode);
            currentFile = currentFile.next;
        }
    }

    public JTree getTree() {
        return tree;
    }

    // ─── CLASE INTERNA PARA DATOS DEL NODO ─────────────────────────────────
    /**
     * Datos que se almacenan en cada nodo del JTree. Permite distinguir entre
     * directorios y archivos sin depender del texto del nodo.
     */
    public static class TreeNodeData {

        public final String name;
        public final boolean isDirectory;
        public final boolean isReadOnly;

        public TreeNodeData(String name, boolean isDirectory) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.isReadOnly = false;
        }

        public TreeNodeData(String name,
                boolean isDirectory,
                boolean isReadOnly) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.isReadOnly = isReadOnly;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Retorna la ruta del nodo actualmente seleccionado en el JTree. Si no hay
     * seleccion o es el nodo raiz, retorna null.
     *
     * @return Arreglo con los nombres de cada nivel de la ruta, o null si no
     * hay carpeta seleccionada.
     */
    public String[] getSelectedPath() {
        javax.swing.tree.TreePath selectedPath
                = tree.getSelectionPath();

        if (selectedPath == null) {
            return null;
        }

        // Construir la ruta saltando el nodo raiz
        // (que es "Sistema de archivos", no un dir real)
        Object[] pathComponents = selectedPath.getPath();

        // Si solo hay el nodo raiz seleccionado
        if (pathComponents.length <= 1) {
            return null;
        }

        // Extraer nombres desde el segundo nivel (saltamos raiz)
        String[] path = new String[pathComponents.length - 1];
        for (int i = 1; i < pathComponents.length; i++) {
            DefaultMutableTreeNode node
                    = (DefaultMutableTreeNode) pathComponents[i];
            TreeNodeData data = (TreeNodeData) node.getUserObject();
            // Solo incluir si es directorio
            if (!data.isDirectory) {
                // Si seleccionaron un archivo, subir un nivel
                path = new String[pathComponents.length - 2];
                for (int j = 1; j < pathComponents.length - 1; j++) {
                    DefaultMutableTreeNode parentNode
                            = (DefaultMutableTreeNode) pathComponents[j];
                    TreeNodeData parentData
                            = (TreeNodeData) parentNode.getUserObject();
                    path[j - 1] = parentData.name;
                }
                return path.length == 0 ? null : path;
            }
            path[i - 1] = data.name;
        }
        return path;
    }
}
