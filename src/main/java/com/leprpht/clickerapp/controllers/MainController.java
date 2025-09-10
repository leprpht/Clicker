package com.leprpht.clickerapp.controllers;

import com.leprpht.clickerapp.utils.MouseTracker;
import com.leprpht.clickerapp.utils.ScriptExecutor;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainController {
    @FXML
    public Button startButton;
    @FXML
    private Label mouseX;
    @FXML
    private Label mouseY;
    @FXML
    private TextArea scriptArea;
    @FXML
    private Circle statusCircle;

    private final ScriptExecutor executor = new ScriptExecutor();

    private ScheduledExecutorService scheduler;


    public void initialize() {
        MouseTracker.Track(this);
        checkStatus();
    }

    /**
     * Updates the mouse coordinate labels in the UI.
     *
     * @param x current mouse X coordinate
     * @param y current mouse Y coordinate
     */
    @FXML
    public void mousePosition(int x, int y) {
        Platform.runLater(() -> {
            mouseX.setText("Mouse X: " + x);
            mouseY.setText("Mouse Y: " + y);
        });
    }

    /**
     * Checks the status of the script executor and updates the UI accordingly.
     */
    public void checkStatus() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            boolean running = executor.isRunning();
            Platform.runLater(() -> {
                if (running) {
                    statusCircle.setFill(Paint.valueOf("#449D44"));
                    startButton.setDisable(true);
                } else {
                    statusCircle.setFill(Paint.valueOf("#C9302C"));
                    startButton.setDisable(false);
                }
            });
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Handles script insertion from button clicks.
     * <p>
     * This method dynamically generates a script line based on the key combination
     * encoded in the clicked button's {@code userData} attribute. The format should be
     * a plus-separated string of keys, e.g. {@code "CTRL+C"} or {@code "CTRL+ALT+DELETE"}.
     * </p>
     *
     * <p>
     * The generated script format follows this pattern:
     * <pre>
     * KEYPRESS [modifier1]
     * KEYPRESS [modifier2]
     * ...
     * KEYCLICK [main key]
     * KEYRELEASE [modifier2]
     * KEYRELEASE [modifier1]
     * </pre>
     * </p>
     *
     * @param event the action event triggered by a button click
     */
    @FXML
    public void handleScriptInsert(ActionEvent event) {
        Button source = (Button) event.getSource();
        String userData = (String) source.getUserData();
        String[] keys = userData.split("\\+");
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < keys.length - 1; i++) {
            builder.append("PRESS ").append(keys[i]).append("\n");
        }

        builder.append("CLICK ").append(keys[keys.length - 1]).append("\n");

        for (int i = keys.length - 2; i >= 0; i--) {
            builder.append("RELEASE ").append(keys[i]).append("\n");
        }

        int caretPosition = scriptArea.getCaretPosition();
        scriptArea.insertText(caretPosition, builder.toString());
    }

    @FXML
    public void handleReset() {
        scriptArea.clear();
    }

    /**
     * Opens a file chooser dialog to select a text file,
     * reads its content and inserts it into the script area.
     */
    @FXML
    private void handleUploadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a Text File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        Stage stage = (Stage) scriptArea.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                String content = Files.readString(selectedFile.toPath());
                scriptArea.setText(content);
            } catch (IOException e) {
                scriptArea.setText("Error reading file.");
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends the current script area content to the script executor.
     */
    @FXML
    public void handleStart() {
        scriptArea.setText(scriptArea.getText().toUpperCase());
        String script = scriptArea.getText();
        new Thread(() -> executor.execute(script)).start();
    }

    /**
     * Stops the script execution, mouse tracking and status checker.
     */
    public void shutdown() {
        executor.halt();
        MouseTracker.stop();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            try {
                if (!scheduler.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    System.err.println("Scheduler did not terminate in time.");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}