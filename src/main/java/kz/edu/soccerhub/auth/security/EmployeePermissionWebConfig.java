package kz.edu.soccerhub.auth.security;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
@Configuration
@RequiredArgsConstructor
public class EmployeePermissionWebConfig implements WebMvcConfigurer {
 private final EmployeePermissionInterceptor interceptor;
 public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(interceptor); }
}
