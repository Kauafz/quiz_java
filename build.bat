@echo off
rem Compila o projeto e gera o JAR executavel (requer JDK 11 ou superior)
if exist out rmdir /s /q out
mkdir out
dir /b *.java > fontes.txt
javac -encoding UTF-8 --release 11 -d out @fontes.txt || exit /b 1
del fontes.txt
jar --create --file quiz-seguranca-digital.jar --main-class Main -C out . || exit /b 1
echo Pronto: quiz-seguranca-digital.jar
echo Para jogar: java -jar quiz-seguranca-digital.jar
