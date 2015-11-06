/*
 * Main: The entry point of UiDroid static analysis module 
 */

package uiDroid;

import java.io.IOException;

import app.IfStmtInstrument;
import app.MyTest;

public class UiDroidMain {
	public static void main(String[] args) {
		if (args[0].equals("instrument")) {
			IfStmtInstrument.instrumentMain(args);
		} else if (args[0].equals("factor")) {
			String[] mainArgs = new String[args.length - 1];
			for (int i = 1; i < args.length; i++) {
				mainArgs[(i - 1)] = args[i];
			}
			try {
				MyTest.main(mainArgs);
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
