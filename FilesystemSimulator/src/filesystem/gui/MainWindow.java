package filesystem.gui;

import filesystem.gui.panels.*;
import filesystem.journal.JournalManager;
import filesystem.model.DirectoryEntry;
import filesystem.model.FileSystem;
import filesystem.model.UserMode;
import filesystem.scheduler.DiskScheduler;
import filesystem.scheduler.IORequest;
import filesystem.scheduler.SchedulerPolicy;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import filesystem.persistence.JsonSaver;
import filesystem.persistence.JsonLoader;
import filesystem.persistence.TestCaseLoader;
import filesystem.scheduler.DiskSchedulerThread;
import java.util.concurrent.Semaphore;

/**
 * Ventana principal del simulador de sistema de archivos. Ensambla todos los
 * paneles y gestiona la interaccion del usuario.
 */
public class MainWindow extends JFrame {

    // ─── COMPONENTES DEL SISTEMA ────────────────────────────────────────────
    private final FileSystem fileSystem;
    private final JournalManager journalManager;
    private DiskScheduler scheduler;
    private DiskSchedulerThread schedulerThread;
    private JSpinner speedSpinner;

    // ─── PANELES ────────────────────────────────────────────────────────────
    private final FileTreePanel fileTreePanel;
    private final DiskPanel diskPanel;
    private final FileTablePanel fileTablePanel;
    private final LogPanel logPanel;
    private final JournalPanel journalPanel;
    private final ProcessQueuePanel processQueuePanel;

    // ─── COMPONENTES DE TOOLBAR ─────────────────────────────────────────────
    private JComboBox<String> policyCombo;
    private JComboBox<String> directionCombo;
    private JLabel directionLabel;
    private JSpinner headPositionSpinner;

    // ─── BARRA DE ESTADO ────────────────────────────────────────────────────
    private final JLabel statusLabel;
    private final JLabel modeLabel;

