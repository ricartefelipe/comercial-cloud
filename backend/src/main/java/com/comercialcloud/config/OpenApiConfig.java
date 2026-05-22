package com.comercialcloud.config;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                title = "ComercialCloud API",
                version = "1.0.0",
                description = "SaaS multitenant commercial system",
                contact = @Contact(name = "ComercialCloud")
        ),
        servers = @Server(url = "/", description = "Default")
)
public class OpenApiConfig {
}
