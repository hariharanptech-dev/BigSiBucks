import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main - the entry point. Java starts running the program from here.
 *
 * Swing windows must be created on the "Event Dispatch Thread" (EDT),
 * which is why we use SwingUtilities.invokeLater(...).
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // The cross-platform "Metal" look respects our custom colours on Windows, macOS and Linux.
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // If this fails, Swing simply uses its default look. Nothing else to do.
            }
            new PasswordStrengthGUI().setVisible(true);
        });
    }
}
