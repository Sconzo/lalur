package br.com.lalurecf.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do OpenAPI/Swagger para documentação da API.
 *
 * <p>Define informações gerais da API e esquema de segurança JWT.
 */
@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "LALUR V2 ECF API",
            version = "1.0.0",
            description = "API para escrituração contábil fiscal - cálculos de IRPJ e CSLL",
            contact =
                @Contact(
                    name = "LALUR V2 Team",
                    email = "support@lalurecf.com.br",
                    url = "https://lalurecf.com.br")))
@SecurityScheme(
    name = "bearer-jwt",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "JWT token de autenticação. Obtenha via POST /api/v1/auth/login")
public class OpenApiConfig {}
