package com.FocusVault.anticheat;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * TimeTamperGuard monitors the system clock for manual changes by the user.
 * It uses Java's monotonic clock (System.nanoTime) to check for local drift,
 * and an NTP server to detect offline clock changes between app launches.
 */
public class TimeTamperGuard {

    private static final String NTP_SERVER = "time.google.com";
    private static final long MAX_ALLOWED_DRIFT_MS = 60000; // 1 minute drift allowance

    private final ScheduledExecutorService scheduler;
    
    // Monotonic clock baseline
    private long startNanoTime;
    // NTP baseline
    private long startNetworkTime;
    // System clock baseline (can be tampered with)
    private long startSystemTime;
    
    private boolean isTampered = false;

    public TimeTamperGuard() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TimeTamperGuard-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    public void initialize() {
        try {
            this.startNetworkTime = fetchNetworkTime();
            this.startNanoTime = System.nanoTime();
            this.startSystemTime = System.currentTimeMillis();
            System.out.println("TimeTamperGuard initialized online.");
        } catch (Exception e) {
            System.err.println("Failed to fetch initial network time, relying on monotonic clock only.");
            this.startNanoTime = System.nanoTime();
            this.startSystemTime = System.currentTimeMillis();
            this.startNetworkTime = -1; // Indicate offline mode
        }
    }

    public void startMonitoring() {
        // Check for tampering periodically (e.g., every 1 minute)
        scheduler.scheduleAtFixedRate(this::checkTimeIntegrity, 1, 1, TimeUnit.MINUTES);
    }

    public void stopMonitoring() {
        scheduler.shutdown();
    }

    public boolean isTampered() {
        return isTampered;
    }

    private void checkTimeIntegrity() {
        long currentSystemTime = System.currentTimeMillis();
        long currentNanoTime = System.nanoTime();
        
        long elapsedSystemMs = currentSystemTime - startSystemTime;
        long elapsedNanoMs = TimeUnit.NANOSECONDS.toMillis(currentNanoTime - startNanoTime);

        // Check 1: Monotonic clock vs System clock
        // If the system clock moved significantly faster or slower than the monotonic clock,
        // it means the user changed the OS time while the app was running.
        long drift = Math.abs(elapsedSystemMs - elapsedNanoMs);
        if (drift > MAX_ALLOWED_DRIFT_MS) {
            System.err.println("[AntiCheat] Time tampering detected! System clock drift: " + drift + " ms");
            isTampered = true;
            handleTamper();
            return;
        }

        // Check 2: Occasional NTP sync if we started online
        if (startNetworkTime != -1) {
            try {
                long currentNetworkTime = fetchNetworkTime();
                long elapsedNetworkMs = currentNetworkTime - startNetworkTime;
                
                long ntpDrift = Math.abs(elapsedNetworkMs - elapsedNanoMs);
                if (ntpDrift > MAX_ALLOWED_DRIFT_MS) {
                    System.err.println("[AntiCheat] NTP Time tampering detected! Drift: " + ntpDrift + " ms");
                    isTampered = true;
                    handleTamper();
                }
            } catch (Exception e) {
                // If offline, skip the network check silently
            }
        }
    }

    private long fetchNetworkTime() throws Exception {
        NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(3000); // 3-second timeout to avoid blocking
        client.open();
        try {
            InetAddress hostAddr = InetAddress.getByName(NTP_SERVER);
            TimeInfo info = client.getTime(hostAddr);
            info.computeDetails(); // compute offset/delay
            return info.getMessage().getReceiveTimeStamp().getTime();
        } finally {
            client.close();
        }
    }

    private void handleTamper() {
        // Here you would trigger the penalty (e.g., clear credits, lock the app, send alert)
        System.out.println("[AntiCheat] Executing tamper protocol...");
    }
}
