package br.com.fiap.embarque;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.*;

/** Never silently expose demo accounts/data when an integration profile was requested. */
@Configuration @Profile("!demo | appwrite | production")
class IntegrationProfileGuard {
    @PostConstruct void rejectUnconfiguredIntegration() {
        throw new IllegalStateException("Appwrite integration is not implemented in this local MVP. Use the demo profile; see apps/api/README.md for integration requirements.");
    }
}
