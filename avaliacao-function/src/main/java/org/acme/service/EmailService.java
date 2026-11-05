package org.acme.service;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EmailService {

    private static final String CONNECTION_STRING = "endpoint=https://acs-tech-challenge.unitedstates.communication.azure.com/;accesskey=C79i7FCWNcGO06HoFd4zHaarX07Zgb0WSf0nXoDFO0xuGgNPeNUkJQQJ99CKACULyCpE8413AAAAAZCSv0ek";
    private static final String SENDER_ADDRESS = "DoNotReply@4d069d01-5745-40ee-92a0-ce28122bfbd1.azurecomm.net";

    public void enviarEmailUrgente(String descricao, String urgencia, String dataEnvio) {
        EmailClient emailClient = new EmailClientBuilder()
                .connectionString(CONNECTION_STRING)
                .buildClient();

        EmailMessage message = new EmailMessage()
                .setSenderAddress(SENDER_ADDRESS)
                .setToRecipients("artemis121289@gmail.com") // Coloque o e-mail de destino
                .setSubject("⚠️ Alerta: Avaliação Urgente Recebida")
                .setBodyPlainText(
                        "Uma nova avaliação urgente foi registrada.\n\n" +
                                "Descrição: " + descricao + "\n" +
                                "Urgência: " + urgencia + "\n" +
                                "Data de Envio: " + dataEnvio
                );

        SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(message);
        PollResponse<EmailSendResult> response = poller.waitForCompletion();

        if (response.getStatus().isComplete()) {
            System.out.println("E-mail enviado com sucesso! ID: " + response.getValue().getId());
        } else {
            System.out.println("Falha ao enviar e-mail. Status: " + response.getStatus());
        }
    }
}
