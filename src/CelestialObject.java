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
import java.util.Scanner;

public class CelestialObject {

	private double DEC; // in degrees
	private double RA;; // in hours
	private String name; // name of celestial object
	private Instant UTCTime; // UTC clock time
	private double LAST; // Local absolute solar time
	//private double azimuth;
	private double altitude; // In degrees
	
	public Instant getUTCTime() {
		return UTCTime;
	}

	public void incrementHour(int amount) {
		// Increments UTCTime by amount (hours)
		UTCTime = UTCTime.plusSeconds(3600*amount);
		
		// Increments local absolute solar time by slightly more than 1hr per hour to account for the difference between sidereal and solar days
		LAST = (LAST+amount*(24/23.9344696))%24;
		
		// calculates new altitude with new observation time
		calculateAltitude();
	}
	
	public void setUTCTime(Instant UTCTime) {
		this.UTCTime = UTCTime;
		getLAST();
	}

	public CelestialObject(String name, double DEC, double RA) {
		setName(name);
		setDEC(DEC);
		setRA(RA);
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
		altitude = Math.asin(Math.sin(Main.LAT * Math.PI / 180) * Math.sin(DEC * Math.PI / 180) + Math.cos(Main.LAT * Math.PI / 180) * Math.cos(DEC * Math.PI / 180) * Math.cos((H * Math.PI) / 180));
		
		altitude = altitude*180/Math.PI;
		
		return altitude;
	}
	
	public double getAltitude() {
		return altitude;
	}
	
	private void getLAST() {
		
		// Used to put in the API call
		String date = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(UTCTime);
		int hour = UTCTime.atZone(ZoneOffset.UTC).getHour();
		int minute = UTCTime.atZone(ZoneOffset.UTC).getMinute();
		int second = UTCTime.atZone(ZoneOffset.UTC).getSecond();
		
		String urlString = String.format("https://aa.usno.navy.mil/api/siderealtime?date=%s&time=%s:%s:%s&coords=%s,%s&reps=1&intv_mag=1&intv_unit=minutes", date, hour, minute, second, Main.LAT, Main.LONG);
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
		
		// Calculates the altitude with this sidereal time
		calculateAltitude();
	}
	
	public boolean isVisible() {
		return altitude > 0;
	}
}
