package il.ac.technion.cs.smarthouse.notification_center;

import javafx.scene.image.Image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.hansolo.enzo.notification.*;
import il.ac.technion.cs.smarthouse.system.services.alerts_service.EmergencyLevel;
import il.ac.technion.cs.smarthouse.utils.JavaFxHelper;

/**
 * The notification center manages the notifications to the user while using the
 * house system GUI
 * 
 * @author RON
 * @since 14-06-2017
 */
public enum NotificationsCenter {
    ;
    private static Logger log = LoggerFactory.getLogger(NotificationsCenter.class);

    private static boolean isEnabled;

    /**
     * This method sends a notification on a successful connection of a new
     * sensor
     * 
     * @param sensorCommercialName
     * @param sensorId
     * @param sensorAlias
     */
    public static void sendSensorConnectedNotification(final String sensorCommercialName, final String sensorId,
                    final String sensorAlias) {
        sendNotification("A new sensor has connected!", "Commercial name: " + sensorCommercialName + "\nMac id: "
                        + sensorId + "\nAlias: " + sensorAlias, Notification.INFO_ICON);
    }

    /**
     * This method sends a notification on an emergency alert
     * 
     * @param senderName
     * @param message
     * @param eLevel
     */
    public static void sendAlertNotifications(final String senderName, final String message,
                    final EmergencyLevel eLevel) {
        sendNotification("Alert! " + senderName, message + "\n" + eLevel.toPretty(), Notification.WARNING_ICON);
    }

    /**
     * This method sends a notification on a successful installation of a new
     * application
     * 
     * @param applicationName
     */
    public static void sendNewAppInstalled(final String applicationName) {
        sendNotification(applicationName, "Installed successfully", Notification.SUCCESS_ICON);
    }

    /**
     * This method sends a notification on a failed installation of a new
     * application
     * 
     * @param error
     */
    public static void sendAppFailedToInstall(final String error) {
        sendNotification("Failed installation", error, Notification.ERROR_ICON);
    }

    /**
     * This method is responsible on showing the actual notification
     * 
     * @param title
     * @param msg
     * @param icon
     */
    public static void sendNotification(final String title, final String msg, final Image icon) {
        if (!isEnabled)
            return;

        log.info("\n\tNotificationsCenter: send notification\n\tTitle: " + title + "\n\tMessage:\n\t\t"
                        + msg.replace("\n", "\n\t\t"));
        if (JavaFxHelper.isJavaFxThreadStarted())
            JavaFxHelper.surroundConsumerWithFx(
                            t -> Notification.Notifier.INSTANCE.notify(new Notification(title, msg, icon)))
                            .accept(null);
    }

    /**
     * This method enables the notification center
     */
    public static void enable() {
        isEnabled = true;
    }

    /**
     * This method closes the notifications center
     */
    public static void close() {
        try {
            Notification.Notifier.INSTANCE.stop();
        } catch (Exception e) {
            log.warn("\n\tCouldn't close Notification center");
        }
    }
}
