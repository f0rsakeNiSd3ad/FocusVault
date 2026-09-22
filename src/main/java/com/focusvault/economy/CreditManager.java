package com.FocusVault.economy;

import java.util.prefs.Preferences;

public class CreditManager {
    private static final String CREDITS_KEY = "FocusVault_credits_balance";
    private final Preferences prefs;
    private long credits; // Represents minutes

    public CreditManager() {
        // Automatically stores in Windows Registry or generic prefs backing store
        this.prefs = Preferences.userNodeForPackage(CreditManager.class);
        this.credits = prefs.getLong(CREDITS_KEY, 0);
    }

    public synchronized long getBalance() {
        return credits;
    }

    public synchronized void addCredits(long amount) {
        if (amount > 0) {
            credits += amount;
            save();
        }
    }

    public synchronized boolean spendCredits(long amount) {
        if (credits >= amount && amount > 0) {
            credits -= amount;
            save();
            return true;
        }
        return false;
    }

    private void save() {
        prefs.putLong(CREDITS_KEY, credits);
    }
}
