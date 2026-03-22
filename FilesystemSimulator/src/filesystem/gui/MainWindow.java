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

/**
 * Ventana principal del simulador de sistema de archivos.
 * Ensambla todos los paneles y gestiona la interaccion del usuario.
 */
public class MainWindow extends JFrame {

    // ─── COMPONENTES DEL SISTEMA ────────────────────────────────────────────
    private final FileSystem fileSystem;
    private final JournalManager journalManager;
    private DiskScheduler scheduler;

    // ─── PANELES ────────────────────────────────────────────────────────────
    private final FileTreePanel fileTreePanel;
    private final DiskPanel diskPanel;
    private final FileTablePanel fileTablePanel;
    private final LogPanel logPanel;
    private final JournalPanel journalPanel;
    private final ProcessQueuePanel processQueuePanel;

    // ─── BARRA DE ESTADO ────────────────────────────────────────────────────
    private final JLabel statusLabel;
    private final JLabel modeLabel;

    public MainWindow() {
        // Inicializar sistema
        fileSystem     = new FileSystem();
        journalManager = new JournalManager(fileSystem);
        scheduler      = new DiskScheduler(53, SchedulerPolicy.FIFO);

        // Configurar ventana
        setTitle("FileSystem Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Inicializar paneles
        fileTreePanel     = new FileTreePanel();
        diskPanel         = new DiskPanel();
        fileTablePanel    = new FileTablePanel();
        logPanel          = new LogPanel();
        journalPanel      = new JournalPanel();
        processQueuePanel = new ProcessQueuePanel();
        statusLabel       = new JLabel("Listo");
        modeLabel         = new JLabel("Modo: ADMINISTRADOR");

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

        JButton btnNewFile  = new JButton("Nuevo archivo");
        JButton btnNewDir   = new JButton("Nueva carpeta");
        JButton btnDelete   = new JButton("Eliminar");
        JButton btnRename   = new JButton("Renombrar");
        JButton btnCrash    = new JButton("Simular fallo");
        JButton btnRecover  = new JButton("Recuperar");
        JButton btnMode     = new JButton("Cambiar modo");
        JButton btnAddIO    = new JButton("Agregar E/S");
        JButton btnProcess  = new JButton("Procesar siguiente");

        btnCrash.setForeground(new Color(226, 75, 74));
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
        JTextField nameField    = new JTextField(15);
        JTextField contentField = new JTextField(15);
        JCheckBox readOnlyBox   = new JCheckBox("Solo lectura");

        JPanel panel = new JPanel(new GridLayout(3, 2, 6, 6));
        panel.add(new JLabel("Nombre:"));
        panel.add(nameField);
        panel.add(new JLabel("Contenido:"));
        panel.add(contentField);
        panel.add(new JLabel(""));
        panel.add(readOnlyBox);

        int result = JOptionPane.showConfirmDialog(
            this, panel, "Crear archivo",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                logPanel.logError("El nombre no puede estar vacio.");
                return;
            }
            try {
                journalManager.createFile(
                    name,
                    contentField.getText(),
                    fileSystem.getRoot(),
                    readOnlyBox.isSelected()
                );
                logPanel.logOK("Archivo creado: " + name);
                refreshAll();
            } catch (Exception ex) {
                logPanel.logError(ex.getMessage());
            }
        }
    }

    private void showCreateDirDialog() {
        String name = JOptionPane.showInputDialog(
            this, "Nombre del directorio:", "Nueva carpeta",
            JOptionPane.PLAIN_MESSAGE
        );
        if (name != null && !name.trim().isEmpty()) {
            try {
                fileSystem.createDirectory(
                    name.trim(), fileSystem.getRoot()
                );
                logPanel.logOK("Directorio creado: " + name.trim());
                refreshAll();
            } catch (Exception ex) {
                logPanel.logError(ex.getMessage());
            }
        }
    }

    private void showDeleteDialog() {
        String name = JOptionPane.showInputDialog(
            this, "Nombre del archivo a eliminar:",
            "Eliminar archivo", JOptionPane.PLAIN_MESSAGE
        );
        if (name != null && !name.trim().isEmpty()) {
            try {
                journalManager.deleteFile(
                    name.trim(), fileSystem.getRoot()
                );
                logPanel.logOK("Archivo eliminado: " + name.trim());
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

        if (result == JOptionPane.OK_OPTION) {
            try {
                journalManager.renameFile(
                    oldNameField.getText().trim(),
                    newNameField.getText().trim(),
                    fileSystem.getRoot()
                );
                logPanel.logOK("Archivo renombrado: "
                    + oldNameField.getText().trim()
                    + " --> " + newNameField.getText().trim());
                refreshAll();
            } catch (Exception ex) {
                logPanel.logError(ex.getMessage());
            }
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
        JTextField processField   = new JTextField("Proceso-1", 10);
        JTextField cylinderField  = new JTextField("50", 10);
        String[] ops = {"READ", "WRITE"};
        JComboBox<String> opCombo = new JComboBox<>(ops);

        JPanel panel = new JPanel(new GridLayout(3, 2, 6, 6));
        panel.add(new JLabel("Proceso:"));
        panel.add(processField);
        panel.add(new JLabel("Cilindro (0-199):"));
        panel.add(cylinderField);
        panel.add(new JLabel("Operacion:"));
        panel.add(opCombo);

        int result = JOptionPane.showConfirmDialog(
            this, panel, "Agregar solicitud E/S",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            try {
                int cyl = Integer.parseInt(
                    cylinderField.getText().trim()
                );
                IORequest.OperationType op =
                    opCombo.getSelectedIndex() == 0
                    ? IORequest.OperationType.READ
                    : IORequest.OperationType.WRITE;

                scheduler.addRequest(new IORequest(
                    processField.getText().trim(), cyl, op
                ));
                logPanel.logInfo("Solicitud E/S agregada: cilindro "
                    + cyl);
                processQueuePanel.refresh(scheduler);
            } catch (NumberFormatException ex) {
                logPanel.logError(
                    "El cilindro debe ser un numero entre 0 y 199."
                );
            } catch (Exception ex) {
                logPanel.logError(ex.getMessage());
            }
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

    // ─── REFRESCO GENERAL ───────────────────────────────────────────────────

    /**
     * Actualiza todos los paneles con el estado actual del sistema.
     * Llamar despues de cualquier operacion que modifique el estado.
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