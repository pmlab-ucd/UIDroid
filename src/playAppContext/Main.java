package playAppContext;

import java.io.IOException;

public class Main {
	public static void main(String[] args) {
		if (args[0].equals("instrument")) {
			IfStmtInstrument.main(args);
		} else if (args[0].equals("factor")) {
			String[] mainArgs = new String[args.length - 1];
			for (int i = 1; i < args.length; i++) {
				mainArgs[(i - 1)] = args[i];
			}
			try {
				MyTest.myTestMain(mainArgs);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}