package org.acme;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.azure.communication.email.*;
import com.azure.communication.email.models.*;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;

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

        // A mensagem virá como JSON (ex: {"descricao":"...","nota":2,"dataEnvio":"..."})
        // Aqui você pode usar Jackson ou Gson para desserializar, mas para simplificar,
        // vamos apenas extrair os dados básicos.

        try {
            // Configuração do ACS (via variáveis de ambiente)
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
                    .setBodyPlainText("Uma nova avaliação urgente foi registrada.\n\nDados: " + message);

            SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(email, null);
            PollResponse<EmailSendResult> response = poller.waitForCompletion();

            context.getLogger().info("E-mail enviado! ID: " + response.getValue().getId());

        } catch (Exception e) {
            context.getLogger().severe("Erro ao enviar e-mail: " + e.getMessage());
            throw new RuntimeException(e); // Faz o Service Bus reenfileirar a mensagem em caso de falha
        }
    }

}