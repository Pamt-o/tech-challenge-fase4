package org.acme.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.azure.communication.email.*;
import com.azure.communication.email.models.*;
import com.azure.core.util.polling.SyncPoller;
import com.azure.core.util.polling.PollResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotificacaoFunction {

    // 1. Criamos o formatador (ex: 11/11/2026 13:55)
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FunctionName("NotificacaoFunction")
    public void run(
            @ServiceBusQueueTrigger(name = "message", queueName = "fila-notificacoes", connection = "AzureServiceBusConnection") final String message,
            final ExecutionContext context) {

        context.getLogger().info("Mensagem recebida: " + message);

        try {
            // 2. Lê o JSON
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            JsonNode jsonNode = mapper.readTree(message);

            // 3. Extrai os dados e formata a data
            String descricao = jsonNode.get("descricao").asText();
            int nota = jsonNode.get("nota").asInt();

            // Pega a data como texto do JSON e converte para LocalDateTime
            String dataString = jsonNode.get("dataEnvio").asText();
            LocalDateTime dataEnvio = LocalDateTime.parse(dataString);
            String dataFormatada = dataEnvio.format(FORMATTER);

            // 4. Monta o e-mail bonitinho
            String corpoEmail = String.format(
                    "⚠️ ALERTA: Avaliação Urgente Recebida\n\n" +
                            "Descrição: %s\n" +
                            "Nota: %d\n" +
                            "Data de Envio: %s",
                    descricao, nota, dataFormatada
            );

            // 5. Envia o e-mail
            String connectionString = System.getenv("AZURE_COMMUNICATION_CONNECTION_STRING");
            String senderAddress = System.getenv("AZURE_COMMUNICATION_SENDER_ADDRESS");
            String adminEmail = System.getenv("ADMIN_EMAIL");

            EmailClient emailClient = new EmailClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            EmailMessage email = new EmailMessage()
                    .setSenderAddress(senderAddress)
                    .setToRecipients(adminEmail)
                    .setSubject("⚠️ ALERTA: Avaliação Urgente Recebida")
                    .setBodyPlainText(corpoEmail);

            SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(email, null);
            poller.waitForCompletion();

            context.getLogger().info("E-mail enviado com sucesso!");

        } catch (Exception e) {
            context.getLogger().severe("Erro ao processar notificação: " + e.getMessage());
            e.printStackTrace();
        }
    }
}