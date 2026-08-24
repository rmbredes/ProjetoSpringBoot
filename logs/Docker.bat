docker run     → cria e inicia container
docker start   → inicia container existente
docker stop    → para container
docker rm      → remove container
docker rmi     → remove imagem
docker ps      → containers ativos
docker ps -a   → todos os containers
docker images  → imagens locais
docker logs    → logs do container



# Comandos Docker, Compose, Redis, PostgreSQL e Maven

> Execute os comandos Docker/Compose normalmente pelo **PowerShell** na raiz do projeto quando indicado.

---

## 1. MAVEN

### Limpar, executar testes, compilar e gerar o `.jar`

```powershell
.\mvnw clean package
```

### Executar somente os testes

```powershell
.\mvnw test
```

### Limpar e executar somente os testes

```powershell
.\mvnw clean test
```

### Executar apenas o teste `ClienteControllerTest`

```powershell
.\mvnw -Dtest=ClienteControllerTest test
```

---

# 2. IMAGENS DOCKER

### Listar todas as imagens existentes localmente

```powershell
docker images
```

### Criar a imagem do nosso Spring Boot usando o `Dockerfile`

```powershell
docker build -t projeto-springboot:1.0 .
```

### Criar novamente a imagem ignorando completamente o cache do Docker

```powershell
docker build --no-cache -t projeto-springboot:1.0 .
```

### Exibir informações detalhadas da nossa imagem

```powershell
docker inspect projeto-springboot:1.0
```

### Mostrar ID e data de criação da imagem

```powershell
docker inspect projeto-springboot:1.0 --format "{{.Id}} {{.Created}}"
```

### Mostrar as camadas utilizadas para construir a imagem

```powershell
docker history projeto-springboot:1.0
```

### Remover uma imagem

```powershell
docker rmi projeto-springboot:1.0
```

### Baixar uma imagem manualmente do Docker Hub

```powershell
docker pull ubuntu
```

### Exemplo: baixar PostgreSQL 17

```powershell
docker pull postgres:17
```

### Exemplo: baixar Redis 7

```powershell
docker pull redis:7
```

---

# 3. CONTAINERS

### Listar somente containers que estão rodando

```powershell
docker ps
```

### Listar todos os containers, inclusive os parados

```powershell
docker ps -a
```

### Criar e executar um container simples

```powershell
docker run hello-world
```

### Criar um container Ubuntu com terminal interativo

```powershell
docker run -it ubuntu bash
```

### Criar um container em segundo plano

```powershell
docker run -d --name ubuntu-teste ubuntu sleep 1000
```

### Subir manualmente nosso Spring Boot

```powershell
docker run -d --name projeto-springboot-container -p 8080:8080 projeto-springboot:1.0
```

### Subir nosso Spring Boot com volume H2

```powershell
docker run -d --name projeto-springboot-container -p 8080:8080 -v projeto-springboot-data:/app/data projeto-springboot:1.0
```

### Criar uma segunda instância usando porta externa 8081

```powershell
docker run -d --name projeto-springboot-container-2 -p 8081:8080 projeto-springboot:1.0
```

---

# 4. START / STOP / REMOVE

### Parar um container

```powershell
docker stop projeto-springboot-container
```

### Iniciar novamente o mesmo container

```powershell
docker start projeto-springboot-container
```

### Iniciar um container e conectar o terminal a ele

```powershell
docker start -ai nome-do-container
```

### Reiniciar um container

```powershell
docker restart projeto-springboot-container
```

### Remover um container parado

```powershell
docker rm projeto-springboot-container
```

### Parar e remover em sequência

```powershell
docker stop projeto-springboot-container
docker rm projeto-springboot-container
```

### Remover um container à força, inclusive se estiver rodando

```powershell
docker rm -f projeto-springboot-container
```

---

# 5. ENTRAR DENTRO DE UM CONTAINER

### Entrar em um container usando Bash

```powershell
docker exec -it projeto-springboot-container bash
```

### Se Bash não estiver disponível, usar `sh`

```powershell
docker exec -it projeto-springboot-container sh
```

### Mostrar o diretório atual dentro do Linux

```bash
pwd
```

### Listar arquivos

```bash
ls
```

### Listar arquivos com detalhes

```bash
ls -l
```

### Ver nosso `.jar` dentro do container

```bash
ls -l /app
```

### Confirmar versão do Java existente no container

```bash
java -version
```

### Sair do container e voltar ao PowerShell

