import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.JScrollBar;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

/** Componentes visuais próprios do quiz (todos desenhados com Java2D). */
final class Ui {

    private Ui() {
    }

    static final FontRenderContext FRC = new FontRenderContext(null, true, true);

    /** Componentes cuja altura depende da largura disponível (texto que quebra linha). */
    interface HeightForWidth {
        int heightFor(int width);
    }

    // ------------------------------------------------------------------ texto

    static List<TextLayout> wrap(String text, Font font, float width) {
        List<TextLayout> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        for (String para : text.split("\n", -1)) {
            if (para.isEmpty()) {
                out.add(new TextLayout(" ", font, FRC));
                continue;
            }
            AttributedString as = new AttributedString(para);
            as.addAttribute(TextAttribute.FONT, font);
            LineBreakMeasurer m = new LineBreakMeasurer(as.getIterator(), FRC);
            while (m.getPosition() < para.length()) {
                out.add(m.nextLayout(Math.max(24f, width)));
            }
        }
        return out;
    }

    static float linesHeight(List<TextLayout> lines, float gap) {
        float h = 0;
        for (TextLayout t : lines) {
            h += t.getAscent() + t.getDescent() + t.getLeading() + gap;
        }
        return h > 0 ? h - gap : 0;
    }

    static float drawLines(Graphics2D g, List<TextLayout> lines, float x, float y, float gap) {
        for (TextLayout t : lines) {
            y += t.getAscent();
            t.draw(g, x, y);
            y += t.getDescent() + t.getLeading() + gap;
        }
        return y;
    }

    static Graphics2D quality(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    // ------------------------------------------------------------------ layout em coluna

    /** Empilha os componentes na vertical, usando a largura disponível para calcular a altura de cada um. */
    static final class Column implements LayoutManager {
        static final String WIDTH = "column.width";
        static final String CENTER = "column.center";

        private final int gap;
        private final int padX;
        private final int padY;
        private final int maxWidth;

        Column(int gap, int padX, int padY, int maxWidth) {
            this.gap = gap;
            this.padX = padX;
            this.padY = padY;
            this.maxWidth = maxWidth;
        }

        private int inner(int parentWidth) {
            int w = parentWidth - 2 * padX;
            return maxWidth > 0 ? Math.min(w, maxWidth) : w;
        }

        private static int childWidth(Component c, int w) {
            if (c instanceof JComponent) {
                Object fixed = ((JComponent) c).getClientProperty(WIDTH);
                if (fixed instanceof Integer) {
                    return Math.min((Integer) fixed, w);
                }
            }
            return w;
        }

        private static int childHeight(Component c, int w) {
            int cw = childWidth(c, w);
            if (c instanceof HeightForWidth) {
                return ((HeightForWidth) c).heightFor(cw);
            }
            return c.getPreferredSize().height;
        }

        int heightFor(Container parent, int parentWidth) {
            int w = inner(parentWidth);
            int h = 2 * padY;
            int n = 0;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) {
                    continue;
                }
                if (n++ > 0) {
                    h += gap;
                }
                h += childHeight(c, w);
            }
            return h;
        }

