package org.acme;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.azure.communication.email.*;
import com.azure.communication.email.models.*;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;

import java.sql.*;
import java.time.LocalDateTime;

public class RelatorioFunction {

    @FunctionName("RelatorioFunction")
    public void run(
            @TimerTrigger(name = "timerInfo", schedule = "0 0 8 * * MON")
            String timerInfo,
            final ExecutionContext context) {

        context.getLogger().info("RelatorioFunction executada em: " + LocalDateTime.now());

        String dbUrl = System.getenv("DB_JDBC_URL");
        String dbUser = System.getenv("DB_USERNAME");
        String dbPass = System.getenv("DB_PASSWORD");

        StringBuilder relatorio = new StringBuilder();
        relatorio.append("📊 RELATÓRIO SEMANAL DE AVALIAÇÕES\n");
        relatorio.append("Data de geração: ").append(LocalDateTime.now()).append("\n\n");

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            // 1. Média das notas
            String sqlMedia = "SELECT AVG(nota) FROM avaliacao WHERE data_envio >= NOW() - INTERVAL '7 days'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlMedia)) {
                if (rs.next()) {
                    double media = rs.getDouble(1);
                    relatorio.append("Média das notas (últimos 7 dias): ").append(String.format("%.2f", media)).append("\n");
                    context.getLogger().info("Média das notas (últimos 7 dias): " + media);
                }
            }

            // 2. Contagem por urgência
            relatorio.append("\nContagem por urgência:\n");
            String sqlUrgencia = "SELECT urgencia, COUNT(*) FROM avaliacao WHERE data_envio >= NOW() - INTERVAL '7 days' GROUP BY urgencia";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlUrgencia)) {
                while (rs.next()) {
                    boolean urgente = rs.getBoolean(1);
                    long count = rs.getLong(2);
                    relatorio.append(" - ").append(urgente ? "Urgente" : "Não urgente").append(": ").append(count).append(" avaliações\n");
                    context.getLogger().info("Urgência " + urgente + ": " + count + " avaliações");
                }
            }

            // 3. Contagem por dia
            relatorio.append("\nContagem por dia:\n");
            String sqlDia = "SELECT DATE(data_envio) as dia, COUNT(*) FROM avaliacao WHERE data_envio >= NOW() - INTERVAL '7 days' GROUP BY DATE(data_envio) ORDER BY dia";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlDia)) {
                while (rs.next()) {
                    String dia = rs.getString(1);
                    long count = rs.getLong(2);
                    relatorio.append(" - ").append(dia).append(": ").append(count).append(" avaliações\n");
                    context.getLogger().info("Dia " + dia + ": " + count + " avaliações");
                }
            }

        } catch (SQLException e) {
            context.getLogger().severe("Erro no banco de dados: " + e.getMessage());
            relatorio.append("\nErro ao gerar relatório: ").append(e.getMessage());
        }

        // Enviar o relatório por e-mail
        try {
            String connectionString = System.getenv("AZURE_COMMUNICATION_CONNECTION_STRING");
            String senderAddress = System.getenv("AZURE_COMMUNICATION_SENDER_ADDRESS");
            String adminEmail = System.getenv("ADMIN_EMAIL");

            EmailClient emailClient = new EmailClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            EmailMessage email = new EmailMessage()
                    .setSenderAddress(senderAddress)
                    .setToRecipients(adminEmail)
                    .setSubject("📊 Relatório Semanal de Avaliações")
                    .setBodyPlainText(relatorio.toString());

            SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(email, null);
            PollResponse<EmailSendResult> response = poller.waitForCompletion();

            context.getLogger().info("Relatório enviado! ID: " + response.getValue().getId());

        } catch (Exception e) {
            context.getLogger().severe("Erro ao enviar relatório: " + e.getMessage());
        }
    }
}