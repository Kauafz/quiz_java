import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Dragão voador desenhado com Java2D: corpo articulado que segue um caminho de voo,
 * asas que batem, patas, cauda com barbatana, cabeça detalhada e sopro de faíscas.
 */
final class Dragon {

    enum Mode { INTRO, QUIZ, RESULT }

    private static final int N = 54;     // segmentos do corpo
    private static final int SH = 8;     // ombros (asas e patas dianteiras)
    private static final int HIP = 25;   // quadris (patas traseiras)

    private static final Color RIM = new Color(0x141a5a);
    private static final Color BELLY = new Color(224, 218, 255, 235);
    private static final Color PLATE = new Color(110, 100, 200, 128);
    private static final Color BONE = new Color(0xece8ff);
    private static final Color SPINE = new Color(0xa78bfa);
    private static final Color EDGE = new Color(196, 181, 253, 242);
    private static final Color EYE = new Color(0x62e6ff);
    private static final Color EYE_CLEAR = new Color(98, 230, 255, 0);
    private static final Color MOUTH = new Color(0x22103f);
    private static final Color WING_A = new Color(139, 92, 246, 209);
    private static final Color WING_B = new Color(59, 130, 246, 158);
    private static final Color FIN = new Color(139, 92, 246, 235);

    private static final class Particle {
        double x, y, vx, vy, life, max, size;
    }

    private final double[] px = new double[N];
    private final double[] py = new double[N];
    private final double[] qx = new double[N];
    private final double[] qy = new double[N];
    private final double[] qfx = new double[N];
    private final double[] qfy = new double[N];
    private final double[] qf = new double[N];
    private final double[] radius = new double[N];
    private final Color[] bodyColor = new Color[N];
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();

    private int width = 900;
    private int height = 700;
    private double len = 14;
    private double maxR = 12;
    private double wingSpan = 200;

    private double time;
    private double roarT = 1;
    private double headFlip = 1;
    private double headA;
    private double sideSh = 1;
    private double sideHip = 1;
    private double flapPh;
    private double jaw = 0.06;
    private double emitAcc;
    private Point pointer;
    private Mode mode = Mode.INTRO;
    private double stageP;

    Dragon() {
        for (int i = 0; i < N; i++) {
            double t = i / (double) N;
            bodyColor[i] = new Color((int) Math.round(mix(58, 142, t)), (int) Math.round(mix(110, 76, t)),
                    (int) Math.round(mix(240, 232, t)));
            px[i] = -120;
            py[i] = 300;
        }
        resize(width, height);
    }

    // ------------------------------------------------------------------ estado

    void setStage(Mode mode, double p) {
        this.mode = mode;
        this.stageP = p;
        this.roarT = 0;
    }

    void resize(int w, int h) {
        width = Math.max(w, 1);
        height = Math.max(h, 1);
        len = clamp(Math.min(width, height) * 0.028, 8, 15);
        maxR = len * 1.05;
        wingSpan = len * 15;
        for (int i = 0; i < N; i++) {
            double t = i / (double) N;
            double sm = clamp(t / 0.18, 0, 1);
            radius[i] = maxR * (0.6 + 0.4 * sm * sm * (3 - 2 * sm)) * Math.pow(1 - t, 0.85) + 0.8;
        }
    }

    // ------------------------------------------------------------------ simulação

    private double[] target() {
        double k = mode == Mode.QUIZ ? stageP * 1.7 : mode == Mode.RESULT ? 7.3 : 0;
        double x = width * (0.5 + 0.46 * Math.sin(time * 0.23 + k));
        double y = height * (0.5 + 0.4 * Math.sin(time * 0.31 + k * 1.3 + 1));
        return new double[]{x, y};
    }

    private double[] dirAt(int i) {
        int a = Math.max(i - 1, 0);
        int b = Math.min(i + 1, N - 1);
        double fx = px[a] - px[b];
        double fy = py[a] - py[b];
        double l = Math.hypot(fx, fy);
        if (l == 0) {
            l = 1;
        }
        return new double[]{fx / l, fy / l};
    }

