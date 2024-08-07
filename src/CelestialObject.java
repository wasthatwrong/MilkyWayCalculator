import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CelestialObject {

	// current observation location in degrees
	private static double LAT = 40.80518; // Latitude (-90 to 90)
	private static double LONG = -73.71100; // Longitude (-180 to 180)

	private static Instant UTCTime; // UTC time of observation
	private static double localAbsoluteSiderealTime; // Sidereal time in decimal format (0-24)

	private boolean positionSyncedWithObservationTime;

	private double declination; // In degrees, (-90 to 90)
	private double rightAscension;; // Right ascension in hours/decimal format (0-24)

	// List of all CelestialObjects, used to calculate their altitudes whenever
	// observation time/position changes
	private static List<CelestialObject> allObjects = new ArrayList<>();

	private String name; // Name of celestial object
	private double azimuth; // In degrees (0-360)
	private double altitude; // In degrees (-90 to 90)

	/**
	 * Sets UTC time, retrieves local sidereal time from the US navy if
	 * localAbsoluteSiderealTime hasn't been initialized, and calculates the new
	 * altitudes for all CelestialObjects.
	 * 
	 * @param UTCTime - new observation time
	 */
	public static void setUTCTime(Instant UTCTime) {

		// Only retrieves local absolute sidereal time if localAbsoluteSiderealTime
		// hasn't been initialized
		if (localAbsoluteSiderealTime == 0) {
			CelestialObject.UTCTime = UTCTime;
			retrieveLocalAbsoluteSiderealTime();
		} else {
			Duration duration = Duration.between(CelestialObject.UTCTime, UTCTime); // If localAbsoluteSiderealTime isn't 0, then UTCTime cannot be 0.
			long hours = duration.toHours();
			incrementHour(hours);
		}

		// Sets all objects' positions to not be in sync with observation time/position
		for (CelestialObject object : allObjects)
			object.positionSyncedWithObservationTime = false;
	}

	/**
	 * Sets name, declination, and right ascension. Sets UTCTime to current time if
	 * it doesn't exist. Generates altitude in the sky.
	 * 
	 * @param name
	 * @param DEC
	 * @param RA
	 */
	public CelestialObject(String name, double DEC, double RA) {
		setName(name);
		setDEC(DEC);
		setRA(RA);

		if (UTCTime == null) {
			System.out.println("Automatically generating UTCTime.");

			setUTCTime(Instant.now()); // Calculates all new altitudes as well
		} else {
			calculateAltitude();
		}

		allObjects.add(this);
	}

	public static void incrementHour(double amount) {
		// Increments UTCTime by amount (hours)
		UTCTime = UTCTime.plusSeconds((long) (3600 * amount));

		// Increments local absolute solar time by slightly more than 1hr per hour to
		// account for the difference between sidereal and solar days
		localAbsoluteSiderealTime = (localAbsoluteSiderealTime + amount * 1.002737909350795) % 24;

		// Sets all objects' positions to not be in sync with observation time/position
		for (CelestialObject object : allObjects)
			object.positionSyncedWithObservationTime = false;
	}

	public void setDEC(double DEC) {
		this.declination = DEC;
	}

	public void setRA(double RA) {
		this.rightAscension = RA;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private void calculateAltitude() {

		// Calculates hour angle of the object
		double H = (localAbsoluteSiderealTime - rightAscension) * (360 / 24);

		// Calculates altitude of the object in the sky
		altitude = Math.asin(Math.sin(LAT * Math.PI / 180) * Math.sin(declination * Math.PI / 180) + Math.cos(LAT * Math.PI / 180) * Math.cos(declination * Math.PI / 180) * Math.cos((H * Math.PI) / 180));

		altitude = altitude * 180 / Math.PI;
	}

	public double getAltitude() {

		if (!positionSyncedWithObservationTime)
			calculateAltitude();

		return altitude;
	}

	public boolean isVisible() {
		return getAltitude() > 0;
	}

	/**
	 * Gets local absolute sidereal time from US Navy. Only needs to be called once.
	 */
	private static void retrieveLocalAbsoluteSiderealTime() {

		// Used to put date & time in the API call
		String date = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(UTCTime);
		int hour = UTCTime.atZone(ZoneOffset.UTC).getHour();
		int minute = UTCTime.atZone(ZoneOffset.UTC).getMinute();
		int second = UTCTime.atZone(ZoneOffset.UTC).getSecond();

		// Builds the body
		String urlString = String.format("https://aa.usno.navy.mil/api/siderealtime?date=%s&time=%s:%s:%s&coords=%s,%s&reps=1&intv_mag=1&intv_unit=minutes", date, hour, minute, second, LAT, LONG);

		// API result to parse
		String result = "";

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

		// Retrieves local absolute sidereal time from result
		int lastIndex = result.indexOf("\"last\":");
		int valueStart = result.indexOf("\"", lastIndex + 7) + 1;
		int valueEnd = result.indexOf("\"", valueStart);
		String siderealTime = result.substring(valueStart, valueEnd);

		Scanner scnr = new Scanner(siderealTime);
		scnr.useDelimiter(":");

		// Converts time from hh:mm:ss into decimal format
		localAbsoluteSiderealTime = scnr.nextDouble();
		localAbsoluteSiderealTime += scnr.nextDouble() / 60;
		localAbsoluteSiderealTime += scnr.nextDouble() / 3600;
		scnr.close();

		System.out.println(localAbsoluteSiderealTime);
	}

	/**
	 * Sets latitude in decimal format.
	 * 
	 * @param LAT - latitude
	 */
	public static void setLAT(double LAT) {
		CelestialObject.LAT = LAT;

		// Sets all objects' positions to not be in sync with observation time/position
		for (CelestialObject object : allObjects)
			object.positionSyncedWithObservationTime = false;
	}

	/**
	 * Sets longitude in decimal format.
	 * 
	 * @param LONG - longitude
	 */
	public static void setLONG(double LONG) {
		CelestialObject.LONG = LONG;

		// Sets all objects' positions to not be in sync with observation time/position
		for (CelestialObject object : allObjects)
			object.positionSyncedWithObservationTime = false;
	}

	/**
	 * Gets the current observation coordinates in decimal format.
	 * 
	 * @return latitude & longitude
	 */
	public static String getCoordinates() {
		return LAT + ", " + LONG;
	}

	/**
	 * Gets current observation time in UTC time
	 * 
	 * @return Instant UTCTime
	 */
	public static Instant getUTCTime() {
		return UTCTime;
	}

	public String toString() {

		LocalTime local = LocalTime.from(UTCTime.atZone(ZoneId.systemDefault()));
		String r = String.format("%s %s:\n\tAltitude: %s degrees\n\tVisible: ", UTCTime.truncatedTo(ChronoUnit.HOURS), local, getAltitude());

		if (isVisible())
			r += "Yes";
		else
			r += "No";

		return r;

	}
}
