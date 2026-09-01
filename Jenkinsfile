// Define uma pipeline declarativa executada pelo Jenkins.
pipeline {

    // Seleciona um agente Windows que possua Java 21.
    agent {
        label 'windows && java21'
    }

    // Solicita ao Jenkins a instalação configurada do JDK 21.
    tools {
        jdk 'JDK21'
    }

    // Agrupa as etapas executadas em sequência.
    stages {
/*
        Versão antiga do checkout mantida apenas como referência.
        stage('Checkout') {

            steps {

                deleteDir()

                git branch: 'master',
                    credentialsId: 'github-projetospringboot',
                    url: 'https://github.com/rmbredes/ProjetoSpringBoot.git'
            }
        }
*/

        // Obtém do controle de versão o mesmo repositório que iniciou a pipeline.
        stage('Checkout') {

            steps {

                // Limpa arquivos deixados por uma execução anterior.
                deleteDir()
                // Baixa o código usando a configuração SCM do job.
                checkout scm
            }
        }
        // Confirma as versões e variáveis do Java disponíveis no agente.
        stage('Teste Java') {

            steps {

                // Exibe a versão do executável Java.
                bat 'java -version'
                // Exibe o diretório do JDK escolhido pelo Jenkins.
                bat 'echo JAVA_HOME=%JAVA_HOME%'
            }
        }

        // Executa a suíte automatizada antes de gerar qualquer artefato.
        stage('Testes') {

            steps {

                // Usa o Maven Wrapper versionado pelo projeto.
                bat 'call mvnw.cmd test'
            }
        }

        // Limpa resultados anteriores e produz o JAR executável.
        stage('Package') {

            steps {

                // O comando também repete os testes como proteção do empacotamento.
                bat 'call mvnw.cmd clean package'
            }
        }
        // Confirma que Docker e Compose estão acessíveis no agente.
        stage('Teste Docker') {

            steps {
                // Exibe a versão do cliente Docker.
                bat 'docker --version'
                // Exibe a versão do plugin Docker Compose.
                bat 'docker compose version'
                // Confirma a comunicação completa entre cliente e servidor Docker.
                bat 'docker version'
            }
        }

        // Constrói e identifica a imagem da aplicação.
        stage('Build Docker') {

            steps {
                // Cria uma tag imutável da execução e atualiza a tag local latest.
                bat '''
                    docker build ^
                        -t projeto-springboot:%BUILD_NUMBER% ^
                        -t projeto-springboot:latest ^
                        .
                '''

                // Confirma que a imagem numerada foi realmente criada.
                bat 'docker image inspect projeto-springboot:%BUILD_NUMBER%'
            }
        }
        // Atualiza os containers descritos no Compose.
        stage('Deploy Docker') {

            steps {
                // Inicia os serviços e aguarda até que fiquem prontos ou atinjam o limite.
                bat '''
                    docker compose ^
                        -p projetospringboot ^
                        up -d ^
                        --no-build ^
                        --wait ^
                        --wait-timeout 120
                '''

                // Exibe o estado final de cada container do projeto.
                bat 'docker compose -p projetospringboot ps'
            }
        }
        // Realiza uma chamada HTTP para confirmar que a aplicação responde.
        stage('Validar Aplicacao') {

            steps {
                // Repete a chamada durante a inicialização e exibe cabeçalhos e corpo.
                bat '''
                    curl.exe ^
                        --retry 12 ^
                        --retry-delay 5 ^
                        --retry-all-errors ^
                        -i ^
                        http://localhost:8080/ProjetoSpringBoot/clientes
                '''
            }
        }
        // Remove tags numeradas antigas que não estão mais em uso.
        stage('Limpar Imagens Antigas') {

            steps {
                // Preserva as tags latest e a tag gerada pela execução atual.
                bat '''
                    for /f "delims=" %%i in ('docker image ls projeto-springboot --format "{{.Repository}}:{{.Tag}}"') do (
                        if /I not "%%i"=="projeto-springboot:latest" (
                            if /I not "%%i"=="projeto-springboot:%BUILD_NUMBER%" (
                                docker image rm "%%i" || echo Imagem em uso, mantida: %%i
                            )
                        )
                    )
                '''
            }
        }
    }
}
