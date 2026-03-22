package filesystem;

import filesystem.gui.MainWindow;
import javax.swing.*;

/**
 * Punto de entrada principal del simulador.
 * Lanza la interfaz grafica en el Event Dispatch Thread de Swing.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }
}