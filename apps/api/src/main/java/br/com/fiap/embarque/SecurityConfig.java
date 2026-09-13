package br.com.fiap.embarque;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration @Profile("demo")
class SecurityConfig {
    @Bean SecurityFilterChain chain(HttpSecurity http,DemoAuth auth,ObjectMapper json,Clock clock,
                                    @Value("${app.cors-origins}") String origins) throws Exception {
        var cors=new CorsConfiguration(); cors.setAllowedOrigins(Arrays.asList(origins.split(",")));
        cors.setAllowedMethods(List.of("GET","POST","PATCH","DELETE","OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization","Content-Type")); cors.setMaxAge(3600L);
        var source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",cors);
        http.cors(c -> c.configurationSource(source)).csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a.requestMatchers("/actuator/health","/v1/auth/demo","/error").permitAll()
                .requestMatchers("/v1/ops/**","/v1/demo/reset").hasRole("OPERATOR").anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> error(res,401,"UNAUTHENTICATED","Autenticação necessária.",json,clock))
                .accessDeniedHandler((req,res,ex) -> error(res,403,"FORBIDDEN","Acesso não permitido.",json,clock)))
            .addFilterBefore(new OncePerRequestFilter() {
                @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
                    res.setHeader("Cache-Control","no-store");
                    String header=req.getHeader("Authorization");
                    if(header!=null) {
                        try {
                            if(!header.startsWith("Bearer ")) throw DemoAuth.unauthorized();
                            var actor=auth.authenticate(header.substring(7));
                            var authentication=new UsernamePasswordAuthenticationToken(actor,null,List.of(new SimpleGrantedAuthority("ROLE_"+actor.role())));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        } catch(ApiException ex) { error(res,ex.status.value(),ex.code,ex.getMessage(),json,clock); return; }
                    }
                    chain.doFilter(req,res);
                }
            },UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    private static void error(HttpServletResponse res,int status,String code,String message,ObjectMapper json,Clock clock) throws IOException {
        res.setStatus(status); res.setContentType("application/json"); res.setCharacterEncoding("UTF-8");
        json.writeValue(res.getWriter(),new Models.ApiError(code,message,clock.instant()));
    }
}