    public MainWindow() {
        // Inicializar sistema
        fileSystem = new FileSystem();
        journalManager = new JournalManager(fileSystem);
        scheduler = new DiskScheduler(53, SchedulerPolicy.FIFO);

        // Configurar ventana
        setTitle("FileSystem Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Inicializar paneles
        fileTreePanel = new FileTreePanel();
        diskPanel = new DiskPanel();
        fileTablePanel = new FileTablePanel();
        logPanel = new LogPanel();
        journalPanel = new JournalPanel();
        processQueuePanel = new ProcessQueuePanel();
        statusLabel = new JLabel("Listo");
        modeLabel = new JLabel("Modo: ADMINISTRADOR");

        buildUI();
        refreshAll();

        logPanel.logInfo("Sistema de archivos iniciado correctamente.");
    }

    // ─── CONSTRUCCION DE LA UI ──────────────────────────────────────────────
    private void buildUI() {
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JToolBar buildToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        directionLabel = new JLabel("Direccion: ");
        directionCombo = new JComboBox<>(new String[]{"UP", "DOWN"});
        directionCombo.setMaximumSize(new Dimension(80, 28));
        directionCombo.addActionListener(e -> applyDirection());
        directionLabel.setVisible(false);
        directionCombo.setVisible(false);
        JButton btnNewFile = new JButton("Nuevo archivo");
        JButton btnNewDir = new JButton("Nueva carpeta");
        JButton btnDelete = new JButton("Eliminar");
        JButton btnDeleteDir = new JButton("Eliminar carpeta");
        btnDeleteDir.addActionListener(e -> showDeleteDirDialog());
        toolbar.add(btnDeleteDir);
        JButton btnRename = new JButton("Renombrar");
        JButton btnCrash = new JButton("Simular fallo");
        JButton btnRecover = new JButton("Recuperar");
        JButton btnMode = new JButton("Cambiar modo");
        JButton btnAddIO = new JButton("Agregar E/S");
        JButton btnProcess = new JButton("Procesar siguiente");
        JButton btnAutoProcess = new JButton("Procesar todo (auto)");
        JButton btnStopProcess = new JButton("Detener");
        JLabel speedLabel = new JLabel("Velocidad (ms): ");
        speedSpinner = new JSpinner(
                new SpinnerNumberModel(800, 100, 3000, 100)
        );
        speedSpinner.setMaximumSize(new Dimension(75, 28));

        btnStopProcess.setForeground(new Color(226, 75, 74));
        btnStopProcess.setEnabled(false);

        btnAutoProcess.addActionListener(e
                -> startAutoProcess(btnAutoProcess, btnStopProcess)
        );
        btnStopProcess.addActionListener(e
                -> stopAutoProcess(btnAutoProcess, btnStopProcess)
        );
        JButton btnSave = new JButton("Guardar estado");
        JButton btnLoad = new JButton("Cargar estado");
        JButton btnLoadTest = new JButton("Cargar caso de prueba");
        btnLoadTest.setForeground(new Color(127, 119, 221));
        btnLoadTest.addActionListener(e -> loadTestCase());
        toolbar.addSeparator();
        toolbar.add(btnLoadTest);
        btnCrash.setForeground(new Color(226, 75, 74));
        btnSave.setForeground(new Color(39, 158, 117));
        btnLoad.setForeground(new Color(55, 138, 221));
        btnCrash.setFont(btnCrash.getFont().deriveFont(Font.BOLD));

        btnNewFile.addActionListener(e -> showCreateFileDialog());
        btnNewDir.addActionListener(e -> showCreateDirDialog());
        btnDelete.addActionListener(e -> showDeleteDialog());
        btnRename.addActionListener(e -> showRenameDialog());
        btnCrash.addActionListener(e -> simulateCrash());
        btnRecover.addActionListener(e -> recover());
        btnMode.addActionListener(e -> toggleMode());
        btnAddIO.addActionListener(e -> showAddIODialog());
        btnProcess.addActionListener(e -> processNextIO());
        btnSave.addActionListener(e -> saveState());
        btnLoad.addActionListener(e -> loadState());

        toolbar.add(btnNewFile);
        toolbar.add(btnNewDir);
        toolbar.addSeparator();
        toolbar.add(btnDelete);
        toolbar.add(btnRename);
        toolbar.addSeparator();
        toolbar.add(btnCrash);
        toolbar.add(btnRecover);
        toolbar.addSeparator();
        toolbar.add(btnMode);
        toolbar.addSeparator();
        toolbar.add(btnAddIO);
        toolbar.add(btnProcess);
        toolbar.addSeparator();
        toolbar.add(btnSave);
        toolbar.add(btnLoad);
        toolbar.add(directionLabel);
        toolbar.add(directionCombo);
        toolbar.add(btnAutoProcess);
        toolbar.add(btnStopProcess);
        toolbar.add(speedLabel);
        toolbar.add(speedSpinner);

        JLabel headLabel = new JLabel("Cabezal inicial: ");
        headPositionSpinner = new JSpinner(
                new SpinnerNumberModel(53, 0, 199, 1)
        );
        headPositionSpinner.setMaximumSize(new Dimension(65, 28));

        JButton btnApplyHead = new JButton("Aplicar");
        btnApplyHead.addActionListener(e -> applyHeadPosition());

        toolbar.addSeparator();
        toolbar.add(headLabel);
        toolbar.add(headPositionSpinner);
        toolbar.add(btnApplyHead);

        policyCombo = new JComboBox<>(
                new String[]{"FIFO", "SSTF", "SCAN", "C-SCAN"}
        );
        policyCombo.setMaximumSize(new Dimension(100, 28));
        policyCombo.addActionListener(e -> applySelectedPolicy());
        toolbar.addSeparator();
        toolbar.add(new JLabel("Politica: "));
        toolbar.add(policyCombo);

        return toolbar;
    }

    private JPanel buildCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(4, 4));
        center.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // Panel izquierdo: arbol de directorios
        fileTreePanel.setPreferredSize(new Dimension(200, 0));

        // Panel central: disco + tabla + cola
        JPanel middlePanel = new JPanel(new GridLayout(3, 1, 4, 4));
        middlePanel.add(diskPanel);
        middlePanel.add(fileTablePanel);
        middlePanel.add(processQueuePanel);

