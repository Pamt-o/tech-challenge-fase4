package org.acme.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
public class ApiKeyFilter implements ContainerRequestFilter {

    @ConfigProperty(name = "app.api.key")
    String apiKeyConfigurada;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (!requestContext.getUriInfo().getPath().contains("avaliacao")) {
            return;
        }

        String apiKeyRecebida = requestContext.getHeaderString("X-API-Key");

        if (apiKeyRecebida == null || !apiKeyRecebida.equals(apiKeyConfigurada)) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("API Key inválida ou ausente")
                    .build());
        }
    }
}