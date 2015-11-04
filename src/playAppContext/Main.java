package playAppContext;

import java.io.IOException;

public class Main {
	public static void main(String[] args) throws IOException,
			InterruptedException {
		if (args[0].equals("instrument")) {
			IfStmtInstrument.instrumentMain(args);
		} else if (args[0].equals("factor")) {
			String[] mainArgs = new String[args.length - 1];
			for (int i = 1; i < args.length; i++) {
				mainArgs[(i - 1)] = args[i];
			}
			MyTest.myTestMain(mainArgs);
		}
	}
}