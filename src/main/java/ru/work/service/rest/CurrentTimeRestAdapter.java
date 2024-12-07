package ru.work.service.rest;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.work.service.rest.response.CurrentTimeResponse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrentTimeRestAdapter {

    private final Gson gson;
    private final CurrentTimeRest timeRest;

    public LocalDateTime getCurrentTime() {
        CurrentTimeResponse response;
        try {
            response = gson.fromJson(timeRest.getCurrentTime().getBody(), CurrentTimeResponse.class);
        } catch (Exception e) {
            log.error("Failed get current time from rest: {}", e.getMessage());
            return null;
        }

        if (response == null) {
            log.error("Failed get current time from rest");
            return null;
        }

        if (CollectionUtils.isEmpty(response.getPeerList())) {
            log.error("Failed get current time from rest. Peer list is empty");
            return null;
        }


        List<Long> milliList =response.getPeerList().stream()
                .map(CurrentTimeResponse.Peer::getLastBlockUpdateTime)
                .sorted()
                .toList();

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(milliList.get(milliList.size() - 1)), ZoneId.systemDefault());
    }
}