        // Panel derecho: log + journal
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 4, 4));
        rightPanel.setPreferredSize(new Dimension(280, 0));
        rightPanel.add(logPanel);
        rightPanel.add(journalPanel);

        center.add(fileTreePanel, BorderLayout.WEST);
        center.add(middlePanel, BorderLayout.CENTER);
        center.add(rightPanel, BorderLayout.EAST);

        return center;
    }

    private JPanel buildStatusBar() {
        JPanel statusBar = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 10, 2)
        );
        statusBar.setBorder(
                BorderFactory.createEtchedBorder()
        );
        modeLabel.setFont(
                modeLabel.getFont().deriveFont(Font.BOLD)
        );
        statusBar.add(modeLabel);
        statusBar.add(new JSeparator(SwingConstants.VERTICAL));
        statusBar.add(statusLabel);
        return statusBar;
    }

    // ─── ACCIONES DE LA TOOLBAR ─────────────────────────────────────────────
    private void showCreateFileDialog() {
        JTextField nameField = new JTextField(15);
        JTextField contentField = new JTextField(15);
        JSpinner blocksSpinner = new JSpinner(
                new SpinnerNumberModel(1, 1, 64, 1)
        );
        JCheckBox readOnlyBox = new JCheckBox("Solo lectura");
        JCheckBox autoBlocksBox = new JCheckBox(
                "Calcular bloques automaticamente", true
        );

        // Cuando se marca automatico, deshabilitar el spinner
        autoBlocksBox.addActionListener(e
                -> blocksSpinner.setEnabled(!autoBlocksBox.isSelected())
        );
        blocksSpinner.setEnabled(false);

        JPanel panel = new JPanel(new GridLayout(5, 2, 6, 6));
        panel.add(new JLabel("Nombre:"));
        panel.add(nameField);
        panel.add(new JLabel("Contenido:"));
        panel.add(contentField);
        panel.add(new JLabel("Bloques a asignar:"));
        panel.add(blocksSpinner);
        panel.add(new JLabel(""));
        panel.add(autoBlocksBox);
        panel.add(new JLabel(""));
        panel.add(readOnlyBox);

        // Mostrar ruta destino
        DirectoryEntry previewDir = getSelectedDirectory();
        JLabel pathLabel = new JLabel(
                "Destino: " + previewDir.getFullPath()
        );
        pathLabel.setForeground(new Color(39, 158, 117));
        pathLabel.setFont(
                pathLabel.getFont().deriveFont(Font.BOLD, 11f)
        );

        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.add(panel, BorderLayout.CENTER);
        wrapper.add(pathLabel, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(
                this, wrapper, "Crear archivo",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();

            // Validar nombre vacio
            if (name.isEmpty()) {
                logPanel.logError(
                        "El nombre del archivo no puede estar vacio."
                );
                return;
            }

            // Validar nombre duplicado
            DirectoryEntry targetDir = getSelectedDirectory();
            if (targetDir.findFile(name) != null) {
                logPanel.logError(
                        "Ya existe un archivo llamado '"
                        + name + "' en este directorio."
                );
                return;
            }

            try {
                String content = contentField.getText();

                if (!autoBlocksBox.isSelected()) {
                    // Modo manual: rellenar contenido hasta
                    // cubrir exactamente los bloques pedidos
                    int requestedBlocks
                            = (int) blocksSpinner.getValue();
                    int targetSize
                            = requestedBlocks * FileSystem.BLOCK_SIZE;

                    // Si el contenido es menor que el espacio
                    // pedido, rellenar con espacios
                    if (content.length() < targetSize) {
                        StringBuilder sb
                                = new StringBuilder(content);
                        while (sb.length() < targetSize) {
                            sb.append(' ');
                        }
                        content = sb.toString();
                    }
                }

                journalManager.createFile(
                        name,
                        content,
                        targetDir,
                        readOnlyBox.isSelected()
                );
                logPanel.logOK(
                        "Archivo creado: " + name
                        + " en " + targetDir.getFullPath()
                );
                refreshAll();

            } catch (Exception ex) {
                logPanel.logError(ex.getMessage());
            }
        }
    }

    private void showCreateDirDialog() {
        String name = JOptionPane.showInputDialog(
                this, "Nombre del directorio:",
                "Nueva carpeta", JOptionPane.PLAIN_MESSAGE
        );

        // Canceló el dialogo
        if (name == null) {
            return;
        }

        // Nombre vacio
        if (name.trim().isEmpty()) {
            logPanel.logError(
                    "El nombre del directorio no puede estar vacio."
            );
            return;
        }

        // Nombre con caracteres invalidos
        if (name.trim().contains("/")
                || name.trim().contains("\\")
                || name.trim().contains(":")) {
            logPanel.logError(
                    "El nombre no puede contener los caracteres: / \\ :"
            );
            return;
        }

        // Directorio duplicado
        DirectoryEntry targetDir = getSelectedDirectory();
        if (targetDir.findSubDirectory(name.trim()) != null) {
            logPanel.logError(
                    "Ya existe una carpeta llamada '"
                    + name.trim() + "' en este directorio."
            );
            return;
        }

        try {
            fileSystem.createDirectory(name.trim(), targetDir);
            logPanel.logOK(
                    "Directorio creado: " + name.trim()
                    + " en " + targetDir.getFullPath()
            );
            refreshAll();
        } catch (Exception ex) {
            logPanel.logError(ex.getMessage());
        }
    }

    private void showDeleteDialog() {
        String name = JOptionPane.showInputDialog(
                this, "Nombre del archivo a eliminar:",
                "Eliminar archivo", JOptionPane.PLAIN_MESSAGE
        );

        if (name == null) {
            return;
        }

        if (name.trim().isEmpty()) {
            logPanel.logError(
                    "El nombre del archivo no puede estar vacio."
            );
            return;
        }

        // Verificar que el archivo existe antes de confirmar
        DirectoryEntry targetDir = getSelectedDirectory();
        if (targetDir.findFile(name.trim()) == null) {
            logPanel.logError(
                    "No existe un archivo llamado '"
                    + name.trim() + "' en este directorio."
            );
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Seguro que deseas eliminar '" + name.trim() + "'?",
                "Confirmar eliminacion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                journalManager.deleteFile(
                        name.trim(), targetDir
                );
                logPanel.logOK(
                        "Archivo eliminado: " + name.trim()
                );
                refreshAll();
            } catch (Exception ex) {
                logPanel.logError(ex.getMessage());
            }
        }
    }

    private void showRenameDialog() {
        JTextField oldNameField = new JTextField(15);
        JTextField newNameField = new JTextField(15);

        JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
        panel.add(new JLabel("Nombre actual:"));
        panel.add(oldNameField);
        panel.add(new JLabel("Nuevo nombre:"));
        panel.add(newNameField);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Renombrar archivo",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String oldName = oldNameField.getText().trim();
        String newName = newNameField.getText().trim();

        // Validar campos vacios
        if (oldName.isEmpty() || newName.isEmpty()) {
            logPanel.logError(
                    "Ambos campos deben estar completos."
            );
            return;
        }

        // Validar que no sean el mismo nombre
        if (oldName.equals(newName)) {
            logPanel.logError(
                    "El nombre nuevo debe ser diferente al actual."
            );
            return;
        }

        // Validar caracteres invalidos
        if (newName.contains("/")
                || newName.contains("\\")
                || newName.contains(":")) {
            logPanel.logError(
                    "El nombre no puede contener los caracteres: / \\ :"
            );
            return;
        }

        // Verificar que el archivo original existe
        DirectoryEntry targetDir = getSelectedDirectory();
        if (targetDir.findFile(oldName) == null) {
            logPanel.logError(
                    "No existe un archivo llamado '" + oldName + "'."
            );
            return;
        }

        // Verificar que el nuevo nombre no esté en uso
        if (targetDir.findFile(newName) != null) {
            logPanel.logError(
                    "Ya existe un archivo llamado '" + newName + "'."
            );
            return;
        }

        try {
            journalManager.renameFile(
                    oldName, newName, targetDir
            );
            logPanel.logOK(
                    "Archivo renombrado: " + oldName
                    + " --> " + newName
            );
            refreshAll();
        } catch (Exception ex) {
            logPanel.logError(ex.getMessage());
        }
    }

    private void simulateCrash() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Simular un fallo del sistema?\n"
                + "Las proximas operaciones quedaran como PENDING.",
                "Simular fallo",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            journalManager.simulateCrash();
            logPanel.logWarn("Fallo simulado activado.");
            refreshAll();
        }
    }

    private void recover() {
        int revertidas = journalManager.recover();
        logPanel.logOK("Recuperacion completada. "
                + revertidas + " operacion(es) revertida(s).");
        refreshAll();
    }

    private void toggleMode() {
        if (fileSystem.getCurrentMode() == UserMode.ADMINISTRATOR) {
            fileSystem.setCurrentMode(UserMode.USER);
            modeLabel.setText("Modo: USUARIO");
            modeLabel.setForeground(new Color(226, 75, 74));
            logPanel.logInfo("Modo cambiado a USUARIO.");
        } else {
            fileSystem.setCurrentMode(UserMode.ADMINISTRATOR);
            modeLabel.setText("Modo: ADMINISTRADOR");
            modeLabel.setForeground(new Color(39, 158, 117));
            logPanel.logInfo("Modo cambiado a ADMINISTRADOR.");
        }
    }

    private void showAddIODialog() {
        JTextField processField = new JTextField("Proceso-1", 10);
        JSpinner cylinderSpinner = new JSpinner(
                new SpinnerNumberModel(50, 0, 199, 1)
        );
        String[] ops = {"READ", "WRITE"};
        JComboBox<String> opCombo = new JComboBox<>(ops);

        JPanel panel = new JPanel(new GridLayout(3, 2, 6, 6));
        panel.add(new JLabel("Proceso:"));
        panel.add(processField);
        panel.add(new JLabel("Cilindro (0-199):"));
        panel.add(cylinderSpinner);
        panel.add(new JLabel("Operacion:"));
        panel.add(opCombo);

        int result = JOptionPane.showConfirmDialog(
                this, panel, "Agregar solicitud E/S",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        // Validar nombre del proceso
        String processName = processField.getText().trim();
        if (processName.isEmpty()) {
            logPanel.logError(
                    "El nombre del proceso no puede estar vacio."
            );
            return;
        }

        try {
            int cyl = (int) cylinderSpinner.getValue();
            IORequest.OperationType op
                    = opCombo.getSelectedIndex() == 0
                    ? IORequest.OperationType.READ
                    : IORequest.OperationType.WRITE;

            scheduler.addRequest(
                    new IORequest(processName, cyl, op)
            );
            logPanel.logInfo(
                    "Solicitud E/S agregada: " + processName
                    + " | cilindro " + cyl
                    + " | " + op
            );
            processQueuePanel.refresh(scheduler);

        } catch (Exception ex) {
            logPanel.logError(ex.getMessage());
        }
    }

    private void processNextIO() {
        if (scheduler.isQueueEmpty()) {
            logPanel.logWarn("No hay solicitudes pendientes en la cola.");
            return;
        }
        try {
            IORequest attended = scheduler.processNext();
            logPanel.logOK("Atendido: cilindro "
                    + attended.getCylinderPosition()
                    + " (" + attended.getOperationType() + ")"
                    + " | Cabezal en: " + scheduler.getHeadPosition()
                    + " | Movimiento total: "
                    + scheduler.getTotalHeadMovement());
            processQueuePanel.refresh(scheduler);
        } catch (Exception ex) {
            logPanel.logError(ex.getMessage());
        }
    }

    /**
     * Abre un dialogo para guardar el estado en un archivo JSON.
     */
    private void saveState() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar estado del sistema");
        chooser.setSelectedFile(
                new java.io.File("filesystem_state.json")
        );
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                JsonSaver.save(
                        fileSystem,
                        journalManager,
                        chooser.getSelectedFile().getAbsolutePath()
                );
                logPanel.logOK("Estado guardado correctamente.");
            } catch (Exception ex) {
                logPanel.logError("Error al guardar: " + ex.getMessage());
            }
        }
    }

    /**
     * Abre un dialogo para cargar el estado desde un archivo JSON.
     */
    private void loadState() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Cargar estado del sistema");
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                JsonLoader.load(
                        fileSystem,
                        journalManager,
                        chooser.getSelectedFile().getAbsolutePath()
                );
                logPanel.logOK("Estado cargado correctamente.");
                refreshAll();
            } catch (Exception ex) {
                logPanel.logError("Error al cargar: " + ex.getMessage());
            }
        }
    }

    /**
     * Retorna el directorio actualmente seleccionado en el JTree. Si no hay
     * seleccion, retorna el directorio raiz.
     *
     * @return El DirectoryEntry seleccionado o la raiz.
     */
    private DirectoryEntry getSelectedDirectory() {
        String[] path = fileTreePanel.getSelectedPath();

        // Sin seleccion o raiz seleccionada: usar root
        if (path == null || path.length == 0) {
            return fileSystem.getRoot();
        }

        // Navegar por el arbol de directorios siguiendo la ruta
        DirectoryEntry current = fileSystem.getRoot();
        for (String segment : path) {
            DirectoryEntry next = current.findSubDirectory(segment);
            if (next == null) {
                return fileSystem.getRoot();
            }
            current = next;
        }
        return current;
    }

    /**
     * Muestra dialogo para eliminar un directorio y todo su contenido.
     */
    private void showDeleteDirDialog() {
        String name = JOptionPane.showInputDialog(
                this,
                "Nombre de la carpeta a eliminar:\n"
                + "(Se eliminara todo su contenido recursivamente)",
                "Eliminar carpeta",
                JOptionPane.WARNING_MESSAGE
        );
        if (name != null && !name.trim().isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Seguro que deseas eliminar la carpeta '"
                    + name.trim() + "' y todo su contenido?",
                    "Confirmar eliminacion",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    DirectoryEntry parent = getSelectedDirectory();
                    // Si el directorio a eliminar está seleccionado,
                    // subir al padre
                    if (parent.getName().equals(name.trim())
                            && parent.getParent() != null) {
                        parent = parent.getParent();
                    }
                    fileSystem.deleteDirectory(name.trim(), parent);
                    logPanel.logOK(
                            "Carpeta eliminada: " + name.trim()
                    );
                    refreshAll();
                } catch (Exception ex) {
                    logPanel.logError(ex.getMessage());
                }
            }
        }
    }

    /**
     * Aplica la posicion inicial del cabezal ingresada por el usuario. Usa el
     * metodo reset del planificador que conserva la politica pero reinicia el
     * movimiento y el historial.
     */
    private void applyHeadPosition() {
        int newHead = (int) headPositionSpinner.getValue();
        try {
            scheduler.reset(newHead);
            // Reaplicar la politica seleccionada en el combo
            applySelectedPolicy();
            logPanel.logInfo(
                    "Cabezal inicial configurado en: " + newHead
            );
            processQueuePanel.refresh(scheduler);
        } catch (IllegalArgumentException ex) {
            logPanel.logError(ex.getMessage());
        }
    }

    private void applySelectedPolicy() {
        if (policyCombo == null) {
            return;
        }
        String selected = (String) policyCombo.getSelectedItem();
        filesystem.scheduler.SchedulerPolicy policy
                = switch (selected) {
            case "SSTF" ->
                filesystem.scheduler.SchedulerPolicy.SSTF;
            case "SCAN" ->
                filesystem.scheduler.SchedulerPolicy.SCAN;
            case "C-SCAN" ->
                filesystem.scheduler.SchedulerPolicy.C_SCAN;
            default ->
                filesystem.scheduler.SchedulerPolicy.FIFO;
        };
        scheduler.setPolicy(policy);
        logPanel.logInfo("Politica cambiada a: " + selected);

        // Mostrar selector de direccion solo para SCAN y C-SCAN
        boolean needsDirection = selected.equals("SCAN")
                || selected.equals("C-SCAN");
        directionLabel.setVisible(needsDirection);
        directionCombo.setVisible(needsDirection);

        // Aplicar direccion actual si aplica
        if (needsDirection) {
            applyDirection();
        }

        processQueuePanel.refresh(scheduler);
    }

    /**
     * Aplica la direccion seleccionada al planificador. Solo relevante para
     * politicas SCAN y C-SCAN. UP = hacia cilindros mayores, DOWN = hacia
     * menores.
     */
    private void applyDirection() {
        if (directionCombo == null) {
            return;
        }
        boolean goingUp = directionCombo.getSelectedItem()
                .equals("UP");
        scheduler.setMovingUp(goingUp);
        logPanel.logInfo("Direccion del cabezal: "
                + (goingUp ? "UP (hacia cilindros mayores)"
                        : "DOWN (hacia cilindros menores)"));
    }

    /**
     * Carga un caso de prueba desde el formato JSON del PDF. Configura el disco
     * con los archivos del sistema y el planificador con las solicitudes del
     * caso.
     */
    private void loadTestCase() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(
                "Cargar caso de prueba (formato PDF)"
        );
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                TestCaseLoader.TestCaseResult testResult
                        = TestCaseLoader.load(
                                fileSystem,
                                chooser.getSelectedFile().getAbsolutePath()
                        );

                // Aplicar el planificador cargado
                scheduler = testResult.scheduler;
                headPositionSpinner.setValue(testResult.initialHead);

                logPanel.logOK(
                        "Caso de prueba cargado: "
                        + testResult.testId
                        + " | Cabezal inicial: "
                        + testResult.initialHead
                );
                policyCombo.setSelectedItem("FIFO");
                refreshAll();
            } catch (Exception ex) {
                logPanel.logError(
                        "Error al cargar caso de prueba: "
                        + ex.getMessage()
                );
            }
        }
    }

    /**
     * Inicia el procesamiento automatico de todas las solicitudes usando el
     * hilo del planificador en segundo plano. La GUI sigue siendo interactiva
     * durante el procesamiento.
     */
    private void startAutoProcess(JButton btnStart,
            JButton btnStop) {
        if (scheduler.isQueueEmpty()) {
            logPanel.logWarn(
                    "No hay solicitudes en la cola para procesar."
            );
            return;
        }

        // Verificar que no haya un hilo ya corriendo
        if (schedulerThread != null
                && schedulerThread.isRunning()) {
            logPanel.logWarn(
                    "El planificador ya esta en ejecucion."
            );
            return;
        }

        int delayMs = (int) speedSpinner.getValue();

        schedulerThread = new DiskSchedulerThread(
                scheduler,
                scheduler.getDiskSemaphore(),
                logPanel,
                processQueuePanel,
                delayMs
        );

        // Callback: actualizar toda la GUI tras cada paso
        schedulerThread.setOnStepCallback(this::refreshAll);

        // Rehabilitar botones cuando el hilo termine
        schedulerThread.setOnStepCallback(() -> {
            refreshAll();
            if (!schedulerThread.isRunning()) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    btnStart.setEnabled(true);
                    btnStop.setEnabled(false);
                    logPanel.logInfo(
                            "Procesamiento automatico completado."
                    );
                });
            }
        });

        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        logPanel.logInfo(
                "Iniciando procesamiento automatico con hilo dedicado..."
                + " Semaforo adquirido."
        );

        schedulerThread.start();
    }

    /**
     * Detiene el hilo del planificador limpiamente. Espera a que termine la
     * solicitud actual antes de parar.
     */
    private void stopAutoProcess(JButton btnStart,
            JButton btnStop) {
        if (schedulerThread != null
                && schedulerThread.isRunning()) {
            schedulerThread.stopGracefully();
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
            logPanel.logWarn(
                    "Procesamiento automatico detenido manualmente."
            );
        }
    }

    // ─── REFRESCO GENERAL ───────────────────────────────────────────────────
    /**
     * Actualiza todos los paneles con el estado actual del sistema. Llamar
     * despues de cualquier operacion que modifique el estado.
     */
    public void refreshAll() {
        fileTreePanel.refresh(fileSystem.getRoot());
        diskPanel.refresh(
                fileSystem.getDisk(),
                fileSystem.getBitmap().getSnapshot(),
                fileSystem.getRoot()
        );
        fileTablePanel.refresh(fileSystem.getRoot());
        journalPanel.refresh(journalManager);
        processQueuePanel.refresh(scheduler);
        statusLabel.setText(fileSystem.getDiskInfo()
                + " | Locks activos: "
                + " | Cola: " + scheduler.getPendingCount()
                + " solicitudes");
    }
}
