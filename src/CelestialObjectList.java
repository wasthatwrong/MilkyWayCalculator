import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalField;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;

/**
 * Searchable list of celestial objects that share an observation location, and
 * observation time. Useful for keeping observation data syncronized between
 * multiple celestial objects.
 * 
 * @author Bradley
 *
 */
public class CelestialObjectList {

	// Allows for searching of objects by name to retrieve their position in the sky.
	private Map<String, CelestialObject> celestialObjects = new HashMap<>();

	// Current observation coordinates in degrees (decimal format)
	private double LAT; // Latitude (-90 to 90)
	private double LONG; // Longitude (-180 to 180)

	// Time of observation
	private Instant UTCTime;
	private double localMeanSiderealTime; // Sidereal time of observation location in decimal format (0-24).
	private TimeZone timezone = TimeZone.getTimeZone("UTC"); // Default timezone is UTC at 0,0

	/**
	 * Creates a celestial object list with the observation time as the parameter,
	 * and the location as 0,0. Does not set timezone.
	 * 
	 * @param UTC time to initialize to
	 */
	public CelestialObjectList(Instant UTCTime) {
		this.UTCTime = UTCTime;
		initializeLocalMeanSiderealTime();
	}

	/**
	 * Creates a celestial object list with the observation time as the current
	 * system time, and location as 0,0.
	 */
	public CelestialObjectList() {
		System.out.println("Automatically generating UTCTime.");
		this.UTCTime = Instant.now();
		initializeLocalMeanSiderealTime();
	}

	/**
	 * Adds a new object to the map to share its observation location and time with
	 * other CelestialObjects.
	 * 
	 * @param name - name of the object, used as the key in the map
	 * @param RA   - right ascension
	 * @param DEC  - declination
	 */
	public void addCelestialObject(String name, double RA, double DEC) {
		celestialObjects.put(name, new CelestialObject(name, RA, DEC));
	}

