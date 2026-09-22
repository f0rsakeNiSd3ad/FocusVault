package com.FocusVault.ui;

import com.FocusVault.blocker.DomainBlocker;
import com.FocusVault.blocker.ProcessBlocker;
import com.FocusVault.blocker.ProcessHelper;
import com.FocusVault.economy.CreditManager;
import com.FocusVault.economy.Goal;
import com.FocusVault.economy.GoalManager;
import com.FocusVault.economy.SessionTracker;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

public class Dashboard {
    private final Stage stage;
    private final CreditManager creditManager;
    private final SessionTracker sessionTracker;
    private final GoalManager goalManager;
    private final ProcessBlocker processBlocker;
    private final DomainBlocker domainBlocker;

    private Label creditsLabel;
    private Button startFocusBtn;
    
    // Lists for Blockers
    private ObservableList<String> activeBlockedProcesses;
    private ObservableList<String> activeBlockedDomains;

    public Dashboard(CreditManager creditManager, SessionTracker sessionTracker, GoalManager goalManager, ProcessBlocker processBlocker, DomainBlocker domainBlocker) {
        this.creditManager = creditManager;
        this.sessionTracker = sessionTracker;
        this.goalManager = goalManager;
        this.processBlocker = processBlocker;
        this.domainBlocker = domainBlocker;
        
        activeBlockedProcesses = FXCollections.observableArrayList(processBlocker.getBlacklistedProcesses());
        activeBlockedDomains = FXCollections.observableArrayList(domainBlocker.getBlacklistedDomains());

        stage = new Stage();
        stage.setTitle("FocusVault Dashboard");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // --- Sidebar ---
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(200);

        javafx.scene.image.ImageView logo = new javafx.scene.image.ImageView(new javafx.scene.image.Image(getClass().getResourceAsStream("/logo.jpg")));
        logo.setFitWidth(150);
        logo.setPreserveRatio(true);
        
        Button focusTabBtn = new Button("Focus & Goals");
        focusTabBtn.getStyleClass().addAll("sidebar-btn", "active");
        
        Button blockerTabBtn = new Button("Blocker Settings");
        blockerTabBtn.getStyleClass().add("sidebar-btn");
        
        Button exitBtn = new Button("Exit FocusVault");
        exitBtn.getStyleClass().add("sidebar-btn");
        exitBtn.setStyle("-fx-text-fill: #f44336;");
        exitBtn.setOnAction(e -> javafx.application.Platform.exit());

        sidebar.getChildren().addAll(logo, focusTabBtn, blockerTabBtn, new Region(), exitBtn);
        VBox.setVgrow(sidebar.getChildren().get(3), Priority.ALWAYS); // Spacer
        root.setLeft(sidebar);

        // --- Content Area ---
        StackPane contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");
        
        VBox focusView = buildFocusView();
        VBox blockerView = buildBlockerView();
        
        // Fix: Explicitly hide the inactive view to prevent visual bleed-through in the StackPane
        focusView.setVisible(true);
        blockerView.setVisible(false);
        
        contentArea.getChildren().addAll(blockerView, focusView); // focusView on top initially
        root.setCenter(contentArea);

        // Sidebar Navigation Logic
        focusTabBtn.setOnAction(e -> {
            focusView.setVisible(true);
            blockerView.setVisible(false);
            focusView.toFront();
            focusTabBtn.getStyleClass().add("active");
            blockerTabBtn.getStyleClass().remove("active");
        });
        
        blockerTabBtn.setOnAction(e -> {
            blockerView.setVisible(true);
            focusView.setVisible(false);
            blockerView.toFront();
            blockerTabBtn.getStyleClass().add("active");
            focusTabBtn.getStyleClass().remove("active");
        });

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
    }

