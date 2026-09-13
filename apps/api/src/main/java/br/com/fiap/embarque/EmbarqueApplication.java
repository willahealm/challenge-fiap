package br.com.fiap.embarque;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class EmbarqueApplication {
    public static void main(String[] args) { SpringApplication.run(EmbarqueApplication.class, args); }
    @Bean Clock clock() { return Clock.systemUTC(); }
}
