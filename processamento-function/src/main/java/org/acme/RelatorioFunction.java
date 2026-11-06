package org.acme;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.sql.*;
import java.time.LocalDateTime;

public class RelatorioFunction {

    @FunctionName("RelatorioFunction")
    public void run(
            @TimerTrigger(name = "timerInfo", schedule = "0 0 8 * * MON") // Toda segunda-feira às 8h
            String timerInfo,
            final ExecutionContext context) {

        context.getLogger().info("RelatorioFunction executada em: " + LocalDateTime.now());

        String dbUrl = System.getenv("DB_JDBC_URL");
        String dbUser = System.getenv("DB_USERNAME");
        String dbPass = System.getenv("DB_PASSWORD");

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            // 1. Média das notas
            String sqlMedia = "SELECT AVG(nota) FROM avaliacao WHERE data_envio >= NOW() - INTERVAL '7 days'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlMedia)) {
                if (rs.next()) {
                    double media = rs.getDouble(1);
                    context.getLogger().info("Média das notas (últimos 7 dias): " + media);
                }
            }

            // 2. Contagem por urgência
            String sqlUrgencia = "SELECT urgencia, COUNT(*) FROM avaliacao WHERE data_envio >= NOW() - INTERVAL '7 days' GROUP BY urgencia";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlUrgencia)) {
                while (rs.next()) {
                    boolean urgente = rs.getBoolean(1);
                    long count = rs.getLong(2);
                    context.getLogger().info("Urgência " + urgente + ": " + count + " avaliações");
                }
            }

            // 3. Contagem por dia
            String sqlDia = "SELECT DATE(data_envio) as dia, COUNT(*) FROM avaliacao WHERE data_envio >= NOW() - INTERVAL '7 days' GROUP BY DATE(data_envio) ORDER BY dia";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlDia)) {
                while (rs.next()) {
                    String dia = rs.getString(1);
                    long count = rs.getLong(2);
                    context.getLogger().info("Dia " + dia + ": " + count + " avaliações");
                }
            }

            // Aqui você pode montar o relatório e enviar por e-mail (reutilizando o ACS)

        } catch (SQLException e) {
            context.getLogger().severe("Erro no banco de dados: " + e.getMessage());
        }
    }
}