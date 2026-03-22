package filesystem.gui.panels;

import javax.swing.*;
import java.awt.*;

/**
 * Panel del log de eventos del sistema.
 * Muestra mensajes con colores segun su tipo:
 * verde = OK, rojo = ERROR, amarillo = WARN.
 */
public class LogPanel extends JPanel {

    private final JTextPane logArea;
    private final javax.swing.text.StyledDocument doc;

    public LogPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Log de eventos"));

        logArea = new JTextPane();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        doc = logArea.getStyledDocument();

        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JButton clearBtn = new JButton("Limpiar");
        clearBtn.addActionListener(e -> logArea.setText(""));
        add(clearBtn, BorderLayout.SOUTH);
    }

    /**
     * Agrega un mensaje de tipo OK (verde).
     */
    public void logOK(String message) {
        appendLog("[OK] " + message, new Color(39, 158, 117));
    }

    /**
     * Agrega un mensaje de tipo ERROR (rojo).
     */
    public void logError(String message) {
        appendLog("[ERROR] " + message, new Color(226, 75, 74));
    }

    /**
     * Agrega un mensaje de tipo WARNING (naranja).
     */
    public void logWarn(String message) {
        appendLog("[WARN] " + message, new Color(239, 159, 39));
    }

    /**
     * Agrega un mensaje de tipo INFO (gris).
     */
    public void logInfo(String message) {
        appendLog("[INFO] " + message, Color.GRAY);
    }

    private void appendLog(String message, Color color) {
        try {
            javax.swing.text.SimpleAttributeSet attrs =
                new javax.swing.text.SimpleAttributeSet();
            javax.swing.text.StyleConstants.setForeground(attrs, color);
            javax.swing.text.StyleConstants.setBold(attrs, true);
            doc.insertString(doc.getLength(),
                message + "\n", attrs);

            // Auto-scroll al final
            logArea.setCaretPosition(doc.getLength());
        } catch (javax.swing.text.BadLocationException e) {
            e.printStackTrace();
        }
    }
}