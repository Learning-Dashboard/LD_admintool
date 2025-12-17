package com.upc.ld_admintool.domain.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StrategicIndicatorsService {

    @Autowired
    private LDService ldService;

    public void fetchStrategicIndicators() {
        ldService.fetchStrategicIndicators();
    }
}
