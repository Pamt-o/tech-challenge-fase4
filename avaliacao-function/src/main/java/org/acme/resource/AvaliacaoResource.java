package org.acme.resource;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.model.Avaliacao;
import org.acme.service.ServiceBusPublisher;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/avaliacao")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AvaliacaoResource {

    @Inject
    ServiceBusPublisher serviceBusPublisher;

    @POST
    @Transactional
    public Response criarAvaliacao(Avaliacao avaliacao) {
        if (avaliacao.nota == null || avaliacao.nota < 0 || avaliacao.nota > 10) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Nota deve ser entre 0 e 10")
                    .build();
        }

        avaliacao.persist();

        // Se for urgente, publica a mensagem no Service Bus
        if (avaliacao.urgencia != null && avaliacao.urgencia) {
            serviceBusPublisher.publicarAvaliacaoUrgente(avaliacao); // <-- MUDOU AQUI
        }
        return Response.status(Response.Status.CREATED).entity(avaliacao).build();
    }


    @GET
    @Path("/relatorio")
    public Response gerarRelatorioSemanal() {
        // 1. Buscar todas as avaliações
        List<Avaliacao> todas = Avaliacao.listAll();

        if (todas.isEmpty()) {
            return Response.ok("Nenhuma avaliação registrada ainda.").build();
        }

        // 2. Quantidade por dia (agrupando pela data)
        Map<LocalDate, Long> porDia = todas.stream()
                .collect(Collectors.groupingBy(
                        a -> a.dataEnvio.toLocalDate(),
                        Collectors.counting()
                ));

        // 3. Quantidade por urgência (true/false)
        Map<Boolean, Long> porUrgencia = todas.stream()
                .collect(Collectors.groupingBy(
                        a -> a.urgencia != null && a.urgencia,
                        Collectors.counting()
                ));

        // 4. Montar o objeto de resposta
        Map<String, Object> relatorio = new HashMap<>();
        relatorio.put("totalAvaliacoes", todas.size());
        relatorio.put("avaliacoesPorDia", porDia);
        relatorio.put("avaliacoesPorUrgencia", porUrgencia);

        // 5. Listar as descrições (para o e-mail, conforme pedido no enunciado)
        List<Map<String, String>> detalhes = todas.stream().map(a -> {
            Map<String, String> d = new HashMap<>();
            d.put("descricao", a.descricao);
            d.put("urgencia", String.valueOf(a.urgencia));
            d.put("dataEnvio", a.dataEnvio.toString());
            return d;
        }).collect(Collectors.toList());

        relatorio.put("detalhes", detalhes);

        return Response.ok(relatorio).build();
    }

    @GET
    @Path("/health")
    @Produces(MediaType.TEXT_PLAIN)
    public Response health() {
        System.out.println("HEALTH CHECK EXECUTADO COM SUCESSO");

        return Response.ok("AvaliacaoFunction ONLINE").build();
    }
}