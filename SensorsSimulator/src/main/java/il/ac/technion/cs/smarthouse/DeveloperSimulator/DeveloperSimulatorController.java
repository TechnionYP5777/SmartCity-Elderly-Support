package il.ac.technion.cs.smarthouse.DeveloperSimulator;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import il.ac.technion.cs.smarthouse.gui_controller.GuiController;
import il.ac.technion.cs.smarthouse.sensors.simulator.SensorsSimulator;
import il.ac.technion.cs.smarthouse.utils.JavaFxHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.scene.control.TextArea;

/**
 * @author Roy Shchory
 * @since Jun 17, 2017
 */
public class DeveloperSimulatorController extends SimulatorGuiController {

    @FXML AnchorPane mainPane;
    @FXML public TextArea sentConsole, receivedConsole;
    MainSensorListController listController;
    ConfigurationWindowController configController;
    SendMessageController messageController;

    private Consumer<String> getConsoleConsumer(TextArea console) {
        return x -> Platform.runLater(() -> console.appendText(x + "\n\n"));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * il.ac.technion.cs.smarthouse.gui_controller.GuiController#initialize(java
     * .lang.Object, il.ac.technion.cs.smarthouse.gui_controller.GuiController,
     * java.net.URL, java.util.ResourceBundle)
     */
    @Override
    protected <T extends GuiController<SensorsSimulator>> void initialize(SensorsSimulator model1, T parent1,
                    URL location, ResourceBundle b) {
        this.listController = createChildController(getClass().getResource("/sensor_config_list_ui.fxml"));
        this.configController = createChildController(getClass().getResource("/sensor_configuration_ui.fxml"));

        model1.addSentMsgLogger(getConsoleConsumer(sentConsole));
        model1.addInstructionReceivedLogger(getConsoleConsumer(receivedConsole));
        JavaFxHelper.placeNodeInPane(listController.getRootViewNode(), mainPane);
    }

    public void moveToConfiguration() {
        JavaFxHelper.placeNodeInPane(configController.getRootViewNode(), mainPane);
    }

    public void moveToSensorsList() {
        JavaFxHelper.placeNodeInPane(listController.getRootViewNode(), mainPane);
    }

    public void openMessageWindow() {
        if (getObservablePaths(this.getModel().getSensor(getSelectedSensor())).isEmpty()) {
            final Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText("Sensor has no fields");
            alert.setContentText("Make sure to configure the sensor before starting to stream.");
            alert.showAndWait();
            return;
        }

        this.messageController = createChildController(getClass().getResource("/message_ui.fxml"));
        messageController.loadFields();
        final Stage stage = new Stage();
        stage.setScene(new Scene(messageController.getRootViewNode(), 500, 200));
        stage.show();
    }

}
