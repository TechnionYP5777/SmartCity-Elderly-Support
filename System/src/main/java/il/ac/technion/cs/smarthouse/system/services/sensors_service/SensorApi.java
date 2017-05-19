package il.ac.technion.cs.smarthouse.system.services.sensors_service;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import il.ac.technion.cs.smarthouse.networking.messages.Message;
import il.ac.technion.cs.smarthouse.networking.messages.MessageType;
import il.ac.technion.cs.smarthouse.system.SensorLocation;
import il.ac.technion.cs.smarthouse.system.SystemCore;
import il.ac.technion.cs.smarthouse.system.exceptions.SensorNotFoundException;
import il.ac.technion.cs.smarthouse.utils.JavaFxHelper;

/**
 * An API class for the developers, that allows interactions with a specific
 * sensor.
 * 
 * @author RON
 * @author Inbal Zukerman
 * @since 07-04-2017
 * @param <T>
 *            the sensor's messages will be deserialize into this class. T must
 *            extend SensorData
 */
public final class SensorApi<T extends SensorData> {
	private static Logger log = LoggerFactory.getLogger(SensorApi.class);
	private static String LOG_MSG_RUNTIME_THROW = "SensorLostRuntimeException is being thrown. This is unexpected!";
	private static String LOG_MSG_SENSOR_NOT_FOUND = LOG_MSG_RUNTIME_THROW
			+ " This is thrown because SensorNotFoundException was received";

	private final SystemCore systemCore;
	private final String sensorId;
	private final Class<T> sensorDataClass;

	/**
	 * This c'tor should be used only by the {@link SensorsManager}
	 * 
	 * @param systemCore
	 *            a reference to the system's core
	 * @param sensorId
	 *            The ID of the sensor, returned from
	 *            inquireAbout(sensorCommercialName)
	 * @param sensorDataClass
	 *            The class representing the sensor being listened to, defined
	 *            by the developer
	 */
	SensorApi(final SystemCore systemCore, final String sensorId, final Class<T> sensorDataClass) {
		this.systemCore = systemCore;
		this.sensorId = sensorId;
		this.sensorDataClass = sensorDataClass;
	}

	/**
	 * Queries the system for the sensor's current location
	 * 
	 * @return the sensors location
	 */
	public SensorLocation getSensorLocation() {
		try {
			return systemCore.databaseHandler.getSensorLocation(this.sensorId);
		} catch (final SensorNotFoundException e) {
			log.error(LOG_MSG_SENSOR_NOT_FOUND, e);
			throw new SensorLostRuntimeException(e);
		}
	}

	/**
	 * Wraps the <code>functionToRun</code> Consumer with helpful wrappers.
	 * <p>
	 * 1. A wrapper that sets the <code>sensorData</code>'s
	 * <code>sensorLocation</code>
	 * <p>
	 * 2. Surrounds the given function with a Platform.runLater, if
	 * <code>runOnFx == true</code>
	 * 
	 * @param functionToRun
	 *            a Consumer that receives <code>SensorData sensorData</code>
	 *            and operates on it.
	 * @param runOnFx
	 * @return the modified consumer [[SuppressWarningsSpartan]]
	 */
	private Consumer<String> generateSensorListener(final Consumer<T> functionToRun, final boolean runOnFx) {
		final Consumer<T> functionToRunWrapper1 = sensorData -> {

			sensorData.sensorLocation = getSensorLocation();
			functionToRun.accept(sensorData);
		}, functionToRunWrapper2 = !runOnFx ? functionToRunWrapper1
				: JavaFxHelper.surroundConsumerWithFx(functionToRunWrapper1);
		return jsonData -> functionToRunWrapper2.accept(new Gson().fromJson(jsonData, sensorDataClass));
	}

	/**
	 * Allows registration to a sensor. on update, the data will be given to the
	 * consumer for farther processing
	 * 
	 * @param functionToRun
	 *            A consumer that will receive a seneorClass object initialized
	 *            with the newest data from the sensor
	 * @throws SensorLostRuntimeException
	 */
	public void subscribe(final Consumer<T> functionToRun) throws SensorLostRuntimeException, SensorNotFoundException {
		systemCore.databaseHandler.addListener(sensorId, generateSensorListener(functionToRun, true));
	}

	/**
	 * Send a message to a sensor.
	 * 
	 * @param instruction
	 *            the message that the sensor will receive
	 * @throws SensorLostRuntimeException
	 */
	public void instruct(final String instruction) throws SensorLostRuntimeException {
		if (!systemCore.databaseHandler.sensorExists(sensorId)) {
			log.error(LOG_MSG_RUNTIME_THROW + " This is because " + sensorId + " Doesn't exist");
			throw new SensorLostRuntimeException(null);
		}
		systemCore.sensorsHandler.sendInstruction(Message.createMessage(sensorId, MessageType.UPDATE, instruction));
	}

}