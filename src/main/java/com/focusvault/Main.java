package com.FocusVault;

import com.FocusVault.anticheat.TimeTamperGuard;
import com.FocusVault.blocker.DomainBlocker;
import com.FocusVault.blocker.ProcessBlocker;
import com.FocusVault.economy.CreditManager;
import com.FocusVault.economy.GoalManager;
import com.FocusVault.economy.SessionTracker;
import com.FocusVault.ui.Dashboard;
import com.FocusVault.ui.OverlayWidget;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Main extends Application {

    private CreditManager creditManager;
    private SessionTracker sessionTracker;
    private GoalManager goalManager;
    
    private ProcessBlocker processBlocker;
    private DomainBlocker domainBlocker;
    private TimeTamperGuard timeTamperGuard;
    
    private Dashboard dashboard;
    private OverlayWidget overlay;

    @Override
    public void init() throws Exception {
        creditManager = new CreditManager();
        sessionTracker = new SessionTracker(creditManager);
        goalManager = new GoalManager();
        
        processBlocker = new ProcessBlocker();
        domainBlocker = new DomainBlocker();
        processBlocker.setDomainBlocker(domainBlocker);
        timeTamperGuard = new TimeTamperGuard();

        // Start Anti-cheat
        timeTamperGuard.initialize();
        timeTamperGuard.startMonitoring();
        
        // Start Economy
        sessionTracker.start();
        
        // Ensure UI updates run on JavaFX Application Thread
        sessionTracker.setOnUpdateCallback(() -> Platform.runLater(this::handleSessionUpdate));
    }

    private boolean currentlyBlocking = false;

    private void handleSessionUpdate() {
        // If the user is in Focus Mode, OR if the app was originally designed to block by default (idle), we block.
        // But per your previous request, it should only block when Focus is active.
        boolean shouldBlock = sessionTracker.isFocusActive();
        
        // Only apply or remove blocks if the state actually changes, to avoid spamming the OS and destroying network performance
        if (shouldBlock && !currentlyBlocking) {
            currentlyBlocking = true;
            applyBlocks();
        } else if (!shouldBlock && currentlyBlocking) {
            currentlyBlocking = false;
            removeBlocks();
        }
        
        if (overlay != null) overlay.updateUI();
        if (dashboard != null) dashboard.updateUI();
    }

    private void applyBlocks() {
        processBlocker.startBlocking();
        domainBlocker.applyBlocks();
    }

    private void removeBlocks() {
        processBlocker.stopBlocking();
        domainBlocker.removeBlocks();
    }

    @Override
    public void start(Stage primaryStage) {
        dashboard = new Dashboard(creditManager, sessionTracker, goalManager, processBlocker, domainBlocker);
        overlay = new OverlayWidget(creditManager, sessionTracker, dashboard);
        
        removeBlocks(); // Initial state is UNBLOCKED
        
        overlay.show();
        overlay.updateUI();
    }

    @Override
    public void stop() throws Exception {
        sessionTracker.stop();
        processBlocker.shutdown();
        domainBlocker.removeBlocks();
        timeTamperGuard.stopMonitoring();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
