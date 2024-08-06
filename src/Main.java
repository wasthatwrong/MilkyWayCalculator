import java.time.Instant;
import java.time.ZoneId;

public class Main {
	
	// current location in degrees
	static double LAT = 40.80518;
	static double LONG = -73.71100;
	
	public static void main(String[] args) {
		
		// current UTC time
		Instant now = Instant.now();
		
		// Object at/near the center of the great rift 
		CelestialObject shaula = new CelestialObject("Shaula", -37.103889, 17.5602777778);
		
		// sets observation time to current time
		shaula.setUTCTime(now);
		
		
		for(int i = 0; i < 24*100; i+=24) {
			System.out.print(shaula.getUTCTime().atZone(ZoneId.of("US/Eastern")) + ":\n\t"  + shaula.getAltitude() + " degrees, ");
			if(!shaula.isVisible())
				System.out.print("not ");
			
			System.out.println("visible.\n");
			shaula.incrementHour(24);
		}
	}
}
