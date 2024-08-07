public class Main {
	
	public static void main(String[] args) {
		
		double LAT = 40.80518; // Latitude
		double LONG = -73.71100; // Longitude
		
		CelestialObjectList celestialObjects = new CelestialObjectList();
		celestialObjects.setCoordinates(LAT, LONG);
		
		celestialObjects.addCelestialObject("Shaula", 17.5602777778, -37.103889);
		celestialObjects.addCelestialObject("Northern Coalsack", 20.6280277778, 42.2732222222 );
		
		for (int i = 0; i < 20; i++) {
			System.out.println(celestialObjects.toString());
			celestialObjects.incrementTime(24*100000);
		}
	}
}
