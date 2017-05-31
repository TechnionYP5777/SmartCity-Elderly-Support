package il.ac.technion.cs.smarthouse.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.annotations.Expose;

import il.ac.technion.cs.smarthouse.system.applications.ApplicationsCore;
import il.ac.technion.cs.smarthouse.system.file_system.FileSystem;
import il.ac.technion.cs.smarthouse.system.file_system.FileSystemEntries;
import il.ac.technion.cs.smarthouse.system.file_system.FileSystemImpl;
import il.ac.technion.cs.smarthouse.system.sensors.SensorsLocalServer;
import il.ac.technion.cs.smarthouse.system.services.ServiceManager;
import il.ac.technion.cs.smarthouse.system.user_information.UserInformation;

/**
 * Hold the databases of the smart house, and allow sensors and applications to
 * store and read information about the changes in the environment
 */
public class SystemCore implements Savable {
    private static Logger log = LoggerFactory.getLogger(SystemCore.class);

    public final ServiceManager serviceManager = new ServiceManager(this);
    public final DatabaseHandler databaseHandler = new DatabaseHandler();
    public final SensorsLocalServer sensorsHandler = new SensorsLocalServer(databaseHandler);
    @Expose private final ApplicationsCore applicationsHandler = new ApplicationsCore(this);
    private final FileSystem fileSystem = new FileSystemImpl();
    protected UserInformation user;
    private boolean userInitialized;

    public void initializeSystemComponents() {
        System.out.println("Initializing system components...");
        initFileSystemListeners();
        new Thread(sensorsHandler).start();
    }

    public UserInformation getUser() {
        return user;
    }

    public void initializeUser(final String name, final String id, final String phoneNumber, final String homeAddress) {
        user = new UserInformation(name, id, phoneNumber, homeAddress);
        userInitialized = true;
    }

    public boolean isUserInitialized() {
        return userInitialized;
    }

    public void shutdown() {
        sensorsHandler.closeSockets();
    }

    public ApplicationsCore getSystemApplicationsHandler() {
        return applicationsHandler;
    }

    public Dispatcher getSystemDispatcher() {
        return null;// TODO: stub for now
    }

    public DatabaseHandler getSystemDatabaseHandler() {
        return databaseHandler;
    }

    public ServiceManager getSystemServiceManager() {
        return serviceManager;
    }

    public FileSystem getFileSystem() {
        return fileSystem;
    }

    public void initFileSystemListeners() {
        fileSystem.subscribe((path, data) -> {
            fileSystem.sendMessage(this.toJsonString(), FileSystemEntries.SYSTEM_DATA_IMAGE.buildPath());
            log.info("System interrupt: SAME_ME: " + this.toJsonString());
        }, FileSystemEntries.SAVEME.buildPath());

        // TODO: inbal
    }

}
