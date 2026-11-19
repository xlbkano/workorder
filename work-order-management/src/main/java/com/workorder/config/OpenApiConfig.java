package com.workorder.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI workOrderOpenApi() {
        String schemeName = "token";
        return new OpenAPI()
                .info(new Info()
                        .title("内部工单审批管理系统 API")
                        .description("员工提交工单、多级流转审核、操作日志与审批记录可追溯")
                        .version("1.0.0")
                        .contact(new Contact().name("工单平台组")))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("登录接口返回的 token")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName));
    }
}
