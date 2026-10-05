import java.util.ArrayList;
import java.util.List;

/** Regras do quiz: corrige as respostas e calcula acertos, erros, percentuais e o detalhamento dos erros. */
final class QuizEngine {

    private QuizEngine() {
    }

    record PartResult(String name, int hits, int total) {
        int misses() {
            return total - hits;
        }

        double percentHits() {
            return percent(hits, total);
        }
    }

    record Miss(int questionIndex, int part, String partName, int position, String text,
                int chosen, String chosenText, int correct, String correctText, String explanation) {
    }

    record Result(int hits, int total, List<PartResult> parts, List<Miss> misses, boolean[] correct) {
        int errors() {
            return total - hits;
        }

        double percentHits() {
            return percent(hits, total);
        }

        double percentErrors() {
            return percent(errors(), total);
        }
    }

    /** @param answers índice da alternativa escolhida para cada pergunta (0 a 3) */
    static Result grade(List<Question> questions, List<String> partNames, int[] answers) {
        if (answers.length != questions.size()) {
            throw new IllegalArgumentException("Responda todas as " + questions.size() + " perguntas.");
        }
        int[] partHits = new int[partNames.size()];
        int[] partTotal = new int[partNames.size()];
        int[] positions = new int[partNames.size()];
        boolean[] correct = new boolean[questions.size()];
        List<Miss> misses = new ArrayList<>();
        int hits = 0;

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            int pick = answers[i];
            if (pick < 0 || pick >= q.options().size()) {
                throw new IllegalArgumentException("Alternativa inválida na pergunta " + (i + 1));
            }
            positions[q.part()]++;
            partTotal[q.part()]++;
            correct[i] = pick == q.correct();
            if (correct[i]) {
                hits++;
                partHits[q.part()]++;
            } else {
                misses.add(new Miss(i, q.part(), partNames.get(q.part()), positions[q.part()], q.text(),
                        pick, q.options().get(pick), q.correct(), q.options().get(q.correct()), q.explanation()));
            }
        }
        List<PartResult> parts = new ArrayList<>();
        for (int p = 0; p < partNames.size(); p++) {
            parts.add(new PartResult(partNames.get(p), partHits[p], partTotal[p]));
        }
        return new Result(hits, questions.size(), parts, misses, correct);
    }

    static double percent(int part, int total) {
        return total == 0 ? 0.0 : Math.round(part * 1000.0 / total) / 10.0;
    }

    static String verdict(double percentHits) {
        if (percentHits >= 100) {
            return "Desempenho perfeito! Você domina os fundamentos e o nível intermediário de segurança digital.";
        } else if (percentHits >= 80) {
            return "Muito bom! Você tem uma base sólida e só precisa ajustar alguns detalhes.";
        } else if (percentHits >= 60) {
            return "Bom começo. Você conhece o básico, mas vale revisar os pontos que errou abaixo.";
        } else if (percentHits >= 40) {
            return "Ainda há lacunas importantes. Leia as explicações abaixo com calma e tente de novo.";
        }
        return "Tem bastante coisa para reforçar, e tudo bem. As explicações abaixo são um ótimo ponto de partida.";
    }
}
