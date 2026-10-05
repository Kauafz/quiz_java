import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;

/** Quiz de Segurança Digital: aplicação desktop 100% Java (Swing), sem bibliotecas externas. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            QuizApp app = new QuizApp(HistoryStore.defaultStore());
            JFrame frame = new JFrame("Quiz de Segurança Digital");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane((java.awt.Container) app.component());
            frame.setMinimumSize(new Dimension(520, 560));
            frame.setSize(1000, 820);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            app.start();
        });
    }
}
