import java.time.Instant;
import java.time.ZoneId;

public class Main {
	
	public static void main(String[] args) {
		
		double LAT = 40.80518; // Latitude
		double LONG = -73.71100; // Longitude
		
		CelestialObject.setLAT(LAT);
		CelestialObject.setLONG(LONG);
		
		// Object at/near the center of the great rift 
		CelestialObject shaula = new CelestialObject("Shaula", -37.103889, 17.5602777778);
		
		for (int i = 0; i < 72; i++) {
			System.out.println(shaula);
			System.out.println();
			CelestialObject.incrementHour(1);
		}
	}
}