    private VBox buildFocusView() {
        VBox view = new VBox(20);
        
        VBox card = new VBox(15);
        card.getStyleClass().add("card");
        
        creditsLabel = new Label(creditManager.getBalance() + " CR");
        creditsLabel.getStyleClass().add("header-label");
        
        Label subLabel = new Label("Available Credits");
        subLabel.getStyleClass().add("sub-label");
        
        startFocusBtn = new Button("Start Focus Session");
        startFocusBtn.getStyleClass().add("primary-btn");
        startFocusBtn.setPrefWidth(200);
        startFocusBtn.setOnAction(e -> handleFocusToggle());
        
        VBox unlockBox = new VBox(10);
        Label sliderLabel = new Label("Unlock Time: 15 minutes (15 CR)");
        sliderLabel.getStyleClass().add("sub-label");

        Slider timeSlider = new Slider(1, 60, 15);
        timeSlider.setShowTickMarks(true);
        timeSlider.setShowTickLabels(true);
        timeSlider.setMajorTickUnit(15);
        timeSlider.setBlockIncrement(1);
        
        timeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            sliderLabel.setText(String.format("Unlock Time: %d minutes (%d CR)", newVal.intValue(), newVal.intValue()));
        });
        
        HBox unlockActionBox = new HBox(10);
        Button unlockBtn = new Button("Spend Credits & Unlock");
        unlockBtn.getStyleClass().add("action-btn");
        unlockBtn.setOnAction(e -> {
            int minutes = (int) timeSlider.getValue();
            sessionTracker.startUnlockSession(minutes);
        });
        
        Button cancelUnlock = new Button("End Unlock Early");
        cancelUnlock.getStyleClass().add("danger-btn");
        cancelUnlock.setOnAction(e -> sessionTracker.stopUnlockSessionEarly());
        
        unlockActionBox.getChildren().addAll(unlockBtn, cancelUnlock);
        unlockBox.getChildren().addAll(sliderLabel, timeSlider, unlockActionBox);
        
        card.getChildren().addAll(creditsLabel, subLabel, startFocusBtn, new Separator(), new Label("Spend Credits", subLabel), unlockBox);
        
        // Goals Section
        VBox goalsCard = new VBox(15);
        goalsCard.getStyleClass().add("card");
        Label goalsTitle = new Label("Create a Specialized Goal");
        goalsTitle.getStyleClass().add("header-label");
        goalsTitle.setStyle("-fx-font-size: 18px;");
        
        HBox createGoalBox = new HBox(10);
        TextField goalNameInput = new TextField();
        goalNameInput.setPromptText("Goal Name (e.g., Study)");
        goalNameInput.getStyleClass().add("text-input");
        
        TextField goalHoursInput = new TextField();
        goalHoursInput.setPromptText("Hours");
        goalHoursInput.setPrefWidth(60);
        goalHoursInput.getStyleClass().add("text-input");
        
        Button addGoalBtn = new Button("Add Goal");
        addGoalBtn.getStyleClass().add("action-btn");
        addGoalBtn.setOnAction(e -> {
            try {
                long hours = Long.parseLong(goalHoursInput.getText());
                if (!goalNameInput.getText().isEmpty() && hours > 0) {
                    goalManager.addGoal(goalNameInput.getText(), hours * 60);
                    goalNameInput.clear();
                    goalHoursInput.clear();
                }
            } catch (NumberFormatException ex) {}
        });
        
        createGoalBox.getChildren().addAll(goalNameInput, goalHoursInput, addGoalBtn);
        goalsCard.getChildren().addAll(goalsTitle, createGoalBox);
        
        view.getChildren().addAll(card, goalsCard);
        return view;
    }

    private void handleFocusToggle() {
        if (sessionTracker.isFocusActive()) {
            sessionTracker.stopFocusSession();
            startFocusBtn.setText("Start Focus Session");
            startFocusBtn.getStyleClass().remove("danger-btn");
            startFocusBtn.getStyleClass().add("primary-btn");
        } else {
            List<Goal> activeGoals = goalManager.getActiveGoals();
            if (activeGoals.isEmpty()) {
                sessionTracker.startFocusSession(null);
                updateStartButton();
            } else {
                // Show goal selection dialog
                ChoiceDialog<Goal> dialog = new ChoiceDialog<>(activeGoals.get(0), activeGoals);
                dialog.setTitle("Select Focus Goal");
                dialog.setHeaderText("Choose a goal for this session, or cancel for a normal session.");
                dialog.setContentText("Active Goals:");
                
                dialog.showAndWait().ifPresentOrElse(
                    goal -> sessionTracker.startFocusSession(goal),
                    () -> sessionTracker.startFocusSession(null) // Cancel defaults to normal session
                );
                updateStartButton();
            }
        }
    }

    private void updateStartButton() {
        if (sessionTracker.isFocusActive()) {
            startFocusBtn.setText("Stop Focus Session");
            startFocusBtn.getStyleClass().remove("primary-btn");
            startFocusBtn.getStyleClass().add("danger-btn");
        } else {
            startFocusBtn.setText("Start Focus Session");
            startFocusBtn.getStyleClass().remove("danger-btn");
            startFocusBtn.getStyleClass().add("primary-btn");
        }
    }

    private VBox buildBlockerView() {
        VBox view = new VBox(20);
        
        // App Blocker Card
        VBox appCard = new VBox(15);
        appCard.getStyleClass().add("card");
        
        Label appTitle = new Label("Block Applications");
        appTitle.getStyleClass().add("header-label");
        appTitle.setStyle("-fx-font-size: 18px;");
        
        HBox addAppBox = new HBox(10);
        ComboBox<ProcessHelper.ProcessInfo> runningAppsCombo = new ComboBox<>();
        runningAppsCombo.setPromptText("Select a running application...");
        runningAppsCombo.setPrefWidth(300);
        runningAppsCombo.setOnShowing(e -> {
            runningAppsCombo.setItems(FXCollections.observableArrayList(ProcessHelper.getRunningGUIApplications()));
        });
        
        Button addAppBtn = new Button("Block App");
        addAppBtn.getStyleClass().add("action-btn");
        addAppBtn.setOnAction(e -> {
            ProcessHelper.ProcessInfo selected = runningAppsCombo.getValue();
            if (selected != null && !activeBlockedProcesses.contains(selected.exeName)) {
                processBlocker.addBlacklistedProcess(selected.exeName);
                activeBlockedProcesses.add(selected.exeName);
            }
        });
        
        addAppBox.getChildren().addAll(runningAppsCombo, addAppBtn);
        
        ListView<String> appList = new ListView<>(activeBlockedProcesses);
        appList.getStyleClass().add("list-view");
        appList.setPrefHeight(150);
        
        Button removeAppBtn = new Button("Unblock Selected");
        removeAppBtn.getStyleClass().add("danger-btn");
        removeAppBtn.setOnAction(e -> {
            String selected = appList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                processBlocker.removeBlacklistedProcess(selected);
                activeBlockedProcesses.remove(selected);
            }
        });
        
        appCard.getChildren().addAll(appTitle, new Label("Select from currently running apps:"), addAppBox, appList, removeAppBtn);
        
        // Domain Blocker Card
        VBox domainCard = new VBox(15);
        domainCard.getStyleClass().add("card");
        
        Label domainTitle = new Label("Block Websites");
        domainTitle.getStyleClass().add("header-label");
        domainTitle.setStyle("-fx-font-size: 18px;");
        
        HBox addDomainBox = new HBox(10);
        TextField domainInput = new TextField();
        domainInput.setPromptText("e.g. youtube.com");
        domainInput.getStyleClass().add("text-input");
        
        Button addDomainBtn = new Button("Block Website");
        addDomainBtn.getStyleClass().add("action-btn");
        addDomainBtn.setOnAction(e -> {
            String domain = domainInput.getText().trim().toLowerCase();
            if (!domain.isEmpty() && !activeBlockedDomains.contains(domain)) {
                domainBlocker.addDomain(domain);
                activeBlockedDomains.add(domain);
                // Immediately apply changes to hosts file
                domainBlocker.applyBlocks(); 
                domainInput.clear();
            }
        });
        addDomainBox.getChildren().addAll(domainInput, addDomainBtn);
        
        ListView<String> domainList = new ListView<>(activeBlockedDomains);
        domainList.getStyleClass().add("list-view");
        domainList.setPrefHeight(150);
        
        Button removeDomainBtn = new Button("Unblock Selected");
        removeDomainBtn.getStyleClass().add("danger-btn");
        removeDomainBtn.setOnAction(e -> {
            String selected = domainList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                domainBlocker.removeDomain(selected);
                activeBlockedDomains.remove(selected);
                domainBlocker.applyBlocks();
            }
        });
        
        domainCard.getChildren().addAll(domainTitle, addDomainBox, domainList, removeDomainBtn);
        
        view.getChildren().addAll(appCard, domainCard);
        return view;
    }

    public void updateUI() {
        creditsLabel.setText(creditManager.getBalance() + " CR");
        updateStartButton();
    }

    public void show() {
        stage.show();
        stage.toFront();
    }
}
