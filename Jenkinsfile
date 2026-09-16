// Pipeline do ProjetoSpringBoot executada pelo Jenkins.
pipeline {

    // Utiliza o agente Windows com Java 21.
    agent {
        label 'windows && java21'
    }

    // Evita o checkout automático.
    // O código será baixado explicitamente no primeiro stage.
    options {
        skipDefaultCheckout(true)
    }

    // Utiliza o JDK 21 cadastrado no Jenkins.
    tools {
        jdk 'JDK21'
    }

    // Configurações compartilhadas pelos stages.
    environment {

        // AWS CLI instalada no perfil do usuário Ricardo.
        AWS_CLI = 'C:\\Users\\Ricardo\\AppData\\Local\\Programs\\Amazon\\AWSCLIV2\\aws.exe'

        // Permite que o Jenkins, executado como LocalSystem,
        // encontre a configuração e a sessão AWS do usuário Ricardo.
        HOME = 'C:\\Users\\Ricardo'
        USERPROFILE = 'C:\\Users\\Ricardo'
        AWS_CONFIG_FILE = 'C:\\Users\\Ricardo\\.aws\\config'

        // Perfil utilizado no laboratório.
        AWS_PROFILE = 'projeto-s3'

        // Região, registro e repositório ECR do monólito.
        AWS_REGION = 'sa-east-1'
        ECR_REGISTRY = '033649548808.dkr.ecr.sa-east-1.amazonaws.com'
        ECR_REPOSITORY = 'recomeco/projeto-springboot'
    }

    stages {

        // Obtém o código configurado no job do Jenkins.
        stage('Checkout') {

            steps {

                // Remove arquivos deixados por execuções anteriores.
                deleteDir()

                // Baixa a master do repositório configurado no job.
                checkout scm
            }
        }

        // Confirma as ferramentas utilizadas pela pipeline.
        stage('Verificar ambiente') {

            steps {

                bat 'java -version'
                bat 'echo JAVA_HOME=%JAVA_HOME%'
                bat 'docker --version'
                bat 'docker compose version'
                bat 'docker version'

                // Interrompe claramente caso a AWS CLI não exista.
                bat '''
                    if not exist "%AWS_CLI%" (
                        echo AWS CLI nao encontrada em: %AWS_CLI%
                        exit /b 1
                    )
                '''

                // Interrompe caso o arquivo de configuração AWS não exista.
                bat '''
                    if not exist "%AWS_CONFIG_FILE%" (
                        echo Configuracao AWS nao encontrada em: %AWS_CONFIG_FILE%
                        exit /b 1
                    )
                '''

                // Utiliza o caminho completo porque a AWS CLI não está
                // no PATH da conta LocalSystem.
                bat '"%AWS_CLI%" --version'
            }
        }

        // Compila, executa os testes e produz o JAR.
        stage('Testar e gerar pacote') {

            steps {

                bat 'call mvnw.cmd clean verify'
            }
        }

        // Constrói e identifica a imagem local da aplicação.
        stage('Construir imagem Docker') {

            steps {

                // A imagem recebe:
                //
                // BUILD_NUMBER: versão desta execução do Jenkins.
                // latest: imagem utilizada pelo Compose local.
                bat '''
                    docker build --pull ^
                        --tag projeto-springboot:%BUILD_NUMBER% ^
                        --tag projeto-springboot:latest ^
                        .
                '''

                // Confirma que a imagem numerada foi criada.
                bat 'docker image inspect projeto-springboot:%BUILD_NUMBER%'
            }
        }

        // Atualiza os containers descritos no Compose do monólito.
        stage('Deploy Docker local') {

            steps {

                // Inicia os serviços e aguarda a conclusão dos healthchecks.
                bat '''
                    docker compose ^
                        -p projetospringboot ^
                        up -d ^
                        --no-build ^
                        --wait ^
                        --wait-timeout 120
                '''

                // Exibe o estado final dos containers.
                bat 'docker compose -p projetospringboot ps'
            }
        }

        // Confirma que a aplicação implantada localmente responde.
        stage('Validar aplicação') {

            steps {

                // Repete a chamada durante a inicialização da aplicação.
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

        // Confirma que o Jenkins consegue utilizar a sessão AWS.
        stage('Validar identidade AWS') {

            steps {

                bat '''
                    "%AWS_CLI%" sts get-caller-identity --profile %AWS_PROFILE%
                '''
            }
        }

        // Autentica o Docker no registro privado do ECR.
        stage('Autenticar no Amazon ECR') {

            steps {

                // A senha temporária segue diretamente da AWS CLI
                // para o Docker e não é gravada em arquivo.
                bat '''
                    "%AWS_CLI%" ecr get-login-password --region %AWS_REGION% --profile %AWS_PROFILE% | docker login --username AWS --password-stdin %ECR_REGISTRY%
                '''
            }
        }

        // Publica no ECR exatamente a imagem que passou pela validação local.
        stage('Publicar imagem no Amazon ECR') {

            steps {

                // As duas tags remotas apontarão para o mesmo digest.
                bat '''
                    docker tag ^
                        projeto-springboot:%BUILD_NUMBER% ^
                        %ECR_REGISTRY%/%ECR_REPOSITORY%:jenkins-latest

                    docker tag ^
                        projeto-springboot:%BUILD_NUMBER% ^
                        %ECR_REGISTRY%/%ECR_REPOSITORY%:jenkins-%BUILD_NUMBER%

                    docker push ^
                        %ECR_REGISTRY%/%ECR_REPOSITORY%:jenkins-latest

                    docker push ^
                        %ECR_REGISTRY%/%ECR_REPOSITORY%:jenkins-%BUILD_NUMBER%
                '''
            }
        }

        // Remove tags numeradas antigas da máquina local.
        stage('Limpar imagens locais antigas') {

            steps {

                // Preserva latest e a versão produzida nesta execução.
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