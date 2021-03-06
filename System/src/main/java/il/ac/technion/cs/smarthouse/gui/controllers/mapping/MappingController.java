package il.ac.technion.cs.smarthouse.gui.controllers.mapping;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import il.ac.technion.cs.smarthouse.gui.controllers.SystemGuiController;
import il.ac.technion.cs.smarthouse.gui_controller.GuiController;
import il.ac.technion.cs.smarthouse.system.SystemCore;
import il.ac.technion.cs.smarthouse.system.SystemMode;
import il.ac.technion.cs.smarthouse.system.file_system.FileSystemEntries;
import il.ac.technion.cs.smarthouse.system.mapping_information.MappingInformation;
import il.ac.technion.cs.smarthouse.system.sensors.SensorLocation;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Pair;

/**
 * This is the controller of the house mapping
 * 
 * @author Sharon
 * @author Roy Shchory
 * @since 04-01-2017
 */
public class MappingController extends SystemGuiController {
    private static Logger log = LoggerFactory.getLogger(MappingController.class);
    private final Map<String, SensorInfoController> sensors = new HashMap<>();

    MappingInformation mappingInformaton;
    @FXML private VBox sensorsPaneList;
    @FXML private Canvas canvas;

    void addRoom(String roomName) {
        if(!mappingInformaton.addRoom(roomName)){
            final Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText("Room allready exists");
            alert.setContentText("Make sure to give a unique name to each room.");
            alert.showAndWait();
        }
        sensors.values().forEach(e -> e.updateRooms());
        drawMapping();
    }

    /*
     * (non-Javadoc)
     * 
     * @see il.ac.technion.cs.smarthouse.gui.controllers.SystemGuiController#
     * initialize(il.ac.technion.cs.smarthouse.system.SystemCore,
     * il.ac.technion.cs.smarthouse.gui_controller.GuiController,
     * il.ac.technion.cs.smarthouse.system.SystemMode, java.net.URL,
     * java.util.ResourceBundle)
     */
    @Override

    protected <T extends GuiController<SystemCore>> void initialize(SystemCore model, T parent, SystemMode m,
                    URL location, ResourceBundle b) {
        log.debug("initialized the map gui");

        Consumer<MappingInformation> c = x -> {
            this.mappingInformaton = x;
            drawMapping();
        };
        model.subscribeToMappingInformation(c);
        this.mappingInformaton = model.getHouse();

        canvas.setWidth(2000);
        canvas.setHeight(2000);

        addRoom(SensorLocation.UNDEFINED);

        log.debug("subscribed to sensors root");

        model.getFileSystem().subscribeWithNoNulls((p, l) -> {
            log.debug("map gui was notified on (path,val)=(" + p + "," + l + ")");
            final String commname = FileSystemEntries.COMMERCIAL_NAME.getPartFromPath(p),
                            id = FileSystemEntries.SENSOR_ID.getPartFromPath(p);
            if (l != null && mappingInformaton.getAllLocations().contains(l) && !sensors.containsKey(id))
                Platform.runLater(() -> {
                    try {
                        addSensor(id, commname);
                    } catch (final Exception e) {
                        log.warn("\n\tfailed to add sensor: " + id + " (received path " + p
                                        + ") to GuiMapping.\nGot execption" + e);
                    }
                });

        }, FileSystemEntries.SENSORS.buildPath());
        log.debug("finished initialized the map gui");

    }

    /**
     * Adds a sensor to the house mapping
     * 
     * @param id
     *            The ID of the sensor
     * @param commName
     *            The commercial name of the sensor
     * @throws Exception
     */
    public void addSensor(final String id, final String commName) throws Exception {
        if (sensors.containsKey(id))
            return;
        final SensorInfoController controller = createChildController("sensor_info.fxml");
        sensorsPaneList.getChildren().add(controller.getRootViewNode());

        if (mappingInformaton.getSensorsLocations().containsKey(id))
            controller.setLocation(mappingInformaton.getSensorsLocations().get(id));

        controller.setId(id).setName(commName).setMapController(this);
        sensors.put(id, controller);
    }

