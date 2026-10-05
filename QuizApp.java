import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.AbstractBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Telas do quiz (abertura, perguntas e resultado) e a animação do dragão. */
final class QuizApp {

    private static final List<Question> QUESTIONS = QuestionBank.ALL;
    private static final List<String> PARTS = QuestionBank.PARTS;
    private static final int PER_PART = QUESTIONS.size() / PARTS.size();
    private static final String[] LETTERS = {"A", "B", "C", "D"};
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    /** Fundo em degradê azul-roxo com o dragão voando por trás do conteúdo. */
    static final class Backdrop extends JPanel {
        private final Dragon dragon;
        boolean dragonOn = true;

        Backdrop(Dragon dragon) {
            super(new BorderLayout());
            this.dragon = dragon;
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = Math.max(getWidth(), 2);
            int h = Math.max(getHeight(), 2);
            g.setPaint(new LinearGradientPaint(0, 0, w, h, new float[]{0f, 0.55f, 1f},
                    new java.awt.Color[]{Theme.BG1, Theme.BG2, Theme.BG3}));
            g.fillRect(0, 0, getWidth(), getHeight());
            if (dragonOn) {
                dragon.resize(getWidth(), getHeight());
                dragon.paint(g);
            }
            g.dispose();
        }
    }

    private final HistoryStore store;
    private final Dragon dragon = new Dragon();
    private final Backdrop root = new Backdrop(dragon);
    private final JScrollPane scroll = new JScrollPane();
    private final Timer timer;
    private long lastTick = System.nanoTime();

    private int current;
    private int[] answers = new int[QUESTIONS.size()];
    private String playerName = "";

