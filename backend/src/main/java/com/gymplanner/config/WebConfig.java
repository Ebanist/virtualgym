package com.gymplanner.config;

import com.gymplanner.auth.AuthUser;
import com.gymplanner.auth.AuthUserArgumentResolver;
import java.util.List;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    static {
        // AuthUser pochodzi z tokenu – nie jest parametrem żądania w dokumentacji OpenAPI.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(AuthUser.class);
    }

    private final AuthUserArgumentResolver authUserArgumentResolver;

    public WebConfig(AuthUserArgumentResolver authUserArgumentResolver) {
        this.authUserArgumentResolver = authUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authUserArgumentResolver);
    }
}
