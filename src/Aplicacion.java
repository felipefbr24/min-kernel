import javax.swing.SwingUtilities;

public class Aplicacion {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            InterfazKernel ui = new InterfazKernel();
            ui.setVisible(true);
        });
    }
}
