import javax.swing.SwingUtilities;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            KernelUI ui = new KernelUI();
            ui.setVisible(true);
        });
    }
}
