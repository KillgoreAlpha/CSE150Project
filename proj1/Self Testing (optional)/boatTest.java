// Add more tests to Boat.selfTest() in the Boat class with different number of adults and chidren to see how boat behaves respectively.
// Invoke Boat.selfTest() from ThreadedKernel.selfTest()

public static void selfTest() {
		BoatGrader b = new BoatGrader();

		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);

		System.out.println("\n ***Testing Boats with only 100 children***");
		begin(0, 100, b);

		System.out.println("\n ***Testing Boats with 2 children, 100 adults***");
		begin(100, 2, b);


		// Implement more cases here ...

		
 }