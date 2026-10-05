#!/usr/bin/env bash
# Compila o projeto e gera o JAR executável (requer JDK 17 ou superior)
set -e
rm -rf out && mkdir out
javac -encoding UTF-8 --release 17 -d out *.java
jar --create --file quiz-seguranca-digital.jar --main-class Main -C out .
echo "Pronto: quiz-seguranca-digital.jar"
echo "Para jogar: java -jar quiz-seguranca-digital.jar"
