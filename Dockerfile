# Usa somente o ambiente de execução do Java 21, suficiente para iniciar o JAR.
FROM eclipse-temurin:21-jre

# Define a pasta padrão dos próximos comandos dentro da imagem.
WORKDIR /app

# Copia o artefato produzido pelo Maven e lhe atribui um nome simples.
COPY target/ProjetoSpringBoot-0.0.1-SNAPSHOT.jar app.jar

# Documenta a porta HTTP utilizada pela aplicação.
EXPOSE 8080

# Inicia a aplicação quando o container é executado.
ENTRYPOINT ["java", "-jar", "app.jar"]
