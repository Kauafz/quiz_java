import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Toolkit;

/** Quiz de Segurança Digital: aplicativo 100% Java (Swing), sem bibliotecas externas. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            QuizApp app = new QuizApp(HistoryStore.defaultStore());
            JFrame frame = new JFrame("Quiz de Segurança Digital");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane((java.awt.Container) app.component());
            frame.setMinimumSize(new Dimension(320, 400));

            // Quando roda dentro de uma página web (CheerpJ), ocupa toda a área disponível
            boolean embedded = System.getProperty("quiz.embed") != null;
            if (embedded) {
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                frame.setUndecorated(true);
                frame.setBounds(0, 0, screen.width, screen.height);
            } else {
                frame.setSize(1000, 820);
                frame.setLocationRelativeTo(null);
            }
            frame.setVisible(true);
            app.start();
        });
    }
}
