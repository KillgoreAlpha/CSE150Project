package nachos.threads;

import nachos.ag.BoatGrader;

public class Boat {
	static BoatGrader bg;
	static boolean done;
	static boolean boatOnOahu; 	// The location of the boat is stored in a boolean
	static int childrenInOahu;
	static int adultsInOahu;
	static Lock lock;
	static Condition2 oahuCv;	// Wakes those on Oahu when the boat arrives there
	static Condition2 molokaiCv; // Wakes those on Molokai when the boat arrives there
	static Condition2 boatCv; 	// Wakes children on a boat when ready to ride
	static int[] onBoat;

	public static boolean boatEmpty(){
		return onBoat[0] == 0 && onBoat[1] == 0;
	}

	public static boolean boatFitsChild(){
		return onBoat[0] < 2 && onBoat[1] == 0;
	}

	public static void selfTest() {
		BoatGrader b = new BoatGrader();

		// System.out.println("\n ***Testing Boats with only 2 children***");
		// begin(0, 3, b);

		System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		begin(1, 2, b);

		// System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		// begin(3, 3, b);
	}

	public static void begin(int adults, int children, BoatGrader b) {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here
		done = false;
		boatOnOahu = true;
		lock = new Lock();
		oahuCv = new Condition2(lock);
		molokaiCv  = new Condition2(lock);
		boatCv = new Condition2(lock);
		onBoat = new int[]{0, 0};
		childrenInOahu = children;
		adultsInOahu = adults;



		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.
		for(int i = 0; i < children; i++){
			Runnable r = new Runnable() {
				public void run() { ChildItinerary(); }
			};

			KThread t = new KThread(r);
			t.setName("Child " + i);
			t.fork();
		}

		for(int i = 0; i < adults; i++){
			Runnable r = new Runnable() {
				public void run() { AdultItinerary(); }
			};

			KThread t = new KThread(r);
			t.setName("Adult " + i);
			t.fork();
		}

		// Ensures the code is not ended prematurely by yielding the main thread
		// Until execution is done
		while(!done){
			KThread.yield();
		}
	}

	static void AdultItinerary() {
		/*
		 * This is where you should put your solutions. Make calls to the
		 * BoatGrader to show that it is synchronized. For example:
		 * 		bg.AdultRowToMolokai(); 
		 * indicates that an adult has rowed the boat across to Molokai
		 */
		boolean onOahu = true; // Initialize the location
		while(true) {
			if (!onOahu)
				KThread.finish(); // Adults on Molokai have nothing to do.

			lock.acquire();
			// Sleeps if the boat isn't in oahu, the boat can’t fit an adult, or if there are 2
			// children in oahu and an adult shouldn't board.
			if (!boatOnOahu || !boatEmpty() || childrenInOahu > 1) {
				oahuCv.sleep();
			} else {
				onOahu = boardAdult(onOahu);
			}

			lock.release();
		}
	}

	static boolean boardAdult(boolean onOahu){
		onBoat[1] ++;
		// Check the location to figure out which one to use
		if(onOahu) {
			bg.AdultRowToMolokai();
		} else {
			bg.AdultRowToOahu();
		}

		transportBoat(0, 1, onOahu);

		return !onOahu;
	}

	static void ChildItinerary() {
		boolean onOahu = true;
		while(true) {
			// System.out.println(done);
			// System.out.println(childrenInOahu);
			if (done) KThread.finish();
			lock.acquire();

			if (!onOahu){
				if (boatOnOahu || !boatFitsChild()) {
					molokaiCv.sleep();
				} else {
					// Only one child may board from Molokai
					onOahu = boardChild(onOahu, false);
				}
			}
			else {
				if (adultsInOahu != 0 && childrenInOahu == 1 || !boatOnOahu || !boatFitsChild())
					oahuCv.sleep();
				else {
					// Two children have to board from Oahu, unless there is only one
					// child left, in which case this child doesn’t wait for another.

					onOahu = boardChild(onOahu, childrenInOahu > 1);
				}
			}

			lock.release();
		}

	}

	static boolean boardChild(boolean onOahu, boolean waitForAnother){
		boolean empty = boatEmpty();
		onBoat[0]++;

		// Waits for a second rider if necessary
		// The first child to board would be the rider, not the pilot
		// This cv is only for these two children. We don’t want to wake other islanders who are
		// only waiting for the boat to return and for them to board.
		if(empty && waitForAnother) {
			// System.out.println("Sleeping B");
			boatCv.sleep();
			// We can check which one to use based on the current location of the children
			if(onOahu){
				bg.ChildRideToMolokai();
			} else {
				bg.ChildRideToOahu();
			}
			// We have to call this here, otherwise we cannot guarantee transportBoat will be
			// called before the other child reacquires the lock.
			// System.out.println("Boat transported to Oahu?, " + !onOahu);
			transportBoat(2, 0, onOahu);
		} else {
			if(onOahu){
				bg.ChildRowToMolokai();
			} else {
				bg.ChildRowToOahu();
			}

			boatCv.wake();
			// System.out.println("Wake B");

			if(waitForAnother) {
				// If we are taking two riders, we simply let the other rider call transportBoat and
				// relinquish control.
				if(onOahu) {
					// System.out.println("Sleeping M");
					molokaiCv.sleep();
					// System.out.println("Wake M");
				} else{
					oahuCv.sleep();
				}

				return !onOahu;
			}
			// This encapsulates that one child traveling to Molokai indicates that we are done.
			if(onOahu) done = true;
			transportBoat(1, 0, onOahu);
		}
		return !onOahu;
	}


	static void transportBoat(int children, int adults, boolean onOahu){
		boolean onOahuNew = !onOahu;
		onBoat[0] = 0;
		onBoat[1] = 0;
		boatOnOahu = !boatOnOahu;
		if(onOahuNew){
			childrenInOahu += children;
			adultsInOahu += adults;
			oahuCv.wakeAll();
		} else{
			childrenInOahu -= children;
			adultsInOahu -= adults;
			molokaiCv.wakeAll();
		}
	}


	static void SampleItinerary() {
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}

}
