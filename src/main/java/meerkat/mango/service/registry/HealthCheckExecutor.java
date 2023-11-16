package meerkat.mango.service.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class HealthCheckExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckExecutor.class);
    private static final int PARALLELISM_THRESHOLD = 3;

    @Value("${health.check.interval:60}")
    private static int HEALTH_CHECK_INTERVAL;

    @Value("${backup.update.interval:60}")
    private static int BACKUP_UPDATE_INTERVAL;

    private final AtomicBoolean healthCheckRunning = new AtomicBoolean(false);
    private final ScheduledExecutorService executorService;
    private final ScheduledExecutorService backupExecutor;
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<ServiceUrl>> registeredServices;
    private final ConcurrentHashMap<String, Boolean> healthChecks;
    private final RestTemplate restTemplate;

    public HealthCheckExecutor() {
        executorService = Executors.newScheduledThreadPool(1);
        backupExecutor = Executors.newScheduledThreadPool(1);
        registeredServices = new ConcurrentHashMap<>();
        healthChecks = new ConcurrentHashMap<>();
        restTemplate = new RestTemplate();
    }

    public boolean isHealthCheckRunning() {
        return healthCheckRunning.get();
    }

    public boolean getHealth(final String service) {
        start();
        return healthChecks.containsKey(service) && healthChecks.get(service);
    }

    void setService(final String service, final String ip, final String port) {
        if (!registeredServices.containsKey(service)) {
            registeredServices.put(service, new CopyOnWriteArrayList<>());
        }
        registeredServices.get(service).add(new ServiceUrl(ip, port));
        start();
    }

    void removeService(final String service) {
        registeredServices.remove(service);
        start();
    }

    public void setServices(final Map<String, ServiceUrl> services) {
        services.forEach((k, v) -> {
            if (!registeredServices.containsKey(k)) {
                registeredServices.put(k, new CopyOnWriteArrayList<>());
            }
            registeredServices.get(k).add(new ServiceUrl(v.getIp(), v.getPort()));
        });
    }

    private void start() {
        if (healthCheckRunning.get()) {
            return;
        }
        healthCheckRunning.set(true);
        executorService.scheduleWithFixedDelay(() -> {
            LOG.info("Health check started");
            registeredServices.forEachKey(PARALLELISM_THRESHOLD, (key) -> {
                healthChecks.put(key, verifyService(key));
            });
        }, 0, HEALTH_CHECK_INTERVAL, TimeUnit.SECONDS);
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
            } else {
                registeredServices.get(serviceName).remove(url);
            }
        }
        return false;
    }
}
