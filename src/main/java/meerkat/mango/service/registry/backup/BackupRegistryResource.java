package meerkat.mango.service.registry.backup;

import meerkat.mango.service.registry.HealthCheckExecutor;
import meerkat.mango.service.registry.ServiceUrl;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class BackupRegistryResource {

    private final HealthCheckExecutor healthCheckExecutor;

    public BackupRegistryResource() {
        healthCheckExecutor = new HealthCheckExecutor();
    }
    @PatchMapping(value = "/backup/update", produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody void updateBackupServiceSet(@RequestBody Map<String, ServiceUrl> services) {
        healthCheckExecutor.setServices(services);
    }
}
