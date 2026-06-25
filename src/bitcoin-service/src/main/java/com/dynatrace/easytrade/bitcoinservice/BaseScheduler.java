package com.dynatrace.easytrade.bitcoinservice;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class BaseScheduler {
    private static final Logger logger = LoggerFactory.getLogger(BaseScheduler.class);
    private ScheduledFuture<?> scheduledFuture;

    private String name;
    private int delayInSeconds;
    private int fixedRateInSeconds;
    protected final Random random = new Random();

    @Autowired
    ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() { start(); }

    @PreDestroy
    public void preDestroy() { stop(); }

    public BaseScheduler(String name, int delayInSeconds, int fixedRateInSeconds) {
        this.name = name;
        this.delayInSeconds = delayInSeconds;
        this.fixedRateInSeconds = fixedRateInSeconds;
    }

    public void start() {
        logger.info("Starting {} scheduler with a delay of {} and fixed rate of {}.", name.toLowerCase(), delayInSeconds, fixedRateInSeconds);

        if (scheduledFuture == null) {
            scheduledFuture = scheduler.scheduleAtFixedRate(this::run, delayInSeconds, fixedRateInSeconds, TimeUnit.SECONDS);
            logger.info("{} scheduler started.", name);
        } else {
            logger.info("{} scheduler was already started!", name);
        }
    }

    public void stop() {
        logger.info("Stopping {} scheduler.", name.toLowerCase());

        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            scheduledFuture = null;
            logger.info("{} scheduler stopped.", name);
        } else {
            logger.info("{} scheduler was already stopped!", name);
        }
    }

    protected abstract void run();

    protected void randomFixedRatePlusSleep() {
        try {
            Thread.sleep((fixedRateInSeconds + random.nextInt(fixedRateInSeconds)) * 1000L);
        } catch (InterruptedException e) {
            logger.error("Caught exception while sleeping: " + e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
}
