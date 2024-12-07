package ru.work.service.rest.response;

import lombok.Data;

import java.util.List;

@Data
public class CurrentTimeResponse {

    private Integer activeConnectCount;
    private Integer beginSyncNum;
    private String block;
    private ConfigNodeInfo configNodeInfo;
    private Integer currentConnectCount;
    private List<Peer> peerList;

    @Data
    public static class ConfigNodeInfo {
        private Integer listenPort;
        private String versionNum;
    }

    @Data
    public static class Peer {
        private Long connectTime;
        private String host;
        private Long lastBlockUpdateTime;
        private Integer nodeCount;
        private String nodeId;
    }
}