    QuizApp(HistoryStore store) {
        this.store = store;

        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        Ui.styleScrollBar(scroll.getVerticalScrollBar());

        JCheckBox toggle = new JCheckBox("Dragão animado", true);
        toggle.setOpaque(false);
        toggle.setFocusPainted(false);
        toggle.setForeground(Theme.MUTED);
        toggle.setFont(Theme.font(Font.PLAIN, 13));
        toggle.addActionListener(e -> setDragonOn(toggle.isSelected()));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 6));
        top.setOpaque(false);
        top.add(toggle);

        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);

        timer = new Timer(System.getProperty("quiz.embed") != null ? 33 : 16, e -> tick());
        timer.setCoalesce(true);
        showIntro();
    }

    JComponent component() {
        return root;
    }

    void start() {
        lastTick = System.nanoTime();
        timer.start();
    }

    Dragon dragon() {
        return dragon;
    }

    private void setDragonOn(boolean on) {
        root.dragonOn = on;
        if (on) {
            lastTick = System.nanoTime();
            timer.start();
        } else {
            timer.stop();
        }
        root.repaint();
    }

    private void tick() {
        long now = System.nanoTime();
        double dt = Math.min(0.05, (now - lastTick) / 1e9);
        lastTick = now;
        dragon.update(dt, pointerInRoot());
        root.repaint();
    }

    private Point pointerInRoot() {
        try {
            PointerInfo info = MouseInfo.getPointerInfo();
            if (info == null || !root.isShowing()) {
                return null;
            }
            Point p = info.getLocation();
            SwingUtilities.convertPointFromScreen(p, root);
            return root.contains(p) ? p : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ telas

    private void setPage(Ui.Page page) {
        scroll.setViewportView(page);
        scroll.getVerticalScrollBar().setValue(0);
        page.revalidate();
        root.repaint();
    }

    private static Ui.Paragraph text(String s, int style, float size, java.awt.Color color) {
        return new Ui.Paragraph(s, Theme.font(style, size), color);
    }

    private static <T extends JComponent> T fixedWidth(T c) {
        c.putClientProperty(Ui.Column.WIDTH, c.getPreferredSize().width);
        return c;
    }

    void showIntro() {
        current = 0;
        Arrays.fill(answers, -1);
        dragon.setStage(Dragon.Mode.INTRO, 0);

        List<String[]> steps = new ArrayList<>();
        for (int i = 0; i < PARTS.size(); i++) {
            steps.add(new String[]{String.valueOf(i + 1), PARTS.get(i), PER_PART + " perguntas"});
        }
        JTextField name = new JTextField(playerName);
        name.setOpaque(false);
        name.setForeground(Theme.INK);
        name.setCaretColor(Theme.INK);
        name.setFont(Theme.font(Font.PLAIN, 15));
        name.setBorder(new FieldBorder());
        name.setPreferredSize(new Dimension(300, 42));
        name.setDocument(new LimitedDocument(40));
        name.setText(playerName);

        Ui.GradientButton go = new Ui.GradientButton("Começar", true);
        go.addActionListener(e -> {
            playerName = name.getText().trim();
            showQuestion();
        });
        name.addActionListener(e -> go.doClick());

        Ui.Card card = new Ui.Card();
        card.add(text("Nível básico a intermediário", Font.BOLD, 14, Theme.PURPLE));
        card.add(text("Quiz de Segurança Digital", Font.BOLD, 40, Theme.INK));
        card.add(text("Teste o que você sabe sobre senhas, golpes, redes e privacidade. São " + QUESTIONS.size()
                + " perguntas em " + PARTS.size() + " etapas, cada uma com uma única resposta certa. "
                + "No final você vê seu desempenho em gráficos e entende cada erro.", Font.PLAIN, 16, Theme.MUTED));
        card.add(new Ui.RowsList(steps));
        card.add(text("Seu nome (opcional, para aparecer no ranking)", Font.PLAIN, 14, Theme.MUTED));
        card.add(name);
        card.add(fixedWidth(go));

        Ui.Page page = new Ui.Page();
        page.add(card);
        setPage(page);
    }

    void showQuestion() {
        Question q = QUESTIONS.get(current);
        dragon.setStage(Dragon.Mode.QUIZ, current);
        boolean last = current == QUESTIONS.size() - 1;

        Ui.GradientButton next = new Ui.GradientButton(last ? "Ver resultado" : "Próxima", true);
        next.setEnabled(answers[current] >= 0);
        Ui.GradientButton back = new Ui.GradientButton("Voltar", false);
        back.setEnabled(current > 0);

        List<Ui.OptionCard> options = new ArrayList<>();
        for (int i = 0; i < q.options().size(); i++) {
            final int index = i;
            Ui.OptionCard card = new Ui.OptionCard(LETTERS[i], q.options().get(i), () -> {
                answers[current] = index;
                for (int k = 0; k < options.size(); k++) {
                    options.get(k).setSelected(k == index);
                }
                next.setEnabled(true);
            });
            card.setSelected(answers[current] == i);
            options.add(card);
        }

        next.addActionListener(e -> {
            if (last) {
                showResult();
            } else {
                current++;
                showQuestion();
            }
        });
        back.addActionListener(e -> {
            current--;
            showQuestion();
        });

        int inPart = current % PER_PART;
        Ui.Card card = new Ui.Card();
        card.add(new Ui.TopInfo("Etapa " + (q.part() + 1) + " de " + PARTS.size(),
                "Pergunta " + (current + 1) + " de " + QUESTIONS.size()));
        card.add(new Ui.ProgressLine(current / (double) QUESTIONS.size()));
        card.add(text(PARTS.get(q.part()) + " · " + (inPart + 1) + "/" + PER_PART, Font.BOLD, 14, Theme.PURPLE));
        card.add(text(q.text(), Font.BOLD, 24, Theme.INK));
        for (Ui.OptionCard o : options) {
            card.add(o);
        }
        card.add(new Ui.NavRow(back, next));

        Ui.Page page = new Ui.Page();
        page.add(card);
        setPage(page);
        if (answers[current] >= 0) {
            options.get(answers[current]).requestFocusInWindow();
        }
    }

    void showResult() {
        dragon.setStage(Dragon.Mode.RESULT, 0);
        QuizEngine.Result result = QuizEngine.grade(QUESTIONS, PARTS, answers);
        store.save(playerName, result);
        buildResultPage(result);
    }

    /** Monta a tela de resultado (separado para permitir testes e capturas de tela). */
    void buildResultPage(QuizEngine.Result result) {
        Ui.Page page = new Ui.Page();

        Ui.Card score = new Ui.Card();
        score.add(text("Seu resultado", Font.BOLD, 14, Theme.PURPLE));
        score.add(new Ui.ScoreBlock(result.hits(), result.total()));
        score.add(text(QuizEngine.verdict(result.percentHits()), Font.PLAIN, 16, Theme.MUTED));
        page.add(score);

        Ui.Card overall = new Ui.Card();
        overall.add(text("Acertos e erros", Font.BOLD, 20, Theme.INK));
        overall.add(new Ui.OverallChart(result));
        page.add(overall);

        Ui.Card stages = new Ui.Card();
        stages.add(text("Resultado por etapa", Font.BOLD, 20, Theme.INK));
        stages.add(new Ui.StageCharts(result.parts()));
        page.add(stages);

        Ui.Card misses = new Ui.Card();
        int n = result.misses().size();
        misses.add(text(n > 0 ? "Perguntas que você errou (" + n + ")" : "Perguntas que você errou", Font.BOLD, 20, Theme.INK));
        if (n == 0) {
            misses.add(new Ui.AnswerBox("Parabéns!", "Você não errou nenhuma pergunta. Não há nada para revisar!",
                    Theme.OK, Theme.OK_SOFT));
        }
        for (QuizEngine.Miss m : result.misses()) {
            Ui.Card item = new Ui.Card(true, 18);
            item.add(text("Etapa " + (m.part() + 1) + " · " + m.partName() + " · pergunta " + m.position()
                    + " de " + PER_PART, Font.PLAIN, 13, Theme.MUTED));
            item.add(text(m.text(), Font.BOLD, 17, Theme.INK));
            item.add(new Ui.AnswerBox("Sua resposta", LETTERS[m.chosen()] + ") " + m.chosenText(), Theme.BAD, Theme.BAD_SOFT));
            item.add(new Ui.AnswerBox("Resposta certa", LETTERS[m.correct()] + ") " + m.correctText(), Theme.OK, Theme.OK_SOFT));
            item.add(text(m.explanation(), Font.PLAIN, 15, Theme.MUTED));
            misses.add(item);
        }
        page.add(misses);

        List<HistoryStore.Attempt> ranking = store.ranking(10);
        if (!ranking.isEmpty()) {
            List<String[]> rows = new ArrayList<>();
            for (int i = 0; i < ranking.size(); i++) {
                HistoryStore.Attempt a = ranking.get(i);
                rows.add(new String[]{String.valueOf(i + 1), a.name(), a.hits() + "/" + a.total()});
            }
            Ui.Card card = new Ui.Card();
            card.add(text("Ranking", Font.BOLD, 20, Theme.INK));
            card.add(new Ui.RowsList(rows));
            page.add(card);
        }

        List<HistoryStore.Attempt> recent = store.recent(5);
        if (!recent.isEmpty()) {
            List<String[]> rows = new ArrayList<>();
            for (HistoryStore.Attempt a : recent) {
                String who = a.named() ? a.name() : "Anônimo";
                rows.add(new String[]{"", DATE.format(Instant.ofEpochMilli(a.time())) + " · " + who, a.hits() + "/" + a.total()});
            }
            Ui.Card card = new Ui.Card();
            card.add(text("Seu histórico (últimas tentativas)", Font.BOLD, 20, Theme.INK));
            card.add(new Ui.RowsList(rows));
            page.add(card);
        }

        int[] wrong = store.wrongCounts(QUESTIONS.size());
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < wrong.length; i++) {
            if (wrong[i] > 0) {
                order.add(i);
            }
        }
        order.sort((a, b) -> Integer.compare(wrong[b], wrong[a]));
        if (!order.isEmpty()) {
            List<String[]> rows = new ArrayList<>();
            for (int i = 0; i < Math.min(5, order.size()); i++) {
                int q = order.get(i);
                rows.add(new String[]{String.valueOf(i + 1), QUESTIONS.get(q).text(),
                        wrong[q] + (wrong[q] == 1 ? " erro" : " erros")});
            }
            Ui.Card card = new Ui.Card();
            card.add(text("Perguntas mais erradas (todas as tentativas)", Font.BOLD, 20, Theme.INK));
            card.add(new Ui.RowsList(rows));
            page.add(card);
        }

        Ui.GradientButton again = new Ui.GradientButton("Refazer o quiz", true);
        again.addActionListener(e -> showIntro());
        page.add(fixedWidth(again));
        setPage(page);
    }

    // ------------------------------------------------------------------ utilidades de campo de texto

    /** Borda arredondada do campo de nome. */
    private static final class FieldBorder extends AbstractBorder {
        @Override
        public void paintBorder(Component c, Graphics g0, int x, int y, int w, int h) {
            Graphics2D g = Ui.quality(g0);
            g.setColor(c.hasFocus() ? Theme.PURPLE : Theme.LINE);
            g.setStroke(new java.awt.BasicStroke(c.hasFocus() ? 2f : 1.5f));
            g.draw(new RoundRectangle2D.Double(x + 1, y + 1, w - 2, h - 2, 12, 12));
            g.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(8, 14, 8, 14);
        }
    }

    /** Limita o tamanho do nome digitado. */
    private static final class LimitedDocument extends javax.swing.text.PlainDocument {
        private final int max;

        LimitedDocument(int max) {
            this.max = max;
        }

        @Override
        public void insertString(int offs, String str, javax.swing.text.AttributeSet a)
                throws javax.swing.text.BadLocationException {
            if (str == null) {
                return;
            }
            String clean = str.replace(';', ' ');
            if (getLength() + clean.length() <= max) {
                super.insertString(offs, clean, a);
            }
        }
    }
}
