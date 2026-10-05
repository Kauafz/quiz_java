import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Histórico, ranking e estatísticas, guardados em um arquivo de texto na pasta do usuário.
 * Formato de cada linha: milissegundos;nome;acertos;total;respostas (1 = acertou, 0 = errou).
 */
final class HistoryStore {

    record Attempt(long time, String name, int hits, int total, String bits) {
        boolean named() {
            return !name.isBlank();
        }
    }

    private final Path file;

    HistoryStore(Path directory) {
        this.file = directory.resolve("historico.csv");
    }

    static HistoryStore defaultStore() {
        String dir = System.getProperty("quiz.dir");
        Path base = dir != null ? Path.of(dir) : Path.of(System.getProperty("user.home"), ".quiz-seguranca-digital");
        return new HistoryStore(base);
    }

    synchronized boolean save(String playerName, QuizEngine.Result result) {
        StringBuilder bits = new StringBuilder();
        for (boolean ok : result.correct()) {
            bits.append(ok ? '1' : '0');
        }
        String line = System.currentTimeMillis() + ";" + clean(playerName) + ";" + result.hits() + ";"
                + result.total() + ";" + bits + "\n";
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            System.err.println("Não foi possível salvar o histórico: " + e.getMessage());
            return false;
        }
    }

    synchronized List<Attempt> all() {
        List<Attempt> list = new ArrayList<>();
        if (!Files.exists(file)) {
            return list;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] f = line.split(";", -1);
                if (f.length != 5) {
                    continue;
                }
                try {
                    list.add(new Attempt(Long.parseLong(f[0]), f[1], Integer.parseInt(f[2]),
                            Integer.parseInt(f[3]), f[4]));
                } catch (NumberFormatException ignored) {
                    // linha corrompida: ignora
                }
            }
        } catch (IOException e) {
            System.err.println("Não foi possível ler o histórico: " + e.getMessage());
        }
        return list;
    }

    /** Melhores resultados de quem informou o nome (empate: quem fez primeiro). */
    List<Attempt> ranking(int limit) {
        return all().stream()
                .filter(Attempt::named)
                .sorted(Comparator.comparingInt(Attempt::hits).reversed().thenComparingLong(Attempt::time))
                .limit(limit)
                .toList();
    }

    /** Últimas tentativas, da mais recente para a mais antiga. */
    List<Attempt> recent(int limit) {
        return all().stream()
                .sorted(Comparator.comparingLong(Attempt::time).reversed())
                .limit(limit)
                .toList();
    }

    /** Quantas vezes cada pergunta foi errada, somando todas as tentativas. */
    int[] wrongCounts(int questionCount) {
        int[] wrong = new int[questionCount];
        for (Attempt a : all()) {
            if (a.bits().length() != questionCount) {
                continue;
            }
            for (int i = 0; i < questionCount; i++) {
                if (a.bits().charAt(i) == '0') {
                    wrong[i]++;
                }
            }
        }
        return wrong;
    }

    private static String clean(String name) {
        if (name == null) {
            return "";
        }
        String n = name.strip().replaceAll("[;\\p{Cntrl}]", " ");
        return n.length() > 40 ? n.substring(0, 40) : n;
    }
}