```bash
exit
```

---

# 6. LOGS

### Mostrar todos os logs de um container

```powershell
docker logs projeto-springboot-container
```

### Mostrar somente as últimas 100 linhas

```powershell
docker logs --tail 100 projeto-springboot-container
```

### Mostrar somente as últimas 50 linhas

```powershell
docker logs --tail 50 projeto-springboot-container
```

### Mostrar somente as últimas 20 linhas

```powershell
docker logs --tail 20 projeto-springboot-container
```

### Acompanhar os logs em tempo real

```powershell
docker logs -f projeto-springboot-container
```

### Mostrar as últimas 20 linhas e continuar acompanhando

```powershell
docker logs -f --tail 20 projeto-springboot-container
```

### Sair do acompanhamento de logs sem parar o container

```text
Ctrl + C
```

---

# 7. PORTAS

### Exemplo Nginx: Windows 8085 → container 80

```powershell
docker run -d --name nginx-teste -p 8085:80 nginx
```

### Spring Boot: Windows 8080 → container 8080

```powershell
docker run -d --name projeto-springboot-container -p 8080:8080 projeto-springboot:1.0
```

Regra:

```text
-p PORTA_DO_WINDOWS:PORTA_DO_CONTAINER
```

---

# 8. VOLUMES

### Listar volumes

```powershell
docker volume ls
```

### Criar um volume manualmente

```powershell
docker volume create projeto-springboot-data
```

### Inspecionar um volume

```powershell
docker volume inspect projeto-springboot-data
```

### Remover um volume

```powershell
docker volume rm projeto-springboot-data
```

> Cuidado: remover volume pode significar perder os dados persistidos.

---

# 9. DOCKER COMPOSE

### Subir todos os serviços definidos no `compose.yaml`

```powershell
docker compose up -d
```

### Subir tudo reconstruindo a imagem da aplicação

```powershell
docker compose up -d --build
```

### Derrubar os containers e a rede do Compose

```powershell
docker compose down
```

### Derrubar os containers, rede E volumes

```powershell
docker compose down -v
```

> **Cuidado:** `-v` também remove os volumes e pode apagar o PostgreSQL.

### Listar os serviços do Compose

```powershell
docker compose ps
```

### Mostrar logs de todos os serviços

```powershell
docker compose logs
```

### Acompanhar logs de todos os serviços em tempo real

```powershell
docker compose logs -f
```

### Mostrar logs somente da aplicação

```powershell
docker compose logs app
```

### Mostrar logs somente do PostgreSQL

```powershell
docker compose logs postgres
```

### Mostrar logs somente do Redis

```powershell
docker compose logs redis
```

### Acompanhar logs do Spring Boot

```powershell
docker compose logs -f app
```

### Sair do acompanhamento dos logs

```text
Ctrl + C
```

---

# 10. REDIS

### Entrar no Redis CLI dentro do container

```powershell
docker exec -it projeto-redis redis-cli
```

O prompt ficará parecido com:

```text
127.0.0.1:6379>
```

### Criar uma chave simples

```text
SET nome Ricardo
```

### Consultar uma chave

```text
GET nome
```

### Criar um contador

```text
SET contador 1
```

### Incrementar contador

```text
INCR contador
```

### Listar todas as chaves

```text
KEYS *
```

### Consultar TTL da chave de cache do cliente 5

```text
TTL clientes::5
```

Interpretação:

```text
valor positivo = segundos restantes
-1 = chave existe, mas não tem TTL
-2 = chave não existe
```

### Apagar uma chave específica

```text
DEL clientes::5
```

### Apagar todas as chaves do Redis

```text
FLUSHALL
```

> Use `FLUSHALL` somente sabendo que todo o conteúdo do Redis será apagado.

### Observar os comandos chegando ao Redis em tempo real

```text
MONITOR
```

### Sair do `MONITOR`

```text
Ctrl + C
```

### Sair do Redis CLI

```text
exit
```

### Executar `KEYS *` diretamente pelo PowerShell sem entrar no Redis CLI

```powershell
docker exec -it projeto-redis redis-cli KEYS *
```

### Limpar o Redis diretamente pelo PowerShell

```powershell
docker exec -it projeto-redis redis-cli FLUSHALL
```

---

# 11. POSTGRESQL

### Confirmar se o container PostgreSQL está rodando

```powershell
docker ps
```

### Ver logs do PostgreSQL

```powershell
docker logs projeto-postgres
```

