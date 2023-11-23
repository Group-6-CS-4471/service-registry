package meerkat.mango.service.registry.backup;

import meerkat.mango.service.registry.HealthCheckExecutor;
import meerkat.mango.service.registry.ServiceUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Profile("backup")
public class BackupRegistryExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(BackupRegistryExecutor.class);
    private static final String UPDATE_PATH = "/registry/services";
    private final RestTemplate restTemplate;
    private final String mainRegistryUri;
    private final int backupUpdateInterval;

    private final HealthCheckExecutor healthCheckExecutor;
    private final ScheduledExecutorService backupExecutor;

    public BackupRegistryExecutor(@Value("${service.registry.ip}") final String mainRegistryIp,
                                  @Value("${service.registry.port}") final String mainRegistryPort,
                                  @Value("${backup.update.interval}") final int backupUpdateInterval,
                                  final HealthCheckExecutor healthCheckExecutor) {
        this.restTemplate = new RestTemplate();
        this.backupUpdateInterval = backupUpdateInterval;
        this.backupExecutor = Executors.newSingleThreadScheduledExecutor();
        this.healthCheckExecutor = healthCheckExecutor;
        this.mainRegistryUri = UriComponentsBuilder.newInstance()
                .host(mainRegistryIp)
                .port(mainRegistryPort)
                .path(UPDATE_PATH)
                .toUriString();
        this.start();
    }

    public void start() {
        backupExecutor.scheduleWithFixedDelay(() -> {
            LOG.info("Retrieving services");
            final var response = restTemplate.getForEntity("http:" + mainRegistryUri, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.hasBody()) {
                final Map<String, Map<String, ServiceUrl>> services = response.getBody();
                healthCheckExecutor.setServices(services);
                services.forEach((k, v) -> LOG.info("retrieved service: {} with health {}", k, v));
            }
        }, 5, backupUpdateInterval, TimeUnit.SECONDS);
    }
}
