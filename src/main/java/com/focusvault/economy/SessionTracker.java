package com.FocusVault.economy;

import com.FocusVault.anticheat.PresenceDetector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionTracker {
    private final CreditManager creditManager;
    private final ScheduledExecutorService scheduler;
    
    private boolean isFocusSessionActive = false;
    private Goal activeGoal = null;
    
    private long focusSessionStartTime;
    private long earnedCreditsThisSession = 0;
    private long totalSessionTimeMs = 0; // Total time spent in current session for live timer
    
    // AFK Threshold: 2 minutes
    private static final long AFK_THRESHOLD_MS = 2 * 60 * 1000;
    private boolean wasAFK = false;
    private long afkStartTime = 0;
    
    // Unlocking tracking
    private boolean isUnlockSessionActive = false;
    private long unlockSessionEndTime = 0;
    
    private Runnable onUpdateCallback;

    public SessionTracker(CreditManager creditManager) {
        this.creditManager = creditManager;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SessionTracker-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    public void setOnUpdateCallback(Runnable callback) {
        this.onUpdateCallback = callback;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    public synchronized void startFocusSession(Goal goal) {
        if (!isFocusSessionActive && !isUnlockSessionActive) {
            isFocusSessionActive = true;
            activeGoal = goal;
            focusSessionStartTime = System.currentTimeMillis();
            earnedCreditsThisSession = 0;
            totalSessionTimeMs = 0;
            triggerUpdate();
        }
    }

    public synchronized void stopFocusSession() {
        if (isFocusSessionActive) {
            isFocusSessionActive = false;
            activeGoal = null;
            long expectedCredits = totalSessionTimeMs / 60000;
            long creditsToGive = expectedCredits - earnedCreditsThisSession;
            if (creditsToGive > 0) {
                creditManager.addCredits(creditsToGive);
            }
            earnedCreditsThisSession = 0;
            totalSessionTimeMs = 0;
            triggerUpdate();
        }
    }

    public synchronized boolean startUnlockSession(long minutesToUnlock) {
        if (!isUnlockSessionActive && !isFocusSessionActive) {
            if (creditManager.spendCredits(minutesToUnlock)) {
                isUnlockSessionActive = true;
                unlockSessionEndTime = System.currentTimeMillis() + (minutesToUnlock * 60000);
                triggerUpdate();
                return true;
            }
        }
        return false;
    }

    public synchronized void stopUnlockSessionEarly() {
        if (isUnlockSessionActive) {
            long remainingMs = unlockSessionEndTime - System.currentTimeMillis();
            if (remainingMs > 60000) {
                long refundMinutes = remainingMs / 60000;
                creditManager.addCredits(refundMinutes);
            }
            isUnlockSessionActive = false;
            triggerUpdate();
        }
    }

    private synchronized void tick() {
        if (isFocusSessionActive) {
            long idleTime = PresenceDetector.getIdleTimeMillis();
            if (idleTime > AFK_THRESHOLD_MS) {
                if (!wasAFK) {
                    wasAFK = true;
                    afkStartTime = System.currentTimeMillis() - AFK_THRESHOLD_MS;
                }
            } else {
                if (wasAFK) {
                    long afkDuration = System.currentTimeMillis() - afkStartTime;
                    focusSessionStartTime += afkDuration;
                    wasAFK = false;
                }
                
                totalSessionTimeMs = System.currentTimeMillis() - focusSessionStartTime; 
                
                long expectedCredits = totalSessionTimeMs / 60000;
                long creditsToGive = expectedCredits - earnedCreditsThisSession;
                
                if (creditsToGive > 0) {
                    creditManager.addCredits(creditsToGive);
                    if (activeGoal != null) {
                        activeGoal.addCompletedMinutes(creditsToGive);
                    }
                    earnedCreditsThisSession += creditsToGive;
                }
            }

            triggerUpdate();
        }
        
        if (isUnlockSessionActive) {
            if (System.currentTimeMillis() >= unlockSessionEndTime) {
                isUnlockSessionActive = false;
            }
            triggerUpdate();
        }
    }

    public synchronized boolean isUnlockActive() { return isUnlockSessionActive; }
    public synchronized boolean isFocusActive() { return isFocusSessionActive; }
    public synchronized boolean isAFK() { return wasAFK; }
    public synchronized Goal getActiveGoal() { return activeGoal; }
    
    public synchronized String getUnlockTimeRemainingFormatted() {
        if (!isUnlockSessionActive) return "00:00";
        long remainingMs = Math.max(0, unlockSessionEndTime - System.currentTimeMillis());
        long minutes = (remainingMs / 1000) / 60;
        long seconds = (remainingMs / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public synchronized String getFocusTimeFormatted() {
        if (!isFocusSessionActive) return "00:00:00";
        long seconds = (totalSessionTimeMs / 1000) % 60;
        long minutes = (totalSessionTimeMs / (1000 * 60)) % 60;
        long hours = (totalSessionTimeMs / (1000 * 60 * 60));
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    private void triggerUpdate() {
        if (onUpdateCallback != null) {
            onUpdateCallback.run(); 
        }
    }
}