    /**
     * Updates a sensor location on the house mapping
     * 
     * @param id The id of the sensor
     * @param commName the commercial name of the sensor
     * @param l The location
     */
    public void updateSensorLocation(final String id,final String commName ,final String l) {
        if (mappingInformaton.getSensorsLocations().containsKey(id) && mappingInformaton.getLocationsContents()
                        .containsKey(mappingInformaton.getSensorsLocations().get(id))){
            List<Pair<String,String>> sList = mappingInformaton.getLocationsContents().get(mappingInformaton.getSensorsLocations().get(id));
            Pair<String,String> pToRemove = null;
            for(Pair<String,String> p:sList)
                if(p.getKey().equals(id))
                    pToRemove = p;
           if(pToRemove != null)
               sList.remove(pToRemove);
        }
        mappingInformaton.getSensorsLocations().put(id, l);

        if (!mappingInformaton.getLocationsContents().containsKey(l))
            mappingInformaton.getLocationsContents().put(l, new ArrayList<>());

        mappingInformaton.getLocationsContents().get(l).add(new Pair<>(id,commName));

        drawMapping();

    }

    /**
     * 
     * @return a list of all the locations in the house
     */
    public List<String> getAlllocations() {
        return mappingInformaton.getAllLocations();
    }
    
    private String getPath_alias(final String sensorId1 , String commName) {
        assert commName != null && sensorId1 != null;
        return FileSystemEntries.ALIAS.buildPath(commName, sensorId1);
    }

    private void drawMapping() {
        final GraphicsContext g = canvas.getGraphicsContext2D();

        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.setFont(new Font(14.0));
        g.setStroke(Color.BLACK);

        for (final Room room : mappingInformaton.getHouse().getRooms()) {
            g.strokeRect(room.x, room.y, room.width, room.height);
            g.strokeLine(room.x, room.y + 20, room.x + room.width, room.y + 20);
            g.setFill(Color.BLACK);
            g.fillText(room.location, room.x + 4, room.y + 15);
            if (mappingInformaton.getLocationsContents().containsKey(room.location)) {
                int dy = 20;

                for (final Pair<String,String> p : mappingInformaton.getLocationsContents().get(room.location)) {
                    String alias = this.getModel().getFileSystem().getData(getPath_alias(p.getKey(),p.getValue()));
                    g.fillText(" (" + alias + ")", room.x + 10, room.y + dy + 20);
                    dy += 20;
                }
            }
        }

        int xPlusRoom = mappingInformaton.calcxPlusRoom(), yPlusRoom = mappingInformaton.calcyPlusRoom();
        g.strokeRect(xPlusRoom, yPlusRoom, MappingInformation.getWidth(), MappingInformation.getHeight());
        g.setFont(new Font(45.0));
        g.fillText("+", xPlusRoom + 150, yPlusRoom + 85);
        g.setFill(Color.BLUE);
        g.setFont(new Font(84.0));
        canvas.setOnMouseClicked(mouseEvent -> {
            double x = mouseEvent.getX(), y = mouseEvent.getY();
            if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                mappingInformaton.getHouse().getRooms().forEach(r -> {
                    if (x > r.x && x < r.x + MappingInformation.getWidth() && y > r.y
                                    && y < r.y + MappingInformation.getHeight()) {
                        if (r.location.equals(SensorLocation.UNDEFINED))
                            return;
                        final TextInputDialog dialog = new TextInputDialog(r.location);
                        dialog.setTitle("Update Room");
                        dialog.setHeaderText("Config your smarthouse");
                        dialog.setContentText("Please enter new room name:");
                        final Optional<String> result = dialog.showAndWait();
                        if (!result.isPresent())
                            return;
                        final String name = result.get();
                        mappingInformaton.getAllLocations().add(name);
                        List<Pair<String,String>> currSensors = mappingInformaton.getLocationsContents().get(r.location);
                        mappingInformaton.getAllLocations().remove(r.location);
                        mappingInformaton.getLocationsContents().remove(r.location);
                        r.location = name;
                        sensors.values().forEach(e -> e.updateRooms());
                        for(Pair<String,String> p:currSensors)
                            sensors.get(p.getKey()).setLocation(name);
                    }
                });
                return;
            }

            if (x > xPlusRoom && x < xPlusRoom + MappingInformation.getWidth() && y > yPlusRoom
                            && y < yPlusRoom + MappingInformation.getHeight()) {
                final TextInputDialog dialog = new TextInputDialog("rooms name");
                dialog.setTitle("Create Room");
                dialog.setHeaderText("Config your smarthouse");
                dialog.setContentText("Please enter room name:");
                final Optional<String> result = dialog.showAndWait();
                if (!result.isPresent())
                    return;
                final String name = result.get();
                addRoom(name);
            }
        });

    }
}
