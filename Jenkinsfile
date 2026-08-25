pipeline {

    agent {
        label 'windows && java21'
    }

    tools {
        jdk 'JDK21'
    }

    stages {
/*
        stage('Checkout') {

            steps {

                deleteDir()

                git branch: 'master',
                    credentialsId: 'github-projetospringboot',
                    url: 'https://github.com/rmbredes/ProjetoSpringBoot.git'
            }
        }
*/

        stage('Checkout') {

            steps {

                deleteDir()
                checkout scm
            }
        }
        stage('Teste Java') {

            steps {

                bat 'java -version'
                bat 'echo JAVA_HOME=%JAVA_HOME%'
            }
        }

        stage('Testes') {

            steps {

                bat 'call mvnw.cmd test'
            }
        }

        stage('Package') {

            steps {

                bat 'call mvnw.cmd clean package'
            }
        }
        stage('Teste Docker') {

            steps {
                bat 'docker --version'
                bat 'docker compose version'
                bat 'docker version'
            }
        }

        stage('Build Docker') {

            steps {
                bat '''
                    docker build ^
                        -t projeto-springboot:%BUILD_NUMBER% ^
                        -t projeto-springboot:latest ^
                        .
                '''

                bat 'docker image inspect projeto-springboot:%BUILD_NUMBER%'
            }
        }
        stage('Deploy Docker') {

            steps {
                bat '''
                    docker compose ^
                        -p projetospringboot ^
                        up -d ^
                        --no-build ^
                        --wait ^
                        --wait-timeout 120
                '''

                bat 'docker compose -p projetospringboot ps'
            }
        }
        stage('Validar Aplicacao') {

            steps {
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
        stage('Limpar Imagens Antigas') {

            steps {
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