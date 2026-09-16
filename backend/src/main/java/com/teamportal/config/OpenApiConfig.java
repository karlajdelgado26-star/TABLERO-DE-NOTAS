package com.teamportal.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI 3 de la API (springdoc).
 * <ul>
 *   <li>Swagger UI: {@code /swagger-ui/index.html}</li>
 *   <li>Especificación: {@code /v3/api-docs} (JSON) y {@code /v3/api-docs.yaml}</li>
 * </ul>
 * El servidor se declara como "/" para que "Try it out" use la misma dirección
 * desde la que se abrió Swagger (EC2, localhost:8081, etc.).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Team Portal API",
                version = "1.0.0",
                description = """
                        API REST del portal de equipo con tableros de notas.

                        **Cómo probarla:**
                        1. Ejecuta `POST /api/auth/login` con un usuario (por ejemplo `admin@demo.com`).
                        2. Copia el valor de `token` de la respuesta.
                        3. Pulsa **Authorize**, pega el token (sin la palabra Bearer) y confirma.

                        **Roles:** `USER` (usuario), `LEADER` (líder) y `ADMIN` (administrador).
                        Los errores devuelven `{ timestamp, status, error, message, path }`.
                        """
        ),
        servers = @Server(url = "/", description = "Servidor actual"),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
)
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT obtenido en POST /api/auth/login (vigencia de 24 horas)"
)
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";
}
