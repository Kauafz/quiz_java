import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Testes de autoverificação sem dependências: java -cp quiz-seguranca-digital.jar SelfTest */
public final class SelfTest {

    private static int checks;

    private SelfTest() {
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError("FALHOU: " + message);
        }
    }

    private static int[] answers(int wrongEvery) {
        int[] a = new int[QuestionBank.ALL.size()];
        for (int i = 0; i < a.length; i++) {
            int correct = QuestionBank.ALL.get(i).correct();
            a[i] = wrongEvery > 0 && i % wrongEvery == 0 ? (correct + 1) % 4 : correct;
        }
        return a;
    }

    public static void main(String[] args) throws Exception {
        List<Question> qs = QuestionBank.ALL;
        List<String> parts = QuestionBank.PARTS;

        // banco de perguntas
        check(qs.size() == 20, "20 perguntas");
        check(parts.size() == 5, "5 etapas");
        for (int p = 0; p < parts.size(); p++) {
            final int part = p;
            check(qs.stream().filter(q -> q.part() == part).count() == 4, "4 perguntas na etapa " + (p + 1));
        }
        for (Question q : qs) {
            check(q.options().size() == 4, "4 alternativas");
            check(q.options().stream().distinct().count() == 4, "alternativas distintas: " + q.text());
            check(!q.explanation().isBlank(), "explicação preenchida");
        }

        // correção: tudo certo
        QuizEngine.Result all = QuizEngine.grade(qs, parts, answers(0));
        check(all.hits() == 20 && all.errors() == 0, "20 acertos");
        check(all.percentHits() == 100.0 && all.percentErrors() == 0.0, "100%");
        check(all.misses().isEmpty(), "sem erros detalhados");
        check(all.parts().stream().allMatch(p -> p.hits() == 4 && p.total() == 4), "4/4 em cada etapa");

        // correção: erra uma a cada 4 (índices 0, 4, 8, 12, 16 = 1ª pergunta de cada etapa)
        QuizEngine.Result some = QuizEngine.grade(qs, parts, answers(4));
        check(some.hits() == 15 && some.errors() == 5, "15 acertos e 5 erros");
        check(some.percentHits() == 75.0 && some.percentErrors() == 25.0, "75% e 25%");
        check(some.misses().size() == 5, "5 questões detalhadas");
        check(some.misses().stream().allMatch(m -> m.position() == 1), "todas eram a 1ª da etapa");
        check(some.misses().stream().noneMatch(m -> m.chosen() == m.correct()), "escolha diferente da correta");
        check(some.parts().stream().allMatch(p -> p.hits() == 3 && p.misses() == 1), "3 acertos por etapa");

        // correção: tudo errado
        int[] wrong = new int[qs.size()];
        for (int i = 0; i < wrong.length; i++) {
            wrong[i] = (qs.get(i).correct() + 1) % 4;
        }
        QuizEngine.Result none = QuizEngine.grade(qs, parts, wrong);
        check(none.hits() == 0 && none.percentErrors() == 100.0, "0 acertos");

        // entradas inválidas
        boolean rejected = false;
        try {
            QuizEngine.grade(qs, parts, new int[5]);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check(rejected, "rejeita quantidade errada de respostas");
        rejected = false;
        try {
            int[] bad = answers(0);
            bad[3] = 9;
            QuizEngine.grade(qs, parts, bad);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check(rejected, "rejeita alternativa inválida");

        // histórico, ranking e estatísticas
        Path dir = Files.createTempDirectory("quiz-test");
        HistoryStore store = new HistoryStore(dir);
        check(store.all().isEmpty(), "histórico começa vazio");
        store.save("Ana", all);
        Thread.sleep(3);
        store.save("Bruno;Silva", some);
        Thread.sleep(3);
        store.save("", none);
        Thread.sleep(3);
        store.save("Carla", some);
        check(store.all().size() == 4, "4 tentativas salvas");
        List<HistoryStore.Attempt> ranking = store.ranking(10);
        check(ranking.size() == 3, "ranking ignora quem não informou nome");
        check(ranking.get(0).name().equals("Ana") && ranking.get(0).hits() == 20, "Ana em 1º");
        check(ranking.get(1).name().startsWith("Bruno") && !ranking.get(1).name().contains(";"), "Bruno em 2º, nome limpo");
        check(ranking.get(2).name().equals("Carla"), "empate: quem fez primeiro vem antes");
        check(store.recent(2).get(0).name().equals("Carla"), "mais recente primeiro");
        int[] counts = store.wrongCounts(qs.size());
        check(counts[0] == 3 && counts[1] == 1, "contagem de erros por pergunta: " + Arrays.toString(counts));
        check(new HistoryStore(dir).all().size() == 4, "dados persistem entre execuções");

        System.out.println("OK: " + checks + " verificações passaram.");
    }
}
