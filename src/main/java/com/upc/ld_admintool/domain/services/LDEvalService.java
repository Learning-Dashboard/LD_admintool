package com.upc.ld_admintool.domain.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

@Service
public class LDEvalService {

    @Value("${ld.eval.url}") //http://learning-dashboard:5000/api
    private String ldEvalUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // -------------------------------
    // Trigger refresh del LDEval
    // -------------------------------
    public boolean triggerRefresh() {
        String url = ldEvalUrl + "/api/refresh";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            restTemplate.postForEntity(url, request, Void.class);
            return true;
        } catch (HttpClientErrorException e) {
            System.err.println("Error triggering LDEval refresh: " + e.getMessage());
            return false;
        }
    }
}
