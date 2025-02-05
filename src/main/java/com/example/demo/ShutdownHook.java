package com.example.demo;

import javax.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class ShutdownHook {

    @Autowired
    private JobCoordinatorService jobCoordinatorService;

    @PreDestroy
    public void onShutdown() {
        jobCoordinatorService.releaseLease();
    }
}