package com.smartlogix.bff.client;

import com.smartlogix.bff.model.ValidateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "ms-auth", url = "${smartlogix.auth.url}")
public interface AuthClient {

    @GetMapping("/api/auth/validate")
    ValidateResponse validate(@RequestHeader("Authorization") String bearerToken);
}
