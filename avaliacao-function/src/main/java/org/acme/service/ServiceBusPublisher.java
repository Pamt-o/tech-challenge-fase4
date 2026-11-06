package org.acme.service;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // <-- ADICIONE ESTE IMPORT
import jakarta.enterprise.context.ApplicationScoped;
import org.acme.model.Avaliacao;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ServiceBusPublisher {

    @ConfigProperty(name = "azure.servicebus.connection-string")
    String connectionString;

    private static final String QUEUE_NAME = "fila-avaliacoes-urgentes";

    public void publicarAvaliacaoUrgente(Avaliacao avaliacao) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            String json = mapper.writeValueAsString(avaliacao);

            ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
                    .connectionString(connectionString)
                    .sender()
                    .queueName(QUEUE_NAME)
                    .buildClient();

            senderClient.sendMessage(new ServiceBusMessage(json));
            senderClient.close();
            System.out.println("Mensagem enviada para o Service Bus: " + json);
        } catch (Exception e) {
            System.err.println("Erro ao publicar no Service Bus: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}