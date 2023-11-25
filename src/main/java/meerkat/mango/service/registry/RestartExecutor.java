package meerkat.mango.service.registry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Component
@Profile("!backup")
public class RestartExecutor {

    private static final String UPDATE_PATH = "/registry/services";
    private final String backupHost;
    private final String backupPort;
    private final RestTemplate restTemplate;
    private final HealthCheckExecutor healthCheckExecutor;

    @Autowired
    public RestartExecutor(@Value("${backup.registry.host:localhost}") final String backupHost,
                           @Value("${backup.registry.host:50001}") final String backupPort,
                           final HealthCheckExecutor healthCheckExecutor) {
        this.backupHost = backupHost;
        this.backupPort = backupPort;
        this.healthCheckExecutor = healthCheckExecutor;
        restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.of(1, ChronoUnit.SECONDS))
                .setReadTimeout(Duration.of(1, ChronoUnit.SECONDS))
                .build();
    }

    @PostConstruct
    private void postConstruct() {
       final var uri = UriComponentsBuilder.newInstance()
                .host(backupHost)
                .port(backupPort)
                .path(UPDATE_PATH)
                .toUriString();
       try {
           final var response = restTemplate.getForEntity("http:" + uri, Map.class);
           if (!response.getStatusCode().is2xxSuccessful() || !response.hasBody()) {
               return;
           }

           Map<String, Map<String, ServiceUrl>> services = response.getBody();
           System.out.println("received Services");
           healthCheckExecutor.setServices(services);
       } catch (ResourceAccessException ignored) {
       }

    }
}
