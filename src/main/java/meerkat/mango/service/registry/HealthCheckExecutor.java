package meerkat.mango.service.registry;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HealthCheckExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckExecutor.class);
    private static final int PARALLELISM_THRESHOLD = 3;
    private final ScheduledExecutorService executorService;
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<ServiceUrl>> registeredServices;

    @Value("${health.check.interval:60}")
    private int healthCheckInterval;
    private final ConcurrentHashMap<String, Boolean> healthChecks;
    private final RestTemplate restTemplate;

    public HealthCheckExecutor() {
        executorService = Executors.newScheduledThreadPool(1);
        registeredServices = new ConcurrentHashMap<>();
        healthChecks = new ConcurrentHashMap<>();
        restTemplate = new RestTemplate();
    }

    public boolean getHealth(final String service) {
        return healthChecks.containsKey(service) && healthChecks.get(service);
    }

    public void setService(final String service, final String ip, final String port) {
        if (registeredServices.containsKey(service)) {
            registeredServices.get(service).add(new ServiceUrl(ip, port));
        } else {
            registeredServices.put(service, new CopyOnWriteArrayList<>(List.of(new ServiceUrl(ip, port))));
        }
        start();
    }

    private void start() {
        executorService.scheduleWithFixedDelay(() -> {
            LOG.info("Health check started");
            registeredServices.forEachKey(PARALLELISM_THRESHOLD, (key) -> {
                healthChecks.put(key, verifyService(key));
            });
        }, 0, healthCheckInterval, TimeUnit.SECONDS);

    }

    private boolean verifyService(final String serviceName) {
        if (!registeredServices.containsKey(serviceName)) {
            return false;
        }

        final var serviceUrls = registeredServices.get(serviceName);
        final var uri = UriComponentsBuilder
                .newInstance()
                .path("/health");
        for (final ServiceUrl url : serviceUrls) {
            final var response = restTemplate.getForEntity("http:" + uri.host(url.getIp()).port(url.getPort()).toUriString(), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return true;
            }
        }
        return false;
    }

    @Data
    static class ServiceUrl {
        private final String ip;
        private final String port;
    }
}
