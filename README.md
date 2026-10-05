# Quiz de Segurança Digital (versão 100% Java)

Aplicativo desktop em **Java puro (Swing/Java2D)**, sem nenhuma biblioteca externa, com as mesmas funções do quiz para web.

## Funções

- 20 perguntas em 5 etapas de 4 perguntas, cada uma com 4 alternativas e uma única resposta correta (nível básico a intermediário).
- Barra de progresso, botão Voltar, e o botão Próxima só libera depois de escolher uma alternativa.
- Resultado final com:
  - pontuação e avaliação;
  - **gráfico de pizza geral** (acertos e erros, com quantidade e porcentagem);
  - **uma pizza por etapa**, com acertos, erros e porcentagens;
  - detalhamento de **cada pergunta errada**: sua resposta, a resposta certa e a explicação.
- Campo de nome opcional, **ranking** (top 10), **histórico** das últimas tentativas e **perguntas mais erradas** de todas as tentativas, guardados no arquivo `~/.quiz-seguranca-digital/historico.csv`.
- Visual em azul e roxo e o **dragão voador**: corpo articulado, asas que batem, patas, cauda com barbatana, olhos que seguem o mouse e sopro de faíscas a cada nova pergunta. Dá para desligá-lo na caixa "Dragão animado".
- Layout que se adapta à largura da janela.

## Jogar pelo navegador (com link)

O `index.html` roda o mesmo programa Java dentro da página, usando o [CheerpJ](https://cheerpj.com) (uma máquina virtual Java em WebAssembly). Funciona no GitHub Pages:

1. No repositório, abra **Settings > Pages**.
2. Em **Branch**, escolha `main` e `/ (root)`, e clique em **Save**.
3. Depois de 1 a 2 minutos o quiz fica em `https://SEU-USUARIO.github.io/NOME-DO-REPOSITORIO/`.

Observações: o `index.html` e o `quiz-seguranca-digital.jar` precisam ficar na mesma pasta; no navegador o ranking e o histórico ficam salvos no próprio navegador; o CheerpJ é gratuito para uso pessoal e não comercial.

## Como jogar no computador

Requisito: Java 11 ou superior.

```bash
java -jar quiz-seguranca-digital.jar
```

Em muitos sistemas basta dar dois cliques no arquivo `.jar`.

## Como compilar

```bash
./build.sh          # Linux/macOS
build.bat           # Windows
```

Ou manualmente:

```bash
javac -encoding UTF-8 --release 11 -d out *.java
java -cp out Main
```

## Testes

Testes de autoverificação, sem bibliotecas (89 verificações: banco de perguntas, correção, percentuais, ranking, histórico e estatísticas):

```bash
java -cp quiz-seguranca-digital.jar SelfTest
```

## Arquivos

| Arquivo | Função |
|---|---|
| `Main.java` | Janela principal |
| `QuizApp.java` | Telas (abertura, perguntas, resultado) e animação |
| `Dragon.java` | Dragão voador desenhado com Java2D |
| `Ui.java` | Componentes visuais: layout, cartões, opções, botões, gráficos de pizza |
| `QuizEngine.java` | Correção, percentuais e detalhamento dos erros |
| `HistoryStore.java` | Histórico, ranking e estatísticas em arquivo |
| `QuestionBank.java` | As 20 perguntas, com explicações |
| `Question.java` | Modelo de pergunta |
| `Theme.java` | Cores e fontes |
| `SelfTest.java` | Testes |

Para mudar as perguntas, edite `QuestionBank.java`: cada `Question` recebe a etapa, o enunciado, as 4 alternativas, o índice da correta (0 a 3) e a explicação.

## Licença

MIT
