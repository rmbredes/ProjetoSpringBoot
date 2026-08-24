# Comandos do Projeto Spring Boot com Keycloak

## Visão geral dos terminais

Durante o desenvolvimento normal, mantenha três terminais:

| Terminal | Finalidade | Porta |
|---|---|---:|
| CMD 1 | Keycloak | 8081 |
| CMD 2 | Aplicação Spring Boot | 8080 |
| PowerShell 3 | Testes OAuth 2.0 | — |

## 1. Conferir o Java

No CMD:

```cmd
echo %JAVA_HOME%
java -version
where java
```

O `JAVA_HOME` esperado é:

```text
C:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot
```

## 2. Iniciar o Keycloak

Abra o primeiro CMD:

```cmd
cd /d C:\Recomeco\ferramentas\keycloak-26.7.0
bin\kc.bat start-dev --http-port=8081
```

Mantenha essa janela aberta.

Console administrativo:

```text
http://localhost:8081/admin/
```

Realm usado pelo projeto:

```text
http://localhost:8081/realms/projeto-springboot
```

Metadados OpenID Connect:

```text
http://localhost:8081/realms/projeto-springboot/.well-known/openid-configuration
```

## 3. Compilar a aplicação

Abra outro CMD:

```cmd
cd /d C:\Recomeco\projetos\ProjetoSpringBoot
mvnw.cmd clean compile
```

Compilar sem limpar os artefatos anteriores:

```cmd
mvnw.cmd compile
```

## 4. Executar os testes Java

```cmd
cd /d C:\Recomeco\projetos\ProjetoSpringBoot
mvnw.cmd test
```

Limpar e executar os testes:

```cmd
mvnw.cmd clean test
```

Executar somente uma classe de teste:

```cmd
mvnw.cmd -Dtest=SecurityControllerTest test
```

Executar somente um método de teste:

```cmd
mvnw.cmd -Dtest=SecurityControllerTest#deveRetornar401QuandoNaoEnviarToken test
```

## 5. Iniciar a aplicação Spring Boot

No segundo CMD:

```cmd
cd /d C:\Recomeco\projetos\ProjetoSpringBoot
mvnw.cmd spring-boot:run
```

Mantenha essa janela aberta.

Endereço-base da API:

```text
http://localhost:8080/ProjetoSpringBoot
```

Endpoints principais:

```text
http://localhost:8080/ProjetoSpringBoot/clientes
http://localhost:8080/ProjetoSpringBoot/pedidos
http://localhost:8080/ProjetoSpringBoot/admin
```

## 6. Executar o teste completo OAuth 2.0

Abra um PowerShell:

```powershell
cd C:\Recomeco\projetos\ProjetoSpringBoot
powershell -ExecutionPolicy Bypass -File .\testar-oauth2.ps1
```

Se a política de execução já permitir scripts:

```powershell
cd C:\Recomeco\projetos\ProjetoSpringBoot
.\testar-oauth2.ps1
```

Os logs são salvos em:

```text
C:\Recomeco\projetos\ProjetoSpringBoot\logs
```

## 7. Teste rápido sem token

No CMD:

```cmd
curl -i "http://localhost:8080/ProjetoSpringBoot/clientes"
```

Resultado esperado:

```text
HTTP 401 Unauthorized
```

## 8. Obter token USER manualmente

No PowerShell:

```powershell
$dadosLogin = @{
    grant_type = "password"
    client_id  = "projeto-springboot-api"
    username   = "ricardo"
    password   = "user123"
}

$tokenUser = (
    Invoke-RestMethod `
        -Method Post `
        -Uri "http://localhost:8081/realms/projeto-springboot/protocol/openid-connect/token" `
        -ContentType "application/x-www-form-urlencoded" `
        -Body $dadosLogin
).access_token
```

Não use uma senha pessoal nesse ambiente de demonstração.

## 9. Chamar `/clientes` com USER

```powershell
Invoke-WebRequest `
    -Uri "http://localhost:8080/ProjetoSpringBoot/clientes" `
    -Headers @{ Authorization = "Bearer $tokenUser" }
```

Resultado esperado:

```text
HTTP 200 OK
```

## 10. Chamar `/admin` com USER

```powershell
try {
    Invoke-WebRequest `
        -Uri "http://localhost:8080/ProjetoSpringBoot/admin" `
        -Headers @{ Authorization = "Bearer $tokenUser" }
}
catch {
    $_.Exception.Response.StatusCode
    $_.ErrorDetails.Message
}
```

Resultado esperado:

```text
HTTP 403 Forbidden
```

## 11. Obter token ADMIN manualmente

```powershell
$dadosAdmin = @{
    grant_type = "password"
    client_id  = "projeto-springboot-api"
    username   = "admin-api"
    password   = "admin123"
}

$tokenAdmin = (
    Invoke-RestMethod `
        -Method Post `
        -Uri "http://localhost:8081/realms/projeto-springboot/protocol/openid-connect/token" `
        -ContentType "application/x-www-form-urlencoded" `
        -Body $dadosAdmin
).access_token
```

## 12. Chamar `/admin` com ADMIN

```powershell
Invoke-WebRequest `
    -Uri "http://localhost:8080/ProjetoSpringBoot/admin" `
    -Headers @{ Authorization = "Bearer $tokenAdmin" }
```

Resultado esperado:

```text
HTTP 200 OK
Acesso administrativo permitido
```

## 13. Gerar o arquivo JAR

Gerar o pacote executando os testes:

```cmd
cd /d C:\Recomeco\projetos\ProjetoSpringBoot
mvnw.cmd clean package
```

Gerar o pacote sem executar testes:

```cmd
mvnw.cmd clean package -DskipTests
```

O arquivo será criado em:

```text
target\ProjetoSpringBoot-0.0.1-SNAPSHOT.jar
```

## 14. Executar o JAR

Com o Keycloak funcionando:

```cmd
cd /d C:\Recomeco\projetos\ProjetoSpringBoot
java -jar target\ProjetoSpringBoot-0.0.1-SNAPSHOT.jar
```

Não execute simultaneamente o JAR e `mvnw.cmd spring-boot:run`, pois ambos tentariam usar a porta 8080.

## 15. Encerrar os servidores

Na janela da aplicação Spring:

```text
Ctrl + C
```

Na janela do Keycloak:

```text
Ctrl + C
```

Se o terminal perguntar se deseja finalizar o processo, confirme com `S` ou `Y`, conforme o idioma apresentado.

## 16. Sequência diária resumida

Terminal 1:

```cmd
cd /d C:\Recomeco\ferramentas\keycloak-26.7.0
bin\kc.bat start-dev --http-port=8081
```

Terminal 2:

```cmd
cd /d C:\Recomeco\projetos\ProjetoSpringBoot
mvnw.cmd spring-boot:run
```

Terminal 3:

```powershell
cd C:\Recomeco\projetos\ProjetoSpringBoot
powershell -ExecutionPolicy Bypass -File .\testar-oauth2.ps1
```
