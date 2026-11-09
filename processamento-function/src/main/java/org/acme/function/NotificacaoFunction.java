package org.acme.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.azure.communication.email.*;
import com.azure.communication.email.models.*;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NotificacaoFunction {

    @FunctionName("NotificacaoFunction")
    public void run(
            @ServiceBusQueueTrigger(
                    name = "message",
                    queueName = "fila-avaliacoes-urgentes",
                    connection = "AzureServiceBusConnection"
            ) String message,
            final ExecutionContext context) {

        context.getLogger().info("Mensagem recebida do Service Bus: " + message);

        try {
            // 1. Parse do JSON da mensagem
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(message);

            String descricao = json.get("descricao").asText();
            int nota = json.get("nota").asInt();
            String dataEnvio = json.get("dataEnvio").toString(); // Ou formatar a data

            // 2. Montar o corpo do e-mail formatado
            String corpoEmail = String.format(
                    "⚠️ ALERTA: Avaliação Urgente Recebida\n\n" +
                            "Descrição: %s\n" +
                            "Nota: %d\n" +
                            "Data de Envio: %s\n\n" +
                            "Por favor, tome as providências necessárias.",
                    descricao, nota, dataEnvio
            );

            // 3. Configuração do ACS
            String connectionString = System.getenv("AZURE_COMMUNICATION_CONNECTION_STRING");
            String senderAddress = System.getenv("AZURE_COMMUNICATION_SENDER_ADDRESS");
            String adminEmail = System.getenv("ADMIN_EMAIL");

            EmailClient emailClient = new EmailClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            EmailMessage email = new EmailMessage()
                    .setSenderAddress(senderAddress)
                    .setToRecipients(adminEmail)
                    .setSubject("⚠️ Alerta: Avaliação Urgente Recebida")
                    .setBodyPlainText(corpoEmail);

            SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(email, null);
            PollResponse<EmailSendResult> response = poller.waitForCompletion();

            context.getLogger().info("E-mail enviado! ID: " + response.getValue().getId());

        } catch (Exception e) {
            context.getLogger().severe("Erro ao enviar e-mail: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}