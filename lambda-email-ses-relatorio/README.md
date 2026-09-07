# Lambda de e-mail de relatório

Esta função Java é acionada pelo tópico SNS quando um relatório de pedido
fica disponível.

## Etapa atual

Nesta versão, a Lambda:

1. recebe o envelope do Amazon SNS;
2. lê o JSON publicado pela aplicação;
3. converte o JSON para o record `EventoRelatorioDisponivel`;
4. utiliza a `objectKey` para baixar o PDF do Amazon S3;
5. monta um e-mail em texto e HTML com o PDF anexado;
6. envia o e-mail pelo Amazon SES;
7. registra no CloudWatch o identificador retornado pelo SES.

## Variável de ambiente

As configurações externas da função são:

```text
BUCKET_RELATORIOS=recomeco-pedidos-anexos-df33a1eb
EMAIL_REMETENTE=ricardo.bredes@gmail.com
EMAIL_DESTINATARIO=ricardo.bredes@gmail.com
```

## Classe handler

Na configuração da função Lambda, o handler deve ser informado como:

```text
br.com.exemplo.lambdaemail.EnviarRelatorioEmailHandler::handleRequest
```

## Gerar o pacote

Dentro desta pasta, execute:

```powershell
..\mvnw.cmd clean package
```

O arquivo que será enviado para a AWS será criado em:

```text
target/recomeco-email-ses-relatorio-lambda.jar
```