    void update(double dt, Point pointerPos) {
        this.pointer = pointerPos;
        time += dt;
        double[] t = target();
        double dx = t[0] - px[0];
        double dy = t[1] - py[0];
        double d = Math.hypot(dx, dy);
        if (d == 0) {
            d = 1;
        }
        double s = d * (1 - Math.exp(-dt * 2.6));
        s = Math.min(s, 520 * dt);
        px[0] += dx / d * s;
        py[0] += dy / d * s;
        for (int i = 1; i < N; i++) {
            double ex = px[i] - px[i - 1];
            double ey = py[i] - py[i - 1];
            double l = Math.hypot(ex, ey);
            if (l == 0) {
                l = 1;
            }
            px[i] = px[i - 1] + ex / l * len;
            py[i] = py[i - 1] + ey / l * len;
        }
        headA = Math.atan2(py[0] - py[3], px[0] - px[3]);
        headFlip += ((Math.cos(headA) >= 0 ? 1 : -1) - headFlip) * Math.min(1, dt * 9);
        sideSh += ((dirAt(SH)[0] >= 0 ? 1 : -1) - sideSh) * Math.min(1, dt * 7);
        sideHip += ((dirAt(HIP)[0] >= 0 ? 1 : -1) - sideHip) * Math.min(1, dt * 7);
        flapPh += dt * (4.6 + clamp(s / dt / 170, 0, 2.6));
        if (roarT < 1) {
            roarT = Math.min(1, roarT + dt / 0.95);
        }

        double hover = 0;
        if (pointer != null) {
            hover = clamp(1 - Math.hypot(pointer.x - px[0], pointer.y - py[0]) / 240, 0, 1) * 0.6;
        }
        double roarJ = roarT < 1 ? Math.sin(roarT * Math.PI) * 0.75 : 0;
        jaw = clamp(0.06 + 0.04 * Math.sin(time * 2.1) + hover + roarJ, 0, 0.85);

        if (roarT < 0.8 && jaw > 0.35) {
            emitAcc += dt * 70;
            while (emitAcc >= 1) {
                emitBreath();
                emitAcc -= 1;
            }
        }
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vx *= 0.985;
            p.vy *= 0.985;
            p.life -= dt;
            if (p.life <= 0) {
                particles.remove(i);
            }
        }
    }

    private void emitBreath() {
        if (particles.size() > 140) {
            return;
        }
        double u = maxR * 1.15;
        double mx = 2.8 * u;
        double my = 0.3 * u;
        double c = Math.cos(headA);
        double s = Math.sin(headA);
        double a = headA + (random.nextDouble() - 0.5) * 0.7;
        double sp = 90 + random.nextDouble() * 140;
        Particle p = new Particle();
        p.x = px[0] + mx * c - my * headFlip * s;
        p.y = py[0] + mx * s + my * headFlip * c;
        p.vx = Math.cos(a) * sp;
        p.vy = Math.sin(a) * sp;
        p.life = 0.6 + random.nextDouble() * 0.5;
        p.max = p.life;
        p.size = 3 + random.nextDouble() * 4;
        particles.add(p);
    }

    /** Roda a simulação por alguns segundos (usado para posicionar o dragão em capturas de tela). */
    void settle(double seconds) {
        for (double t = 0; t < seconds; t += 1 / 30.0) {
            update(1 / 30.0, null);
        }
    }

    // ------------------------------------------------------------------ desenho

    void paint(Graphics2D g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        computeBody();
        drawWing(g, true);
        drawLeg(g, SH + 4, sideSh, true);
        drawLeg(g, HIP + 3, sideHip, true);
        drawTailFin(g);
        drawSpines(g);
        drawBody(g);
        drawLeg(g, SH + 4, sideSh, false);
        drawLeg(g, HIP + 3, sideHip, false);
        drawWing(g, false);
        drawHead(g);
        drawParticles(g);
        g.dispose();
    }

    private void computeBody() {
        double amp = len * 0.45;
        for (int i = 0; i < N; i++) {
            double[] d = dirAt(i);
            double w = amp * Math.sin(i * 0.3 - time * 3) * (0.15 + 0.85 * i / N);
            qx[i] = px[i] - d[1] * w;
            qy[i] = py[i] + d[0] * w;
            qfx[i] = d[0];
            qfy[i] = d[1];
            qf[i] = Math.tanh(d[0] * 3);
        }
    }

    private static void seg(Graphics2D g, double ax, double ay, double bx, double by, double w, Color c) {
        g.setStroke(new BasicStroke((float) w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(c);
        g.draw(new Line2D.Double(ax, ay, bx, by));
    }

    private void drawSpines(Graphics2D g) {
        g.setColor(SPINE);
        for (int i = 4; i < N * 0.9; i += 2) {
            double f = qf[i];
            double nx = qfy[i] * f;
            double ny = -qfx[i] * f;
            double r = radius[i];
            double hg = r * (i < N * 0.5 ? 1.25 : 0.9);
            double bx = qx[i] + nx * r * 0.7;
            double by = qy[i] + ny * r * 0.7;
            Path2D p = new Path2D.Double();
            p.moveTo(bx + qfx[i] * len * 0.5, by + qfy[i] * len * 0.5);
            p.lineTo(bx - qfx[i] * len * 0.5, by - qfy[i] * len * 0.5);
            p.lineTo(qx[i] + nx * (r + hg) - qfx[i] * len * 0.9, qy[i] + ny * (r + hg) - qfy[i] * len * 0.9);
            p.closePath();
            g.fill(p);
        }
    }

    private void drawBody(Graphics2D g) {
        for (int i = N - 1; i > 0; i--) {
            seg(g, qx[i], qy[i], qx[i - 1], qy[i - 1], radius[i] * 2 + 3, RIM);
        }
        for (int i = N - 1; i > 0; i--) {
            seg(g, qx[i], qy[i], qx[i - 1], qy[i - 1], radius[i] * 2, bodyColor[i]);
        }
        // barriga clara com placas
        for (int i = (int) Math.floor(N * 0.86); i > 2; i--) {
            double fa = Math.abs(qf[i]);
            double sa = qf[i] >= 0 ? 1 : -1;
            double sb = qf[i - 1] >= 0 ? 1 : -1;
            double oa = radius[i] * 0.52 * sa;
            double ob = radius[i - 1] * 0.52 * sb;
            double w = radius[i] * 0.62 * clamp(fa * 2, 0, 1);
            if (w < 0.5) {
                continue;
            }
            seg(g, qx[i] - qfy[i] * oa, qy[i] + qfx[i] * oa,
                    qx[i - 1] - qfy[i - 1] * ob, qy[i - 1] + qfx[i - 1] * ob, w, BELLY);
            if (i % 2 == 0 && fa > 0.35) {
                double nx = -qfy[i] * sa;
                double ny = qfx[i] * sa;
                seg(g, qx[i] + nx * radius[i] * 0.22, qy[i] + ny * radius[i] * 0.22,
                        qx[i] + nx * radius[i] * 0.82, qy[i] + ny * radius[i] * 0.82, 1, PLATE);
            }
        }
        // escamas e brilho no dorso
        g.setColor(new Color(255, 255, 255, 41));
        for (int i = 5; i < N * 0.85; i++) {
            double nx = qfy[i] * qf[i];
            double ny = -qfx[i] * qf[i];
            double r = radius[i];
            double o = (i % 2 == 1 ? 0.28 : 0.02) * r;
            double dr = Math.max(0.8, r * 0.13);
            g.fill(new Ellipse2D.Double(qx[i] + nx * o - dr, qy[i] + ny * o - dr, dr * 2, dr * 2));
        }
        Color shine = new Color(255, 255, 255, 77);
        for (int i = (int) Math.floor(N * 0.9); i > 1; i--) {
            double oa = radius[i] * 0.5 * qf[i];
            double ob = radius[i - 1] * 0.5 * qf[i - 1];
            seg(g, qx[i] + qfy[i] * oa, qy[i] - qfx[i] * oa,
                    qx[i - 1] + qfy[i - 1] * ob, qy[i - 1] - qfx[i - 1] * ob, Math.max(1, radius[i] * 0.26), shine);
        }
    }

    private void drawTailFin(Graphics2D g0) {
        Graphics2D g = (Graphics2D) g0.create();
        double ang = Math.atan2(py[N - 1] - py[N - 4], px[N - 1] - px[N - 4]);
        double sw = Math.sin(time * 3 - N * 0.3) * 0.22;
        g.translate(qx[N - 1], qy[N - 1]);
        g.rotate(ang + sw);
        g.scale(1, safe(sideHip));
        double f = len * 3.6;
        double wd = len * 1.5;
        Path2D p = new Path2D.Double();
        p.moveTo(-len * 0.4, 0);
        p.quadTo(f * 0.35, -wd * 1.25, f, 0);
        p.quadTo(f * 0.35, wd * 1.25, -len * 0.4, 0);
        p.closePath();
        g.setPaint(new GradientPaint(0, 0, WING_A, (float) f, 0, WING_B));
        g.fill(p);
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(EDGE);
        g.draw(p);
        g.setColor(BONE);
        g.draw(new Line2D.Double(0, 0, f * 0.92, 0));
        g.dispose();
    }

    private void drawWing(Graphics2D g0, boolean far) {
        Graphics2D g = (Graphics2D) g0.create();
        double sc = far ? 0.86 : 1;
        double ph = flapPh + (far ? 0.55 : 0);
        double s = wingSpan * sc;
        double ang = Math.atan2(qfy[SH], qfx[SH]);
        g.translate(qx[SH] + (far ? qfx[SH] * len * 0.8 : 0), qy[SH] + (far ? qfy[SH] * len * 0.8 : 0));
        g.rotate(ang);
        g.scale(1, safe(sideSh));
        double th = 0.3 + 0.95 * Math.sin(ph);
        g.rotate(th);
        double lag = Math.cos(ph - 0.9) * 0.3;
        double[] wr = {-0.1 * s, -0.62 * s};
        double[][] raw = {{0.2, -1.0}, {-0.32, -1.08}, {-0.8, -0.9}, {-1.02, -0.52}};
        double[][] tips = new double[4][2];
        for (int k = 0; k < 4; k++) {
            double tx = raw[k][0] * s - wr[0];
            double ty = raw[k][1] * s - wr[1];
            double a = lag * (0.4 + k * 0.35);
            double c = Math.cos(a);
            double sn = Math.sin(a);
            tips[k][0] = wr[0] + tx * c - ty * sn;
            tips[k][1] = wr[1] + tx * sn + ty * c;
        }
        double c0 = Math.cos(-th);
        double s0 = Math.sin(-th);
        double bx = -0.9 * s;
        double by = 0.04 * s;
        double[] b = {bx * c0 - by * s0, bx * s0 + by * c0};

        Path2D m = new Path2D.Double();
        m.moveTo(0, 0);
        m.lineTo(wr[0], wr[1]);
        m.lineTo(tips[0][0], tips[0][1]);
        scallop(m, tips[0], tips[1], wr);
        scallop(m, tips[1], tips[2], wr);
        scallop(m, tips[2], tips[3], wr);
        scallop(m, tips[3], b, wr);
        m.closePath();
        g.setComposite(AlphaComposite.SrcOver.derive(far ? 0.78f : 1f));
        g.setPaint(new GradientPaint(0, 0, WING_A, (float) (-s * 0.9), (float) -s, WING_B));
        g.fill(m);
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(EDGE);
        g.draw(m);

        g.setColor(BONE);
        g.setStroke(new BasicStroke((float) Math.max(1.1, s * 0.013), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (double[] t : tips) {
            g.draw(new Line2D.Double(wr[0], wr[1], t[0], t[1]));
        }
        g.setStroke(new BasicStroke((float) Math.max(1, s * 0.009), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(wr[0], wr[1], b[0], b[1]));
        g.setColor(new Color(0xc4b5fd));
        g.setStroke(new BasicStroke((float) Math.max(2, s * 0.03), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(0, 0, wr[0], wr[1]));
        g.setColor(BONE);
        double jr = Math.max(2, s * 0.022);
        g.fill(new Ellipse2D.Double(wr[0] - jr, wr[1] - jr, jr * 2, jr * 2));
        Path2D claw = new Path2D.Double();
        claw.moveTo(wr[0] + s * 0.01, wr[1] - s * 0.01);
        claw.quadTo(wr[0] + s * 0.14, wr[1] - s * 0.05, wr[0] + s * 0.12, wr[1] - s * 0.17);
        claw.quadTo(wr[0] + s * 0.08, wr[1] - s * 0.08, wr[0] - s * 0.02, wr[1] - s * 0.03);
        claw.closePath();
        g.fill(claw);
        g.dispose();
    }

    private static void scallop(Path2D path, double[] p, double[] q, double[] wrist) {
        double mx = (p[0] + q[0]) / 2;
        double my = (p[1] + q[1]) / 2;
        path.quadTo(mx + (wrist[0] - mx) * 0.25, my + (wrist[1] - my) * 0.25, q[0], q[1]);
    }

    private void drawLeg(Graphics2D g0, int i, double side, boolean far) {
        Graphics2D g = (Graphics2D) g0.create();
        double ang = Math.atan2(qfy[i], qfx[i]);
        double r = radius[i];
        double sc = far ? 0.88 : 1;
        double sw = Math.sin(time * 2.3 + i) * 0.18 * len;
        g.translate(qx[i] + (far ? qfx[i] * len * 0.7 : 0), qy[i] + (far ? qfy[i] * len * 0.7 : 0));
        g.rotate(ang);
        g.scale(1, safe(side));
        if (far) {
            g.setComposite(AlphaComposite.SrcOver.derive(0.72f));
        }
        double[] a = {0.2 * len, r * 0.3};
        double[] k = {-0.9 * len * sc + sw, r * 0.3 + 1.05 * len * sc};
        double[] f = {0.45 * len * sc + sw, r * 0.3 + 1.95 * len * sc};
        seg(g, a[0], a[1], k[0], k[1], r * 0.85 * sc + 3, RIM);
        seg(g, a[0], a[1], k[0], k[1], r * 0.85 * sc, bodyColor[i]);
        seg(g, k[0], k[1], f[0], f[1], r * 0.55 * sc + 3, RIM);
        seg(g, k[0], k[1], f[0], f[1], r * 0.55 * sc, bodyColor[i]);
        double[][] claws = {{0.55, 0.15}, {0.5, 0.5}, {0.05, 0.68}};
        for (double[] c : claws) {
            seg(g, f[0], f[1], f[0] + c[0] * len, f[1] + c[1] * len, Math.max(1.3, len * 0.13), BONE);
        }
        g.dispose();
    }

    private static Path2D blade(double x0, double y0, double tx, double ty, double wid) {
        double mx = (x0 + tx) / 2;
        double my = (y0 + ty) / 2;
        Path2D p = new Path2D.Double();
        p.moveTo(x0, y0 - wid);
        p.quadTo(mx, my - wid * 0.3, tx, ty);
        p.quadTo(mx, my + wid * 0.8, x0, y0 + wid);
        p.closePath();
        return p;
    }

    private static Path2D horn(double u) {
        Path2D p = new Path2D.Double();
        p.moveTo(-0.2 * u, -1.0 * u);
        p.curveTo(-1.4 * u, -2.1 * u, -2.8 * u, -2.4 * u, -4.0 * u, -1.6 * u);
        p.curveTo(-2.9 * u, -1.9 * u, -1.7 * u, -1.6 * u, -0.9 * u, -0.6 * u);
        p.closePath();
        return p;
    }

    private void drawHead(Graphics2D g0) {
        Graphics2D g = (Graphics2D) g0.create();
        double u = maxR * 1.15;
        double flip = safe(headFlip);
        g.translate(px[0], py[0]);
        g.rotate(headA);
        g.scale(1, flip);

        double lx = 0;
        double ly = 0;
        if (pointer != null) {
            double vx = pointer.x - px[0];
            double vy = pointer.y - py[0];
            double c = Math.cos(headA);
            double s = Math.sin(headA);
            double ax = vx * c + vy * s;
            double ay = (-vx * s + vy * c) * flip;
            double l = Math.hypot(ax, ay);
            if (l == 0) {
                l = 1;
            }
            lx = ax / l;
            ly = ay / l;
        }
        double wv = Math.sin(time * 3.6) * 0.25 * u;

        // leques da nuca
        double[][] fins = {{-0.9, 0.0, -3.0, 1.0, 0.28}, {-0.9, -0.25, -3.2, 0.05, 0.26}, {-0.8, 0.3, -2.6, 1.7, 0.22}};
        g.setStroke(new BasicStroke(1f));
        for (double[] f : fins) {
            Path2D b = blade(f[0] * u, f[1] * u, f[2] * u, f[3] * u + wv, f[4] * u);
            g.setColor(FIN);
            g.fill(b);
            g.setColor(EDGE);
            g.draw(b);
        }

        // chifres
        g.setColor(BONE);
        AffineTransform keep = g.getTransform();
        g.setComposite(AlphaComposite.SrcOver.derive(0.55f));
        g.translate(-0.3 * u, 0.25 * u);
        g.scale(0.85, 0.85);
        g.fill(horn(u));
        g.setTransform(keep);
        g.setComposite(AlphaComposite.SrcOver);
        g.fill(horn(u));
        g.setColor(new Color(90, 70, 170, 128));
        double[][] rings = {{-1.3, -1.75}, {-2.0, -2.0}, {-2.7, -2.0}};
        for (double[] r : rings) {
            g.draw(new Line2D.Double(r[0] * u, (r[1] - 0.2) * u, (r[0] + 0.05) * u, (r[1] + 0.22) * u));
        }

        // bigodes
        g.setComposite(AlphaComposite.SrcOver.derive(0.7f));
        g.setColor(EYE);
        g.setStroke(new BasicStroke(1.3f));
        Path2D w1 = new Path2D.Double();
        w1.moveTo(2.5 * u, -0.1 * u);
        w1.curveTo(1.6 * u, -1.6 * u - wv, -0.6 * u, -2.2 * u + wv, -2.6 * u, -1.2 * u - wv);
        g.draw(w1);
        Path2D w2 = new Path2D.Double();
        w2.moveTo(2.4 * u, 0.2 * u);
        w2.curveTo(1.4 * u, 1.8 * u + wv, -0.8 * u, 2.2 * u - wv, -2.8 * u, 1.4 * u + wv);
        g.draw(w2);
        g.setComposite(AlphaComposite.SrcOver);

        // interior da boca (com brilho quando ruge)
        double hx = -0.6 * u;
        double hy = 0.25 * u;
        double cj = Math.cos(jaw);
        double sj = Math.sin(jaw);
        if (jaw > 0.07) {
            double lx0 = 2.4 * u - hx;
            double ly0 = 0.12 * u - hy;
            Path2D mouth = new Path2D.Double();
            mouth.moveTo(hx, hy);
            mouth.lineTo(2.7 * u, 0.1 * u);
            mouth.lineTo(hx + lx0 * cj - ly0 * sj, hy + lx0 * sj + ly0 * cj);
            mouth.closePath();
            g.setColor(MOUTH);
            g.fill(mouth);
            if (roarT < 0.85) {
                g.setPaint(new RadialGradientPaint((float) (1.2 * u), (float) (0.5 * u), (float) (1.6 * u),
                        new float[]{0f, 1f}, new Color[]{new Color(98, 230, 255, 217), EYE_CLEAR}));
                g.fill(mouth);
            }
        }

        // mandíbula inferior
        AffineTransform beforeJaw = g.getTransform();
        g.translate(hx, hy);
        g.rotate(jaw);
        g.translate(-hx, -hy);
        Path2D lower = new Path2D.Double();
        lower.moveTo(-1.0 * u, 0.3 * u);
        lower.lineTo(2.4 * u, 0.12 * u);
        lower.quadTo(2.5 * u, 0.4 * u, 2.0 * u, 0.55 * u);
        lower.quadTo(0.6 * u, 1.15 * u, -1.0 * u, 0.95 * u);
        lower.closePath();
        g.setColor(bodyColor[2]);
        g.fill(lower);
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(RIM);
        g.draw(lower);
        g.setColor(SPINE);
        for (double x : new double[]{0.1, 0.8}) {
            Path2D sp = new Path2D.Double();
            sp.moveTo((x + 0.35) * u, 0.98 * u);
            sp.lineTo((x - 0.25) * u, 1.65 * u);
            sp.lineTo((x - 0.05) * u, 0.95 * u);
            sp.closePath();
            g.fill(sp);
        }
        g.setColor(BONE);
        for (double x : new double[]{1.2, 1.7, 2.15}) {
            Path2D t = new Path2D.Double();
            t.moveTo((x - 0.13) * u, 0.2 * u);
            t.lineTo((x + 0.13) * u, 0.2 * u);
            t.lineTo(x * u, -0.25 * u);
            t.closePath();
            g.fill(t);
        }
        g.setTransform(beforeJaw);

        // crânio e focinho
        Path2D skull = new Path2D.Double();
        skull.moveTo(-1.2 * u, -0.2 * u);
        skull.curveTo(-1.2 * u, -1.3 * u, 0.6 * u, -1.5 * u, 1.4 * u, -0.95 * u);
        skull.quadTo(2.2 * u, -0.85 * u, 2.9 * u, -0.35 * u);
        skull.quadTo(3.0 * u, 0.0, 2.7 * u, 0.1 * u);
        skull.lineTo(0.2 * u, 0.22 * u);
        skull.lineTo(-1.2 * u, 0.35 * u);
        skull.closePath();
        g.setColor(bodyColor[0]);
        g.fill(skull);
        g.setColor(RIM);
        g.draw(skull);
        // brilho no focinho e escamas da bochecha
        g.setStroke(new BasicStroke(1.4f));
        g.setColor(new Color(255, 255, 255, 77));
        Path2D shine = new Path2D.Double();
        shine.moveTo(-0.6 * u, -1.0 * u);
        shine.curveTo(0.2 * u, -1.3 * u, 1.0 * u, -1.15 * u, 2.6 * u, -0.45 * u);
        g.draw(shine);
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(255, 255, 255, 56));
        for (int k = 0; k < 3; k++) {
            double cx = (-0.4 + k * 0.5) * u;
            double rr = 0.22 * u;
            g.draw(new java.awt.geom.Arc2D.Double(cx - rr, -0.1 * u - rr, rr * 2, rr * 2, -18, -144, java.awt.geom.Arc2D.OPEN));
        }
        // narina
        AffineTransform keepNose = g.getTransform();
        g.translate(2.45 * u, -0.42 * u);
        g.rotate(-0.4);
        g.setColor(RIM);
        g.fill(new Ellipse2D.Double(-0.1 * u, -0.055 * u, 0.2 * u, 0.11 * u));
        g.setTransform(keepNose);
        // presas superiores
        g.setColor(BONE);
        double[][] fangs = {{1.0, 0.45}, {1.6, 0.5}, {2.35, 0.85}};
        for (double[] f : fangs) {
            Path2D t = new Path2D.Double();
            t.moveTo((f[0] - 0.16) * u, 0.18 * u);
            t.lineTo((f[0] + 0.16) * u, 0.16 * u);
            t.lineTo(f[0] * u, (0.18 + f[1]) * u);
            t.closePath();
            g.fill(t);
        }

        // olho
        double ex = 0.65 * u;
        double ey = -0.5 * u;
        g.setComposite(AlphaComposite.SrcOver.derive(0.45f));
        g.setPaint(new RadialGradientPaint((float) ex, (float) ey, (float) (0.95 * u),
                new float[]{0f, 1f}, new Color[]{EYE, EYE_CLEAR}));
        g.fill(new Ellipse2D.Double(ex - 0.95 * u, ey - 0.95 * u, 1.9 * u, 1.9 * u));
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(EYE);
        g.fill(new Ellipse2D.Double(ex - 0.27 * u, ey - 0.27 * u, 0.54 * u, 0.54 * u));
        g.setColor(new Color(0x0a0a24));
        g.fill(new Ellipse2D.Double(ex + lx * 0.09 * u - 0.06 * u, ey + ly * 0.09 * u - 0.19 * u, 0.12 * u, 0.38 * u));
        // pálpebra inclinada (olhar feroz)
        Path2D lid = new Path2D.Double();
        lid.moveTo(0.2 * u, -0.78 * u);
        lid.lineTo(1.12 * u, -0.46 * u);
        lid.lineTo(1.12 * u, -1.0 * u);
        lid.lineTo(0.2 * u, -1.0 * u);
        lid.closePath();
        g.setColor(bodyColor[0]);
        g.fill(lid);
        g.setStroke(new BasicStroke(1.2f));
        g.setColor(RIM);
        g.draw(new Line2D.Double(0.2 * u, -0.78 * u, 1.12 * u, -0.46 * u));
        g.dispose();
    }

    private void drawParticles(Graphics2D g) {
        for (Particle p : particles) {
            double t = 1 - p.life / p.max;
            int r = (int) Math.round(mix(110, 165, t));
            int gr = (int) Math.round(mix(230, 100, t));
            int a = (int) Math.round(clamp((1 - t) * 0.75, 0, 1) * 255);
            g.setColor(new Color(r, gr, 255, a));
            double rad = p.size * (0.6 + t * 1.4);
            g.fill(new Ellipse2D.Double(p.x - rad, p.y - rad, rad * 2, rad * 2));
        }
    }

    // ------------------------------------------------------------------ utilidades

    /** Evita escala zero (matriz não invertível) quando o dragão "vira" de lado. */
    private static double safe(double flip) {
        if (Math.abs(flip) < 0.06) {
            return flip < 0 ? -0.06 : 0.06;
        }
        return flip;
    }

    private static double clamp(double v, double a, double b) {
        return Math.max(a, Math.min(b, v));
    }

    private static double mix(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
