package meerkat.mango.service.registry;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ServiceUrl {

    @JsonProperty
    private final String ip;

    @JsonProperty
    private final String port;

    @JsonProperty
    private final String path;
}