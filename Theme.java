import java.awt.Color;
import java.awt.Font;

/** Cores e fontes do quiz (azul e roxo). */
final class Theme {

    private Theme() {
    }

    static final Color BG1 = new Color(0x0a0f2c);
    static final Color BG2 = new Color(0x140f36);
    static final Color BG3 = new Color(0x1e1148);

    static final Color CARD = new Color(17, 21, 56, 205);
    static final Color LINE = new Color(150, 160, 255, 70);
    static final Color INK = new Color(0xeef0ff);
    static final Color MUTED = new Color(0xa6abd8);
    static final Color BLUE = new Color(0x5b9bff);
    static final Color PURPLE = new Color(0xa06bff);
    static final Color GRAD_A = new Color(0x4f7dff);
    static final Color GRAD_B = new Color(0x9a5cff);

    static final Color OK = new Color(0x62a8ff);
    static final Color BAD = new Color(0xc68bff);
    static final Color OK_SOFT = new Color(98, 168, 255, 41);
    static final Color BAD_SOFT = new Color(198, 139, 255, 41);
    static final Color PICK = new Color(160, 107, 255, 46);

    static Font font(int style, float size) {
        return new Font("SansSerif", style, 12).deriveFont(style, size);
    }
}
