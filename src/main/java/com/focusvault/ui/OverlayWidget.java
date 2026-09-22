package com.FocusVault.ui;

import com.FocusVault.economy.CreditManager;
import com.FocusVault.economy.Goal;
import com.FocusVault.economy.SessionTracker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class OverlayWidget {
    private final Stage stage;
    private final CreditManager creditManager;
    private final SessionTracker sessionTracker;
    
    private double xOffset = 0;
    private double yOffset = 0;
    
    private VBox root;
    private Label balanceLabel;
    private Label statusLabel;
    private Label timerLabel;

    public OverlayWidget(CreditManager creditManager, SessionTracker sessionTracker, Dashboard dashboard) {
        this.creditManager = creditManager;
        this.sessionTracker = sessionTracker;

        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        root = new VBox(8);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER_LEFT);
        
        // Premium class for background and borders
        root.getStyleClass().add("overlay-card");
        root.getStyleClass().add("overlay-glow-locked"); // Initial glow

        // Top Row: Logo/Name and Status Indicator
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("FocusVault Wallet");
        titleLabel.setTextFill(Color.web("#a1a1aa"));
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        statusLabel = new Label("LOCKED");
        statusLabel.setTextFill(Color.web("#ef4444")); // Red
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
        
        topRow.getChildren().addAll(titleLabel, spacer, statusLabel);

        // Middle: Balance
        balanceLabel = new Label(creditManager.getBalance() + " CR");
        balanceLabel.setTextFill(Color.WHITE);
        balanceLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));

        // Bottom: Timer
        timerLabel = new Label("Secure System");
        timerLabel.setTextFill(Color.web("#a1a1aa"));
        timerLabel.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));

        root.getChildren().addAll(topRow, balanceLabel, timerLabel);

        // Drag functionality
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
        
        // Open Dashboard on double click
        root.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                dashboard.show();
            }
        });

        Scene scene = new Scene(root, 220, 110);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        
        stage.setX(100);
        stage.setY(100);
    }

    public void updateUI() {
        balanceLabel.setText(creditManager.getBalance() + " CR");
        
        // Reset glow
        root.getStyleClass().removeAll("overlay-glow-locked", "overlay-glow-unlocked", "overlay-glow-focus", "overlay-glow-afk");

        if (sessionTracker.isUnlockActive()) {
            statusLabel.setText("UNLOCKED");
            statusLabel.setTextFill(Color.web("#22c55e")); // Green
            timerLabel.setText(sessionTracker.getUnlockTimeRemainingFormatted() + " remaining");
            root.getStyleClass().add("overlay-glow-unlocked");
            
        } else if (sessionTracker.isFocusActive()) {
            if (sessionTracker.isAFK()) {
                statusLabel.setText("FOCUS (PAUSED)");
                statusLabel.setTextFill(Color.web("#eab308")); // Yellow
                root.getStyleClass().add("overlay-glow-afk");
            } else {
                statusLabel.setText("ACTIVE FOCUS");
                statusLabel.setTextFill(Color.web("#3b82f6")); // Blue
                root.getStyleClass().add("overlay-glow-focus");
            }
            
            Goal goal = sessionTracker.getActiveGoal();
            if (goal != null) {
                timerLabel.setText("Goal: " + goal.getRemainingMinutes() + "m left");
            } else {
                timerLabel.setText("Timer: " + sessionTracker.getFocusTimeFormatted());
            }
            
        } else {
            statusLabel.setText("LOCKED");
            statusLabel.setTextFill(Color.web("#ef4444")); // Red
            timerLabel.setText("Secure System");
            root.getStyleClass().add("overlay-glow-locked");
        }
    }

    public void show() {
        stage.show();
    }
}
