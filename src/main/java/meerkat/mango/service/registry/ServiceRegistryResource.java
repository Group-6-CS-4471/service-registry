package meerkat.mango.service.registry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ServiceRegistryResource {

    private final HealthCheckExecutor healthCheckExecutor;

    @Autowired
    public ServiceRegistryResource(final HealthCheckExecutor healthCheckExecutor) {
        this.healthCheckExecutor = healthCheckExecutor;
    }

    @PutMapping(value = "/register/{service}", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody void registerService(@PathVariable("service") final String service,
                                              @RequestParam("ip") final String ip,
                                              @RequestParam("port") final String port) {
        if (ip == null || port == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ip and port need to be specified");
        }

        healthCheckExecutor.setService(service, ip, port);
    }

    @GetMapping(value = "/remove/{service}", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody void removeService(@PathVariable("service") final String service) {
        healthCheckExecutor.removeService(service);
    }

    @GetMapping(value = "/verify", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody boolean verifyService(@RequestParam final String service) {
        return healthCheckExecutor.getHealth(service);
    }
}
