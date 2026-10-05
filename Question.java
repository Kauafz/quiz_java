import java.util.List;

/** Uma pergunta de múltipla escolha: o índice {@code correct} aponta a única alternativa certa. */
final class Question {

    private final int part;
    private final String text;
    private final List<String> options;
    private final int correct;
    private final String explanation;

    Question(int part, String text, List<String> options, int correct, String explanation) {
        if (options.size() != 4) {
            throw new IllegalArgumentException("Cada pergunta precisa de 4 alternativas");
        }
        if (correct < 0 || correct >= options.size()) {
            throw new IllegalArgumentException("Alternativa correta fora do intervalo");
        }
        this.part = part;
        this.text = text;
        this.options = List.copyOf(options);
        this.correct = correct;
        this.explanation = explanation;
    }

    int part() { return part; }
    String text() { return text; }
    List<String> options() { return options; }
    int correct() { return correct; }
    String explanation() { return explanation; }
}
