package ru.work.service.rest;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import ru.work.service.config.FeignClientConfiguration;

@FeignClient(name = "current-time-rest",
        url = "https://infragrid.v.network/wallet",
        configuration = FeignClientConfiguration.class)
public interface CurrentTimeRest {

    @PostMapping(value = "/getnodeinfo")
    ResponseEntity<String> getCurrentTime();

}
