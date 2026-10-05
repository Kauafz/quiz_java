import java.util.List;

/** Uma pergunta de múltipla escolha: o índice {@code correct} aponta a única alternativa certa. */
record Question(int part, String text, List<String> options, int correct, String explanation) {

    Question {
        if (options.size() != 4) {
            throw new IllegalArgumentException("Cada pergunta precisa de 4 alternativas");
        }
        if (correct < 0 || correct >= options.size()) {
            throw new IllegalArgumentException("Alternativa correta fora do intervalo");
        }
        options = List.copyOf(options);
    }
}
