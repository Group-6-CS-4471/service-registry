package meerkat.mango.service.registry;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor(force = true)
public class VerifyServiceResponse {

    @JsonProperty
    private final Map<String, ServiceUrl> serviceProviders;
}
