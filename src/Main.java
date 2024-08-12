public class Main {

	public static void main(String[] args) {

		// Group of all celestial objects to calculate locations
		CelestialObjectList celestialObjects = new CelestialObjectList();
		
		// Observation location
		double LAT = 40.75; // Latitude
		double LONG = -73.5; // Longitude

		celestialObjects.setCoordinates(LAT, LONG);

		celestialObjects.addCelestialObject("Shaula", 17 + 35.0 / 60 + 17.6 / 3600, -(37 + 7.0 / 60 + 22.7 / 3600)); // Center of milky way
		celestialObjects.addCelestialObject("Northern Coalsack", 20.6280277778, 42.2732222222); // End of milky way

		System.out.println(celestialObjects.toString());

		// For testing accuracy at future dates	
		/*for (int i = 0; i < 20; i++) {
			System.out.println(celestialObjects.toString());
			celestialObjects.incrementTime(24 * 19000);
		}*/
	}
}
