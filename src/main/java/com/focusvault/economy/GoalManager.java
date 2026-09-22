package com.FocusVault.economy;

import java.util.ArrayList;
import java.util.List;

public class GoalManager {
    private final List<Goal> goals;

    public GoalManager() {
        this.goals = new ArrayList<>();
    }

    public void addGoal(String name, long targetMinutes) {
        goals.add(new Goal(name, targetMinutes));
    }

    public void removeGoal(Goal goal) {
        goals.remove(goal);
    }

    public List<Goal> getActiveGoals() {
        List<Goal> active = new ArrayList<>();
        for (Goal g : goals) {
            if (!g.isComplete()) {
                active.add(g);
            }
        }
        return active;
    }
}
