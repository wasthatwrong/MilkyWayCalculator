import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CelestialObject {
	
	// current observation location in degrees
	private static double LAT = 40.80518; // Latitude
	private static double LONG = -73.71100; // Longitude
	
	// Current time
	private static Instant UTCTime; // UTC clock time of observation
	private static double LAST; // Local absolute sidereal time (using above coordinates)
	
	// List of all CelestialObjects, used to calculate their altitudes whenever observation time/position changes
	private static List<CelestialObject> allObjects;
	
	private double DEC; // Declination of object in degrees
	private double RA;; // Right ascension in hours (decimal format)
	private String name; // Name of celestial object
	
	private double azimuth; // Azimuth in degrees
	private double altitude; // Altitude in sky in degrees
	
	
	/**
	 * Sets name, declination, and right ascension. Sets UTCTime to current time if it doesn't exist. Generates altitude in the sky.
	 * 
	 * @param name
	 * @param DEC
	 * @param RA
	 */
	public CelestialObject(String name, double DEC, double RA) {
		setName(name);
		setDEC(DEC);
		setRA(RA);
		
		// Creates list of all objects if it doesn't exist
		if(allObjects == null) {
			allObjects = new ArrayList<>();
		}
		
		// Adds this object to the list
		allObjects.add(this);
		
		if(UTCTime == null) {
			System.out.println("Automatically generating UTCTime.");
			
			setUTCTime(Instant.now());
		} else {
			calculateAltitude();
		}
	}
	
	/**
	 * Gets current observation time in UTC time
	 * 
	 * @return Instant UTCTime
	 */
	public Instant getUTCTime() {
		return UTCTime;
	}
	
	public void incrementHour(int amount) {
		// Increments UTCTime by amount (hours)
		UTCTime = UTCTime.plusSeconds(3600*amount);
		
		// Increments local absolute solar time by slightly more than 1hr per hour to account for the difference between sidereal and solar days
		LAST = (LAST+amount*(24/23.9344696))%24;
		
		// calculates new altitude with new observation time for every CelestialObject
		for(CelestialObject object : allObjects) {
			object.calculateAltitude();
		}
	}
	
	/**
	 * Sets UTC time to the paramter, retrieves local sidereal time from the US navy, and calculates the altitude  for this object.
	 * 
	 * @param UTCTime
	 * @return 
	 */
	public static void setUTCTime(Instant UTCTime) {
		CelestialObject.UTCTime = UTCTime;
		
		getLAST();
		
		// Calcualtes altitude for all CelestialObjects using the retrieved LAST
		for(CelestialObject object : allObjects) 
			object.calculateAltitude();
	}
	
	/**
	 * Gets local absolute sidereal time from US Navy. Only needs to be called once. 
	 */
	private static void getLAST() {
		
		// Used to put in the API call
		String date = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(UTCTime);
		int hour = UTCTime.atZone(ZoneOffset.UTC).getHour();
		int minute = UTCTime.atZone(ZoneOffset.UTC).getMinute();
		int second = UTCTime.atZone(ZoneOffset.UTC).getSecond();
		
		String urlString = String.format("https://aa.usno.navy.mil/api/siderealtime?date=%s&time=%s:%s:%s&coords=%s,%s&reps=1&intv_mag=1&intv_unit=minutes", date, hour, minute, second, LAT, LONG);
		HttpURLConnection urlConnection;
		
		// API result to parse
		String result = "";
		
		try {
			URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
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
		
		// Gets index of the LAST
		int lastIndex = result.indexOf("\"last\":");
		int valueStart = result.indexOf("\"", lastIndex + 7) + 1;
		int valueEnd = result.indexOf("\"", valueStart);
		String lastValue = result.substring(valueStart, valueEnd);
		
		Scanner scnr = new Scanner(lastValue);
		scnr.useDelimiter(":");
		
		// Converts time from hh:mm:ss into decimal format
		LAST = scnr.nextDouble();
		LAST += scnr.nextDouble() / 60;
		LAST += scnr.nextDouble() / 3600;
		scnr.close();
		
		System.out.println(LAST);
	}

	public void setDEC(double dEC) {
		DEC = dEC;
	}

	public void setRA(double rA) {
		RA = rA;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	private double calculateAltitude() {
		
		// Calculates hour angle of the object
		double H = (LAST - RA) * (360 / 24);
		
		// Calculates altitude of the object in the sky
		altitude = Math.asin(Math.sin(LAT * Math.PI / 180) * Math.sin(DEC * Math.PI / 180) + Math.cos(LAT * Math.PI / 180) * Math.cos(DEC * Math.PI / 180) * Math.cos((H * Math.PI) / 180));
		
		altitude = altitude*180/Math.PI;
		
		return altitude;
	}
	
	public double getAltitude() {
		return altitude;
	}
	
	
	
	public boolean isVisible() {
		return altitude > 0;
	}
}
