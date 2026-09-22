package com.FocusVault.economy;

import java.util.UUID;

public class Goal {
    private String id;
    private String name;
    private long targetMinutes;
    private long completedMinutes;

    public Goal(String name, long targetMinutes) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.targetMinutes = targetMinutes;
        this.completedMinutes = 0;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getTargetMinutes() { return targetMinutes; }
    public long getCompletedMinutes() { return completedMinutes; }
    
    public void addCompletedMinutes(long mins) { 
        this.completedMinutes += mins; 
    }
    
    public boolean isComplete() { 
        return completedMinutes >= targetMinutes; 
    }
    
    public long getRemainingMinutes() { 
        return Math.max(0, targetMinutes - completedMinutes); 
    }
    
    @Override
    public String toString() {
        long remain = getRemainingMinutes();
        long hrs = remain / 60;
        long mins = remain % 60;
        if (hrs > 0) {
            return name + " (" + hrs + "h " + mins + "m left)";
        }
        return name + " (" + mins + "m left)";
    }
}
