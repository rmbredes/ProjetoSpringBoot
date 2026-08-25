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
    }
}