	/**
	 * Sets observation latitude and longitude in decimal format, and sets all
	 * object positions to be out of sync.
	 * 
	 * @param LAT  - latitude
	 * @param LONG - longitude
	 */
	public void setCoordinates(double LAT, double LONG) {

		// Changes sidereal time only if new longitude differs from old longitude
		if (Double.compare(this.LONG, LONG) != 0) {
			double deltaSiderealTime = (LONG - this.LONG) / 15.0; // Difference in sidereal time in hours, where 1hr = 15 degrees of longitud
			localMeanSiderealTime = (localMeanSiderealTime + deltaSiderealTime) % 24; // Sets new sidereal time at current location

			// If result from modulus is negative, convert to positive number.
			if (localMeanSiderealTime < 0)
				localMeanSiderealTime += 24;

			this.LONG = LONG;
		}

		this.LAT = LAT;

		// API call to get time zone for new location
		String urlString = String.format("https://timeapi.io/api/TimeZone/coordinate?latitude=%s&longitude=%s", LAT, LONG);

		// API result to parse
		String result = "";

		// Parses the result into a string
		try {
			URL url = new URL(urlString);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.connect();

			InputStream inputStream = urlConnection.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

			String s;
			while ((s = reader.readLine()) != null)
				result += s;

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Gets timezone ID from result
		int valueEnd = result.indexOf("\"", 13);
		String localTimeZone = result.substring(13, valueEnd);

		// Sets timezone to the correct ID
		timezone.setID(localTimeZone);

		// Sets all objects' positions to not be in sync with observation time/position
		for (CelestialObject object : celestialObjects.values())
			object.positionSyncedWithObservationTime = false;
	}

	/**
	 * Sets UTC time and local sidereal time for the current location, and sets all
	 * objects' positions to not be in sync with observation time/position.
	 * 
	 * @param UTCTime - new observation time
	 */
	public void setUTCTime(Instant UTCTime) {
		// Calculates the difference between old and new time in hours
		Duration duration = Duration.between(this.UTCTime, UTCTime);
		long hours = duration.toHours();

		incrementTime(hours);
	}

	/**
	 * Increments both the UTC time and local mean sidereal time by the number of
	 * hours from the argument.
	 * 
	 * @param amount - amount of hours to increment the observation time by
	 */
	public void incrementTime(double amount) {
		// Increments UTCTime by amount (hours)
		UTCTime = UTCTime.plusSeconds((long) (3600 * amount));

		// Increments local mean sidereal time by slightly more than 1hr per hour to
		// account for the difference between sidereal and solar days
		localMeanSiderealTime = (localMeanSiderealTime + amount * 1.002737909350795) % 24;

		// Sets all objects' positions to not be in sync with observation time/position
		for (CelestialObject object : celestialObjects.values())
			object.positionSyncedWithObservationTime = false;
	}

	/**
	 * Gets local mean sidereal time from US Navy. Only needs to be called once.
	 */
	private void initializeLocalMeanSiderealTime() {

		// Separating hour, minute, and second from UTC time
		int hour = UTCTime.atZone(ZoneOffset.UTC).getHour();
		int minute = UTCTime.atZone(ZoneOffset.UTC).getMinute();
		int second = UTCTime.atZone(ZoneOffset.UTC).getSecond();

		try {
			// Used to put date & time in the API call
			String date = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(UTCTime);

			// Builds the body
			String urlString = String.format("https://aa.usno.navy.mil/api/siderealtime?date=%s&time=%s:%s:%s&coords=%s,%s&reps=1&intv_mag=1&intv_unit=minutes", date, hour, minute, second, LAT, LONG);

			// API result to parse
			String result = "";

			URL url = new URL(urlString);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.connect();

			InputStream inputStream = urlConnection.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

			String s;
			while ((s = reader.readLine()) != null)
				result += s;

			// Retrieves local absolute sidereal time from result
			int lastIndex = result.indexOf("\"last\":");
			int valueStart = result.indexOf("\"", lastIndex + 7) + 1;
			int valueEnd = result.indexOf("\"", valueStart);

			String siderealTime = result.substring(valueStart, valueEnd);

			Scanner scnr = new Scanner(siderealTime);
			scnr.useDelimiter(":");

			// Converts time from hh:mm:ss into decimal format
			localMeanSiderealTime = scnr.nextDouble();
			localMeanSiderealTime += scnr.nextDouble() / 60;
			localMeanSiderealTime += scnr.nextDouble() / 3600;
			scnr.close();

		} catch (Exception e) {
			System.out.println("Invalid result from API. Calculating sidereal time locally (may slightly affect accuracy).");

			// Getting julian day from UTC time (doesn't include fraction of the day)
			LocalDate today = UTCTime.atZone(ZoneOffset.UTC).toLocalDate();
			long julianDay = today.getLong(JulianFields.JULIAN_DAY);

			// Julian date (includes fraction of the day)
			double julianDate = julianDay + (hour + minute / 60.0 + second / 3600.0 - 12.0) % 24 / 24;

			// Number of julian centuries since J2000, Jan 1, 2000, 12:00 Terrestrial Time
			double t = (julianDate - 2451545.0) / 36525.0;

			double greenwichMeanStandardTime = 280.46061837 + 360.98564736629 * (julianDate - 2451545.0) + 0.000387933 * t * t - t * t * t / 38710000.0;

			greenwichMeanStandardTime = (greenwichMeanStandardTime % 360.0) / 15;

			localMeanSiderealTime = greenwichMeanStandardTime + LONG / 15;

			// Converts LMST to positive number if negative, i.e., -1 will correspond to 23:00
			if (localMeanSiderealTime < 0)
				localMeanSiderealTime += 24;
		}
	}

	/**
	 * Gets the current observation coordinates in decimal format.
	 * 
	 * @return latitude & longitude
	 */
	public String getCoordinates() {
		return LAT + ", " + LONG;
	}

	/**
	 * Gets current observation time in UTC time
	 * 
	 * @return UTCTime
	 */
	public Instant getUTCTime() {
		return UTCTime;
	}

	/**
	 * Gets the mean sidereal time in hours at the current observation location.
	 * 
	 * @return localMbsoluteSiderealTime
	 */
	public double getLocalMbsoluteSiderealTime() {
		return localMeanSiderealTime;
	}

	public CelestialObject getCelestialObject(String name) {
		return celestialObjects.get(name);
	}

	/**
	 * Contains declination, rightAscension, azimuth, altitude, and name of a
	 * celestial object. Calculates the altitude and azimuth from the position and
	 * time in the enclosing class.
	 */
	private class CelestialObject {

		private boolean positionSyncedWithObservationTime;

		private final double declination; // In degrees, (-90 to 90)
		private final double rightAscension;; // Right ascension in hours/decimal format (0-24)
		private final String name; // Name of celestial object

		private double azimuth; // In degrees (0-360)
		private double altitude; // In degrees (-90 to 90)

		/**
		 * Sets name, right ascension, and declination.
		 * 
		 * @param name - name of object
		 * @param RA   - right ascension
		 * @param DEC  - declination
		 */
		public CelestialObject(String name, double RA, double DEC) {
			this.name = name;
			rightAscension = RA;
			declination = DEC;
		}

		public String getName() {
			return name;
		}

		public double getRightAscension() {
			return rightAscension;
		}

		public double getDeclination() {
			return getDeclination();
		}

		private void calculateAltitude() {

			// Calculates hour angle (in degrees) of the object using the sidereal time and right ascension of the object.
			double hourAngle = (localMeanSiderealTime - rightAscension) * (360 / 24);

			// Calculates altitude of the object in the sky
			altitude = Math.sin(Math.toRadians(LAT)) * Math.sin(Math.toRadians(declination));
			altitude += Math.cos(Math.toRadians(LAT)) * Math.cos(Math.toRadians(declination)) * Math.cos(Math.toRadians(hourAngle));
			altitude = Math.asin(altitude);

			// Converts from radians to degrees
			altitude = Math.toDegrees(altitude);

			positionSyncedWithObservationTime = true;
		}

		public double getAltitude() {

			if (!positionSyncedWithObservationTime)
				calculateAltitude();

			return altitude;
		}

		public boolean isVisible() {
			return getAltitude() > 0;
		}

		public String toString() {
			String r = String.format("%s:\n\tAltitude: %s degrees\n\tVisible: ", name, getAltitude());

			if (isVisible())
				r += "Yes";
			else
				r += "No";

			return r;
		}
	}

	public String toString() {
		// Local date/time as header in the timezone of the observation location.
		String r = UTCTime.atZone(ZoneId.of(timezone.getID())) + "\n";
		r += "Location: " + getCoordinates();

		for (CelestialObject object : celestialObjects.values())
			r += "\n\t" + object + "\n";

		return r;
	}
}