        @Override
        public void layoutContainer(Container parent) {
            int pw = parent.getWidth();
            int w = inner(pw);
            int x0 = (pw - w) / 2;
            int y = padY;
            for (Component c : parent.getComponents()) {
                if (!c.isVisible()) {
                    continue;
                }
                int cw = childWidth(c, w);
                int h = childHeight(c, w);
                int x = x0;
                if (c instanceof JComponent && Boolean.TRUE.equals(((JComponent) c).getClientProperty(CENTER))) {
                    x = x0 + (w - cw) / 2;
                }
                c.setBounds(x, y, cw, h);
                y += h + gap;
            }
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            int w = parent.getWidth() > 0 ? parent.getWidth() : 640;
            return new Dimension(w, heightFor(parent, w));
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }
    }

    /** Página rolável: acompanha a largura da janela e centraliza o conteúdo. */
    static final class Page extends JPanel implements Scrollable {
        private final Column column = new Column(16, 16, 24, 720);

        Page() {
            setOpaque(false);
            setLayout(column);
        }

        @Override
        public Dimension getPreferredSize() {
            Container p = getParent();
            int w = p instanceof JViewport ? p.getWidth() : getWidth();
            if (w <= 0) {
                w = 760;
            }
            return new Dimension(w, column.heightFor(this, w));
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 28;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(100, visible.height - 60);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Cartão translúcido com cantos arredondados. */
    static class Card extends JPanel implements HeightForWidth {
        private final Column column;
        private final boolean accent;

        Card() {
            this(false, 24);
        }

        Card(boolean accent, int pad) {
            this.accent = accent;
            this.column = new Column(14, pad, pad, 0);
            setOpaque(false);
            setLayout(column);
        }

        @Override
        public int heightFor(int width) {
            return column.heightFor(this, width);
        }

        @Override
        public Dimension getPreferredSize() {
            int w = getWidth() > 0 ? getWidth() : 640;
            return new Dimension(w, heightFor(w));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = quality(g0);
            Shape shape = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 18, 18);
            g.setColor(Theme.CARD);
            g.fill(shape);
            g.setColor(Theme.LINE);
            g.setStroke(new BasicStroke(1f));
            g.draw(shape);
            if (accent) {
                g.setClip(shape);
                g.setColor(Theme.BAD);
                g.fillRect(0, 0, 4, getHeight());
            }
            g.dispose();
        }
    }

    static JComponent spacer(int height) {
        JComponent c = new JComponent() {
        };
        c.setPreferredSize(new Dimension(1, height));
        return c;
    }

    // ------------------------------------------------------------------ componentes simples

    static final class Paragraph extends JComponent implements HeightForWidth {
        private final String text;
        private final Font font;
        private final Color color;
        private final float gap;
        private List<TextLayout> cache;
        private int cacheWidth = -1;

        Paragraph(String text, Font font, Color color) {
            this(text, font, color, 3f);
        }

        Paragraph(String text, Font font, Color color, float gap) {
            this.text = text;
            this.font = font;
            this.color = color;
            this.gap = gap;
            setOpaque(false);
        }

        private List<TextLayout> lines(int w) {
            if (cache == null || w != cacheWidth) {
                cache = wrap(text, font, w);
                cacheWidth = w;
            }
            return cache;
        }

        @Override
        public int heightFor(int w) {
            return (int) Math.ceil(linesHeight(lines(w), gap)) + 1;
        }

        @Override
        public Dimension getPreferredSize() {
            int w = getWidth() > 0 ? getWidth() : 480;
            return new Dimension(w, heightFor(w));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = quality(g0);
            g.setColor(color);
            drawLines(g, lines(getWidth()), 0, 0, gap);
            g.dispose();
        }
    }

    /** Linha de informação: texto à esquerda e à direita (ex.: "Etapa 2 de 5" / "Pergunta 6 de 20"). */
    static final class TopInfo extends JComponent implements HeightForWidth {
        private final String left;
        private final String right;

        TopInfo(String left, String right) {
            this.left = left;
            this.right = right;
            setOpaque(false);
        }

        @Override
        public int heightFor(int w) {
            return 22;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(480, 22);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = quality(g0);
            g.setFont(Theme.font(Font.PLAIN, 14));
            g.setColor(Theme.MUTED);
            FontMetrics fm = g.getFontMetrics();
            int y = fm.getAscent() + 2;
            g.drawString(left, 0, y);
            g.drawString(right, getWidth() - fm.stringWidth(right), y);
            g.dispose();
        }
    }

    static final class ProgressLine extends JComponent implements HeightForWidth {
        private final double fraction;

        ProgressLine(double fraction) {
            this.fraction = fraction;
            setOpaque(false);
        }

        @Override
        public int heightFor(int w) {
            return 8;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(480, 8);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = quality(g0);
            int w = getWidth();
            g.setColor(Theme.LINE);
            g.fill(new RoundRectangle2D.Double(0, 0, w, 8, 8, 8));
            int fw = (int) Math.round(w * fraction);
            if (fw > 0) {
                g.setPaint(new GradientPaint(0, 0, Theme.GRAD_A, Math.max(w, 1), 0, Theme.GRAD_B));
                g.fill(new RoundRectangle2D.Double(0, 0, Math.max(fw, 8), 8, 8, 8));
            }
            g.dispose();
        }
    }

    static final class OptionCard extends JComponent implements HeightForWidth {
        private static final int PAD_X = 16;
        private static final int PAD_Y = 14;
        private static final int BADGE = 30;
        private static final int GAP = 14;
        private static final Font TEXT = Theme.font(Font.PLAIN, 16);
        private static final Font LETTER = Theme.font(Font.BOLD, 14);

        private final String letter;
        private final String text;
        private final Runnable onPick;
        private boolean selected;
        private boolean hover;

        OptionCard(String letter, String text, Runnable onPick) {
            this.letter = letter;
            this.text = text;
            this.onPick = onPick;
            setOpaque(false);
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    requestFocusInWindow();
                    onPick.run();
                }
            });
            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    repaint();
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    repaint();
                }
            });
            javax.swing.Action pick = new javax.swing.AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onPick.run();
                }
            };
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "pick");
            getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "pick");
            getActionMap().put("pick", pick);
        }

        void setSelected(boolean selected) {
            this.selected = selected;
            repaint();
        }

        @Override
        public int heightFor(int w) {
            int tw = w - 2 * PAD_X - BADGE - GAP;
            int h = (int) Math.ceil(linesHeight(wrap(text, TEXT, tw), 3));
            return Math.max(BADGE, h) + 2 * PAD_Y;
        }

        @Override
        public Dimension getPreferredSize() {
            int w = getWidth() > 0 ? getWidth() : 600;
            return new Dimension(w, heightFor(w));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = quality(g0);
            int w = getWidth();
            int h = getHeight();
            Shape shape = new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 14, 14);
            if (selected) {
                g.setColor(Theme.PICK);
                g.fill(shape);
            } else if (hover) {
                g.setColor(new Color(160, 107, 255, 20));
                g.fill(shape);
            }
            boolean focus = isFocusOwner();
            g.setColor(selected || hover ? Theme.PURPLE : Theme.LINE);
            g.setStroke(new BasicStroke(selected ? 2f : 1.5f));
            g.draw(shape);
            if (focus) {
                g.setColor(Theme.BLUE);
                g.setStroke(new BasicStroke(2f));
                g.draw(new RoundRectangle2D.Double(-1, -1, w + 1, h + 1, 16, 16));
            }

            List<TextLayout> lines = wrap(text, TEXT, w - 2 * PAD_X - BADGE - GAP);
            float textH = linesHeight(lines, 3);
            float contentH = Math.max(BADGE, textH);
            float top = (h - contentH) / 2f;

            Ellipse2D badge = new Ellipse2D.Double(PAD_X, top, BADGE, BADGE);
            if (selected) {
                g.setPaint(new GradientPaint(PAD_X, (float) top, Theme.GRAD_A, PAD_X + BADGE, (float) top + BADGE, Theme.GRAD_B));
                g.fill(badge);
            } else {
                g.setColor(Theme.LINE);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(badge);
            }
            g.setFont(LETTER);
            g.setColor(selected ? Color.WHITE : Theme.MUTED);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(letter, PAD_X + (BADGE - fm.stringWidth(letter)) / 2f,
                    (float) top + (BADGE - fm.getHeight()) / 2f + fm.getAscent());

            g.setColor(Theme.INK);
            drawLines(g, lines, PAD_X + BADGE + GAP, top + (contentH - textH) / 2f, 3);
            g.dispose();
        }
    }

    static final class GradientButton extends JButton {
        private final boolean primary;

        GradientButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(Theme.font(Font.BOLD, 15));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(getText()) + 52, 46);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = quality(g0);
            int w = getWidth();
            int h = getHeight();
            Shape shape = new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 14, 14);
            float alpha = isEnabled() ? 1f : 0.4f;
            g.setComposite(java.awt.AlphaComposite.SrcOver.derive(alpha));
            if (primary) {
                Color a = Theme.GRAD_A;
                Color b = Theme.GRAD_B;
                if (getModel().isPressed()) {
                    a = a.darker();
                    b = b.darker();
                }
                g.setPaint(new GradientPaint(0, 0, a, w, h, b));
                g.fill(shape);
            } else {
                g.setColor(getModel().isRollover() ? Theme.PURPLE : Theme.LINE);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(shape);
            }
            if (hasFocus() && isEnabled()) {
                g.setColor(Theme.BLUE);
                g.setStroke(new BasicStroke(2f));
                g.draw(new RoundRectangle2D.Double(-1, -1, w + 1, h + 1, 16, 16));
            }
            g.setFont(getFont());
            g.setColor(primary ? Color.WHITE : Theme.INK);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(getText(), (w - fm.stringWidth(getText())) / 2f, (h - fm.getHeight()) / 2f + fm.getAscent());
            g.dispose();
        }
    }

    /** Linha com o botão "Voltar" à esquerda e o botão principal à direita. */
    static final class NavRow extends JPanel implements HeightForWidth {
        private final JComponent left;
        private final JComponent right;

        NavRow(JComponent left, JComponent right) {
            super(null);
            this.left = left;
            this.right = right;
            setOpaque(false);
            add(left);
            if (right != null) {
                add(right);
            }
        }

        @Override
        public int heightFor(int w) {
            return 46;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(480, 46);
        }

        @Override
        public void doLayout() {
            Dimension l = left.getPreferredSize();
            left.setBounds(0, 0, l.width, 46);
            if (right != null) {
                Dimension r = right.getPreferredSize();
                right.setBounds(getWidth() - r.width, 0, r.width, 46);
            }
        }
    }

    // ------------------------------------------------------------------ listas

    /** Lista de linhas {emblema, texto, direita}; o texto quebra linha conforme a largura. */
    static final class RowsList extends JComponent implements HeightForWidth {
        private static final Font TEXT = Theme.font(Font.PLAIN, 15);
        private static final Font RIGHT = Theme.font(Font.BOLD, 14);
        private static final Font BADGE_FONT = Theme.font(Font.BOLD, 13);
        private static final int BADGE = 28;

        private final List<String[]> rows;

        RowsList(List<String[]> rows) {
            this.rows = rows;
            setOpaque(false);
        }

        private int rightWidth() {
            int w = 0;
            for (String[] r : rows) {
                if (!r[2].isEmpty()) {
                    w = Math.max(w, (int) Math.ceil(new TextLayout(r[2], RIGHT, FRC).getAdvance()));
                }
            }
            return w;
        }

        private boolean hasBadge() {
            for (String[] r : rows) {
                if (!r[0].isEmpty()) {
                    return true;
                }
            }
            return false;
        }

        private int textWidth(int w) {
            int tw = w - (hasBadge() ? BADGE + 12 : 0) - (rightWidth() > 0 ? rightWidth() + 14 : 0) - 4;
            return Math.max(60, tw);
        }

        private int rowHeight(String[] r, int w) {
            int th = (int) Math.ceil(linesHeight(wrap(r[1], TEXT, textWidth(w)), 3));
            return Math.max(40, th + 20);
        }

        @Override
        public int heightFor(int w) {
            int h = 0;
            for (String[] r : rows) {
                h += rowHeight(r, w);
            }
            return h;
        }

        @Override
        public Dimension getPreferredSize() {
            int w = getWidth() > 0 ? getWidth() : 600;
            return new Dimension(w, heightFor(w));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = quality(g0);
            int w = getWidth();
            int y = 0;
            int rw = rightWidth();
            boolean badge = hasBadge();
            for (int i = 0; i < rows.size(); i++) {
                String[] r = rows.get(i);
                int h = rowHeight(r, w);
                g.setColor(Theme.LINE);
                g.setStroke(new BasicStroke(1f));
                g.drawLine(0, y + h - 1, w, y + h - 1);
                int x = 0;
                if (badge && !r[0].isEmpty()) {
                    double by = y + (h - BADGE) / 2.0;
                    g.setColor(Theme.PICK);
                    g.fill(new Ellipse2D.Double(0, by, BADGE, BADGE));
                    g.setFont(BADGE_FONT);
                    g.setColor(Theme.PURPLE);
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(r[0], (BADGE - fm.stringWidth(r[0])) / 2f, (float) by + (BADGE - fm.getHeight()) / 2f + fm.getAscent());
                }
                if (badge) {
                    x = BADGE + 12;
                }
                List<TextLayout> lines = wrap(r[1], TEXT, textWidth(w));
                float th = linesHeight(lines, 3);
                g.setColor(Theme.INK);
                drawLines(g, lines, x, y + (h - th) / 2f, 3);
                if (!r[2].isEmpty()) {
                    g.setFont(RIGHT);
                    g.setColor(Theme.MUTED);
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(r[2], w - fm.stringWidth(r[2]), y + (h - fm.getHeight()) / 2f + fm.getAscent());
                }
                y += h;
            }
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ resultado

    /** Pontuação grande em degradê azul-roxo. */
    static final class ScoreBlock extends JComponent implements HeightForWidth {
        private final int hits;
        private final int total;

        ScoreBlock(int hits, int total) {
            this.hits = hits;
            this.total = total;
            setOpaque(false);
        }

        @Override
        public int heightFor(int w) {
            return 92;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(480, 92);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = quality(g0);
            Font big = Theme.font(Font.BOLD, 84);
            TextLayout t = new TextLayout(String.valueOf(hits), big, FRC);
            float baseline = 78;
            g.setPaint(new GradientPaint(0, 0, Theme.GRAD_A, t.getAdvance(), 80, Theme.GRAD_B));
            t.draw(g, 0, baseline);
            TextLayout of = new TextLayout("de " + total, Theme.font(Font.BOLD, 30), FRC);
            g.setColor(Theme.MUTED);
            of.draw(g, t.getAdvance() + 12, baseline);
            g.dispose();
        }
    }

    static void drawPie(Graphics2D g, double cx, double cy, double r, double[] values, Color[] colors) {
        double total = 0;
        for (double v : values) {
            total += v;
        }
        if (total <= 0) {
            return;
        }
        int nonZero = 0;
        int only = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > 0) {
                nonZero++;
                only = i;
            }
        }
        if (nonZero == 1) {
            g.setColor(colors[only]);
            g.fill(new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r));
            return;
        }
        double start = 90;
        for (int i = 0; i < values.length; i++) {
            if (values[i] <= 0) {
                continue;
            }
            double extent = -values[i] / total * 360.0;
            Arc2D arc = new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r, start, extent, Arc2D.PIE);
            g.setColor(colors[i]);
            g.fill(arc);
            g.setColor(new Color(17, 21, 56));
            g.setStroke(new BasicStroke(2f));
            g.draw(arc);
            start += extent;
        }
    }

    static String pct(double v) {
        return (v == Math.rint(v) ? String.valueOf((int) v) : String.valueOf(v).replace('.', ',')) + "%";
    }

    /** Pizza geral de acertos e erros com legenda. */
    static final class OverallChart extends JComponent implements HeightForWidth {
        private final QuizEngine.Result result;

        OverallChart(QuizEngine.Result result) {
            this.result = result;
            setOpaque(false);
        }

        private static boolean wide(int w) {
            return w >= 500;
        }

        @Override
        public int heightFor(int w) {
            return wide(w) ? 230 : 230 + 24 + 2 * 56;
        }

        @Override
        public Dimension getPreferredSize() {
            int w = getWidth() > 0 ? getWidth() : 600;
            return new Dimension(w, heightFor(w));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = quality(g0);
            int w = getWidth();
            double r = 105;
            double pieX;
            double legendX;
            double legendY;
            if (wide(w)) {
                double groupW = 2 * r + 48 + 210;
                pieX = (w - groupW) / 2 + r;
                legendX = pieX + r + 48;
                legendY = 115 - 56;
            } else {
                pieX = w / 2.0;
                legendX = Math.max(0, (w - 210) / 2.0);
                legendY = 230 + 24;
            }
            drawPie(g, pieX, 115, r, new double[]{result.hits(), result.errors()}, new Color[]{Theme.OK, Theme.BAD});
            legend(g, legendX, legendY, Theme.OK, result.hits() + " acertos", pct(result.percentHits()) + " das perguntas");
            legend(g, legendX, legendY + 56, Theme.BAD, result.errors() + " erros", pct(result.percentErrors()) + " das perguntas");
            g.dispose();
        }

        private static void legend(Graphics2D g, double x, double y, Color c, String title, String sub) {
            g.setColor(c);
            g.fill(new Ellipse2D.Double(x, y + 6, 14, 14));
            g.setFont(Theme.font(Font.BOLD, 20));
            g.setColor(Theme.INK);
            g.drawString(title, (float) x + 26, (float) y + 20);
            g.setFont(Theme.font(Font.PLAIN, 14));
            g.setColor(Theme.MUTED);
            g.drawString(sub, (float) x + 26, (float) y + 40);
        }
    }

    /** Uma pizza pequena por etapa, com acertos, erros e porcentagens. */
    static final class StageCharts extends JComponent implements HeightForWidth {
        private static final int CELL_W = 190;
        private static final int PIE_R = 56;
        private static final Font TITLE = Theme.font(Font.BOLD, 14);
        private final List<QuizEngine.PartResult> parts;

        StageCharts(List<QuizEngine.PartResult> parts) {
            this.parts = parts;
            setOpaque(false);
        }

        private int columns(int w) {
            return Math.max(1, Math.min(parts.size(), w / CELL_W));
        }

        private float titleHeight() {
            float h = 0;
            for (QuizEngine.PartResult p : parts) {
                h = Math.max(h, linesHeight(wrap(p.name(), TITLE, CELL_W - 20), 2));
            }
            return h;
        }

        private int cellHeight() {
            return (int) Math.ceil(2 * PIE_R + 14 + titleHeight() + 8 + 2 * 20) + 12;
        }

        @Override
        public int heightFor(int w) {
            int rows = (int) Math.ceil(parts.size() / (double) columns(w));
            return rows * cellHeight();
        }

        @Override
        public Dimension getPreferredSize() {
            int w = getWidth() > 0 ? getWidth() : 600;
            return new Dimension(w, heightFor(w));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = quality(g0);
            int w = getWidth();
            int cols = columns(w);
            double cellW = w / (double) cols;
            int cellH = cellHeight();
            for (int i = 0; i < parts.size(); i++) {
                QuizEngine.PartResult p = parts.get(i);
                int col = i % cols;
                int row = i / cols;
                double cx = col * cellW + cellW / 2;
                double top = row * cellH;
                drawPie(g, cx, top + PIE_R + 2, PIE_R, new double[]{p.hits(), p.misses()}, new Color[]{Theme.OK, Theme.BAD});
                float y = (float) top + 2 * PIE_R + 16;
                g.setColor(Theme.INK);
                for (TextLayout t : wrap((i + 1) + ". " + p.name(), TITLE, (float) cellW - 20)) {
                    y += t.getAscent();
                    t.draw(g, (float) cx - t.getAdvance() / 2f, y);
                    y += t.getDescent() + t.getLeading() + 2;
                }
                y = (float) top + 2 * PIE_R + 16 + titleHeight() + 12;
                g.setFont(Theme.font(Font.PLAIN, 13));
                FontMetrics fm = g.getFontMetrics();
                String ok = p.hits() + (p.hits() == 1 ? " acerto" : " acertos") + " · " + pct(p.percentHits());
                String bad = p.misses() + (p.misses() == 1 ? " erro" : " erros") + " · " + pct(100 - p.percentHits());
                g.setColor(Theme.OK);
                g.drawString(ok, (float) cx - fm.stringWidth(ok) / 2f, y + fm.getAscent());
                g.setColor(Theme.BAD);
                g.drawString(bad, (float) cx - fm.stringWidth(bad) / 2f, y + 20 + fm.getAscent());
            }
            g.dispose();
        }
    }

    /** Caixa colorida com "Sua resposta" ou "Resposta certa". */
    static final class AnswerBox extends JComponent implements HeightForWidth {
        private static final Font LABEL = Theme.font(Font.BOLD, 12);
        private static final Font TEXT = Theme.font(Font.PLAIN, 15);
        private final String label;
        private final String text;
        private final Color accent;
        private final Color soft;

        AnswerBox(String label, String text, Color accent, Color soft) {
            this.label = label;
            this.text = text;
            this.accent = accent;
            this.soft = soft;
            setOpaque(false);
        }

        @Override
        public int heightFor(int w) {
            return (int) Math.ceil(linesHeight(wrap(text, TEXT, w - 28), 3)) + 22 + 20;
        }

        @Override
        public Dimension getPreferredSize() {
            int w = getWidth() > 0 ? getWidth() : 560;
            return new Dimension(w, heightFor(w));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = quality(g0);
            g.setColor(soft);
            g.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
            g.setFont(LABEL);
            g.setColor(accent);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(label, 14, 10 + fm.getAscent());
            g.setColor(Theme.INK);
            drawLines(g, wrap(text, TEXT, getWidth() - 28), 14, 10 + fm.getHeight() + 4, 3);
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ barra de rolagem

    static final class ScrollUi extends BasicScrollBarUI {
        private static JButton zero() {
            JButton b = new JButton();
            Dimension d = new Dimension(0, 0);
            b.setPreferredSize(d);
            b.setMinimumSize(d);
            b.setMaximumSize(d);
            return b;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zero();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zero();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        }

        @Override
        protected void paintThumb(Graphics g0, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g = quality(g0);
            g.setColor(new Color(160, 107, 255, 140));
            g.fill(new RoundRectangle2D.Double(r.x + 3, r.y + 2, r.width - 6, r.height - 4, 8, 8));
            g.dispose();
        }
    }

    static void styleScrollBar(JScrollBar bar) {
        bar.setUI(new ScrollUi());
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(12, 0));
        bar.setUnitIncrement(28);
    }
}