### Ver últimas 50 linhas do PostgreSQL

```powershell
docker logs --tail 50 projeto-postgres
```

### Entrar diretamente no PostgreSQL usando `psql`

```powershell
docker exec -it projeto-postgres psql -U postgres -d projetodb
```

Depois o prompt ficará parecido com:

```text
projetodb=#
```

### Listar bancos PostgreSQL

```sql
\l
```

### Listar tabelas do banco atual

```sql
\dt
```

### Mostrar estrutura da tabela `clientes`

```sql
\d clientes
```

### Consultar clientes

```sql
SELECT * FROM clientes;
```

### Consultar usuários

```sql
SELECT * FROM usuarios;
```

### Consultar pedidos

```sql
SELECT * FROM pedidos;
```

### Mostrar banco atualmente conectado

```sql
SELECT current_database();
```

### Sair do `psql`

```text
\q
```

---

# 12. POSTGRESQL PELO WINDOWS / DBEAVER

Nosso `compose.yaml` expõe:

```yaml
ports:
  - "5432:5432"
```

Por isso, no DBeaver:

```text
Host: localhost
Port: 5432
Database: projetodb
Username: postgres
Password: postgres
```

Dentro da rede Docker, entretanto, o Spring Boot usa:

```text
postgres:5432
```

e não:

```text
localhost:5432
```

---

# 13. INSPECIONAR CONTAINERS E IMAGENS

### Mostrar informações completas do container

```powershell
docker inspect projeto-springboot-container
```

### Descobrir qual imagem foi usada pelo container

```powershell
docker inspect projeto-springboot-container --format "{{.Image}}"
```

### Descobrir o ID da imagem atual

```powershell
docker inspect projeto-springboot:1.0 --format "{{.Id}}"
```

Se os IDs forem diferentes:

```text
container → imagem antiga
tag projeto-springboot:1.0 → imagem nova
```

Nesse caso, `stop/start` não atualiza o container. É necessário removê-lo e criar outro.

---

# 14. DOCKER INFO

### Ver versão instalada

```powershell
docker --version
```

### Mostrar informações do Docker Engine

```powershell
docker info
```

### Ver versão do Docker Compose

```powershell
docker compose version
```

---

# 15. WSL

### Listar distribuições WSL

```powershell
wsl -l -v
```

### Entrar no Ubuntu do WSL

```powershell
wsl
```

### Entrar explicitamente na distribuição Ubuntu

```powershell
wsl -d Ubuntu
```

### Sair do Ubuntu e voltar ao PowerShell

```bash
exit
```

---

# 16. FLUXO MAIS COMUM DO NOSSO PROJETO

### 1. Fizemos alteração no código Java

```powershell
.\mvnw clean package
```

### 2. Derrubar os containers atuais

```powershell
docker compose down
```

### 3. Reconstruir a imagem e subir Spring Boot + PostgreSQL + Redis

```powershell
docker compose up -d --build
```

### 4. Conferir se tudo está rodando

```powershell
docker compose ps
```

### 5. Ver últimas linhas do Spring Boot

```powershell
docker logs --tail 50 projeto-springboot-container
```

### 6. Acompanhar logs em tempo real

```powershell
docker logs -f --tail 20 projeto-springboot-container
```

### 7. Sair do acompanhamento

```text
Ctrl + C
```

### 8. Ver cache Redis

```powershell
docker exec -it projeto-redis redis-cli KEYS *
```

### 9. Entrar no PostgreSQL

```powershell
docker exec -it projeto-postgres psql -U postgres -d projetodb
```

### 10. Derrubar o ambiente sem perder os dados PostgreSQL

```powershell
docker compose down
```

---

# Resumo rápido para decorar

```text
docker images
→ imagens

docker ps
→ containers rodando

docker ps -a
→ todos os containers

docker build
→ cria imagem

docker run
→ cria + inicia container

docker start
→ inicia container existente

docker stop
→ para container

docker rm
→ remove container

docker rmi
→ remove imagem

docker logs
→ mostra logs

docker exec
→ executa algo dentro de container

docker volume ls
→ lista volumes

docker compose up -d
→ sobe o ambiente

docker compose up -d --build
→ reconstrói e sobe

docker compose ps
→ mostra serviços

docker compose logs
→ logs do ambiente

docker compose down
→ desmonta ambiente

Ctrl + C
→ para acompanhamento/interação no console sem necessariamente parar o serviço
```