package meerkat.mango.service.registry;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HealthCheckExecutor {
    private static int PARALLELISM_THRESHOLD = 3;
    private ExecutorService executorService;
    private final ConcurrentHashMap<String, List<String>> registeredServices;
    private final ConcurrentHashMap<String, Boolean> healthChecks;
    private final RestTemplate restTemplate;

    public HealthCheckExecutor() {
        executorService = Executors.newSingleThreadExecutor();
        registeredServices = new ConcurrentHashMap<>();
        healthChecks = new ConcurrentHashMap<>();
        restTemplate = new RestTemplate();
    }

    public void start() {

        registeredServices.forEachKey(PARALLELISM_THRESHOLD, (key) -> {
            executorService.submit(() -> {
                healthChecks.put(key, verifyService(key));
            });
        });

    }

    private boolean verifyService(final String serviceName) {
        if (!registeredServices.containsKey(serviceName)) {
            return false;
        }

        final var serviceUrls = registeredServices.get(serviceName);
        final var uri = UriComponentsBuilder
                .newInstance()
                .path("/health");
        for (final String url : serviceUrls) {
            final var response = restTemplate.getForEntity(uri.host(url).toUriString(), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return true;
            }
        }
        return false;
    }
}
