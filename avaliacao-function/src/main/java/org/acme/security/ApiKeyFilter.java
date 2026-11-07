package org.acme.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
public class ApiKeyFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {
        // Só valida a chave para o endpoint de avaliação
        if (!requestContext.getUriInfo().getPath().contains("avaliacao")) {
            return;
        }

        // Lê a variável de ambiente diretamente, sem depender da injeção do Quarkus
        String apiKeyConfigurada = System.getenv("API_KEY");
        String apiKeyRecebida = requestContext.getHeaderString("X-API-Key");

        if (apiKeyConfigurada == null || apiKeyConfigurada.isEmpty()) {
            // Se a chave não estiver configurada no ambiente, bloqueia por segurança
            requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("API Key não configurada no servidor")
                    .build());
            return;
        }

        if (apiKeyRecebida == null || !apiKeyRecebida.equals(apiKeyConfigurada)) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("API Key inválida ou ausente")
                    .build());
        }
    }
}