// usage: 1. copy this file to Communicator.java. It should be outside of communicator class.
//        2. To use selfTest1, copy "CommSelfTester.selfTest1();" into selfTest() function in ThreadedKernel.java.
//        3. run it.
class CommSelfTester {

	/**
	 * Test with 1 listener then 1 speaker.
	 */
	public static void selfTest1() {
		

		KThread listener1 = new KThread(listenRun);
		listener1.setName("listener1");
		listener1.fork();

		KThread speaker1 = new KThread(speakerRun);
		speaker1.setName("speaker1");
		speaker1.fork();

	} // selfTest1()

	/**
	 * Test with 1 speaker then 1 listener.
	 */
	public static void selfTest2() {

		KThread speaker1 = new KThread(speakerRun);
		speaker1.setName("speaker1");
		speaker1.fork();

		KThread listener1 = new KThread(listenRun);
		listener1.setName("listener1");
		listener1.fork();

	} // selfTest2()

	/**
	 * Test with 2 speakers and 2 listeners intermixed.
	 */
	public static void selfTest3() {

		KThread speaker1 = new KThread(speakerRun);
		speaker1.setName("speaker1");
		speaker1.fork();

		KThread listener1 = new KThread(listenRun);
		listener1.setName("listener1");
		listener1.fork();

		KThread speaker2 = new KThread(speakerRun);
		speaker2.setName("speaker2");
		speaker2.fork();

		KThread listener2 = new KThread(listenRun);
		listener2.setName("listener2");
		listener2.fork();

	} // selfTest3()

	/**
	 * Second test with 2 speakers and 2 listeners intermixed.
	 */
	public static void selfTest4() {

		KThread speaker1 = new KThread(speakerRun);
		speaker1.setName("speaker1");
		speaker1.fork();

		KThread speaker2 = new KThread(speakerRun);
		speaker2.setName("speaker2");
		speaker2.fork();

		KThread listener1 = new KThread(listenRun);
		listener1.setName("listener1");
		listener1.fork();

		KThread listener2 = new KThread(listenRun);
		listener2.setName("listener2");
		listener2.fork();

	} // selfTest4()

	/**
	 * Stress test with 100 speakers and 100 listeners intermixed.
	 */
	public static void selfTest5() {

		for (int i = 0; i < 100; i++) {
			new KThread(speakerRun).setName("Speaker " + Integer.toString(i)).fork();

			new KThread(listenRun).setName("Listen " + Integer.toString(i)).fork();
		}

	} // selfTest5()

	/**
	 * Function to run inside Runnable object listenRun. Uses the function listen on
	 * static object myComm inside this class, allowing the threads inside the
	 * respective selfTests above to call the runnable variables below and test
	 * functionality for listen. Needs to run with debug flags enabled. See NACHOS
	 * README for info on how to run in debug mode.
	 */
	static void listenFunction() {
		Lib.debug(dbgThread, "Thread " + KThread.currentThread().getName() + " is about to listen");

		Lib.debug(dbgThread, "Thread " + KThread.currentThread().getName() + " got value " + myComm.listen());

	} // listenFunction()

	/**
	 * Function to run inside Runnable object speakerRun. Uses the function listen
	 * on static object myComm inside this class, allowing the threads inside the
	 * respective selfTests above to call the runnable variables below and test
	 * functionality for speak. Needs to run with debug flags enabled. See NACHOS
	 * README for info on how to run in debug mode.
	 */
	static void speakFunction() {
		Lib.debug(dbgThread, "Thread " + KThread.currentThread().getName() + " is about to speak");

		myComm.speak(myWordCount++);

		Lib.debug(dbgThread, "Thread " + KThread.currentThread().getName() + " has spoken");
	} // speakFunction()

	/**
	 * Wraps listenFunction inside a Runnable object so threads can be generated for
	 * testing.
	 */
	private static Runnable listenRun = new Runnable() {
		public void run() {
			listenFunction();
		}
	}; // runnable listenRun

	/**
	 * Wraps speakFunction inside a Runnable object so threads can be generated for
	 * testing.
	 */
	private static Runnable speakerRun = new Runnable() {
		public void run() {
			speakFunction();
		}
	}; // Runnable speakerRun

	// dbgThread = 't' variable needed for debug output
	private static final char dbgThread = 't';
	// myComm is a shared object that tests Communicator functionality
	private static Communicator myComm = new Communicator();
	// myWordCount is used for selfTest5 when spawning listening/speaking threads
	private static int myWordCount = 0;

} // CommSelfTester class
