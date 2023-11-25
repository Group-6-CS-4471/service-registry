package meerkat.mango.service.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class HealthCheckExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckExecutor.class);
    private final int healthCheckInterval;
    private final AtomicBoolean healthCheckRunning;
    private final ScheduledExecutorService executorService;
    private final Map<String, Map<String, ServiceUrl>> registeredServices;
    private final RestTemplate restTemplate;

    @Autowired
    public HealthCheckExecutor(@Value("${health.check.interval:60}") final int healthCheckInterval,
                               @Value("${health.check.threads:1}") final int healthCheckThreads) {
        this.healthCheckInterval = healthCheckInterval;
        this.healthCheckRunning = new AtomicBoolean(false);
        executorService = Executors.newScheduledThreadPool(healthCheckThreads);
        registeredServices = new ConcurrentHashMap<>();
        restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.of(1, ChronoUnit.SECONDS))
                .setReadTimeout(Duration.of(1, ChronoUnit.SECONDS))
                .build();
    }

    public Map<String, ServiceUrl> getHealth(final String service) {
        start();
        return registeredServices.get(service);
    }

    Map<String, Map<String, ServiceUrl>> getServices() {
        return this.registeredServices;
    }

    void setService(final String service, final String serviceProvider, final String ip, final String port) {
        if (!registeredServices.containsKey(service)) {
            registeredServices.put(service, new ConcurrentHashMap<>());
        }
        registeredServices.get(service).put(serviceProvider, new ServiceUrl(ip, port));
        start();
    }

    void removeService(final String service) {
        registeredServices.remove(service);
        start();
    }

    public void setServices(final Map<String, Map<String, ServiceUrl>> services) {
        registeredServices.putAll(services);
        start();
    }

    private void start() {
        if (healthCheckRunning.get()) {
            return;
        }
        healthCheckRunning.set(true);
        executorService.scheduleWithFixedDelay(() -> {
            LOG.info("Health check started");
            registeredServices.keySet().forEach(this::verifyService);
        }, 0, healthCheckInterval, TimeUnit.SECONDS);
    }

    private void verifyService(final String serviceName) {
        if (!registeredServices.containsKey(serviceName)) {
            return;
        }

        final var serviceUrls = registeredServices.get(serviceName);
        final var uri = UriComponentsBuilder
                .newInstance()
                .path("/health");
        for (final var provider : serviceUrls.entrySet()) {
            final var url = provider.getValue();
            final var response = restTemplate.getForEntity("http:" + uri.host(url.getIp()).port(url.getPort()).toUriString(), String.class);
            LOG.info("get request complete");
            LOG.info(response.getStatusCode().toString());
            if (!response.getStatusCode().is2xxSuccessful()) {
                registeredServices.remove(provider.getKey());
            }
        }
    }
}
