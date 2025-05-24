package com.analyticalplatform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class ApiRateLimiterService {

    @Value("${alphavantage.api.rate-limit.calls-per-minute:5}")
    private int callsPerMinute;

    @Value("${alphavantage.api.rate-limit.calls-per-day:25}")
    private int callsPerDay;

    private final Semaphore daySemaphore;
    private final AtomicLong dailyCounter = new AtomicLong(0);
    private final AtomicLong lastMinuteCounter = new AtomicLong(0);

    private volatile LocalDateTime lastReset = LocalDateTime.now();
    private volatile LocalDateTime lastMinuteReset = LocalDateTime.now();

    public ApiRateLimiterService() {
        this.daySemaphore = new Semaphore(25, true);
    }

    public synchronized void acquirePermit() throws InterruptedException {
        log.debug("Trying to acquire API rate limit permit");

        LocalDateTime now = LocalDateTime.now();

        // Reset daily counter if it's a new day
        if (now.getDayOfYear() != lastReset.getDayOfYear() || now.getYear() != lastReset.getYear()) {
            log.info("Resetting daily API counter");
            daySemaphore.release(callsPerDay - daySemaphore.availablePermits());
            dailyCounter.set(0);
            lastReset = now;
        }

        // Reset minute counter if it's been more than a minute
        if (ChronoUnit.MINUTES.between(lastMinuteReset, now) >= 1) {
            log.debug("Resetting minute API counter");
            lastMinuteCounter.set(0);
            lastMinuteReset = now;
        }

        // Check daily limit
        if (dailyCounter.get() >= callsPerDay) {
            throw new InterruptedException("Daily API limit of " + callsPerDay + " calls reached");
        }

        // Check minute limit
        if (lastMinuteCounter.get() >= callsPerMinute) {
            long waitTime = 60 - ChronoUnit.SECONDS.between(lastMinuteReset, now);
            if (waitTime > 0) {
                log.info("Minute rate limit reached. Waiting {} seconds", waitTime);
                throw new InterruptedException("Minute rate limit reached. Please wait " + waitTime + " seconds");
            }
        }

        // Try to acquire daily permit
        boolean dayPermit = daySemaphore.tryAcquire(1, TimeUnit.SECONDS);

        if (!dayPermit) {
            throw new InterruptedException("Daily API limit reached");
        }

        // Increment counters
        dailyCounter.incrementAndGet();
        lastMinuteCounter.incrementAndGet();

        log.debug("Acquired API rate limit permit. Daily: {}/{}, Minute: {}/{}",
                dailyCounter.get(), callsPerDay, lastMinuteCounter.get(), callsPerMinute);
    }

    public long getDailyUsage() {
        return dailyCounter.get();
    }

    public long getMinuteUsage() {
        return lastMinuteCounter.get();
    }
}