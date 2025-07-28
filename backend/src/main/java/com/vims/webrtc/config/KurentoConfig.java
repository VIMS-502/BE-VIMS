package com.vims.webrtc.config;

import org.kurento.client.KurentoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KurentoConfig {

    @Value("${kurento.ws.uri:ws://localhost:8888/kurento}")
    private String kurentoWsUri;

    @Bean
    public KurentoClient kurentoClient() {
        return KurentoClient.create(kurentoWsUri);
    }
}