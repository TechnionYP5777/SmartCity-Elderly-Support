package sensors;

/**
 * @author Sharon
 * @author Yarden
 * @since 7.12.16
 */
public abstract class Sensor {
	protected String name;
	protected String id;

	/**
	 * Initializes a new sensor given its name and id.
	 * 
	 * @param name
	 *            name of the sensor
	 * @param id
	 *            id of the sensor
	 */
	public Sensor(final String name, final String id) {
		this.name = name;
		this.id = id;
	}

	/**
	 * Returns the names of the parameters that will be sent to the system.
	 * These names will be used to pass data to the system as a dictionary of
	 * (type, value) tuples.
	 * 
	 * @return array of names of the data this sensor observers
	 */
	protected abstract String[] getObservationsNames();

	/**
	 * @return name of the sensor
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return id of the sensor
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Sets a new name for the sensor.
	 * 
	 * @param name
	 *            new name of the sensor
	 */
	public void setName(final String name) {
		this.name = name;
	}
}
