/*
 * Main: The entry point of UiDroid static analysis module 
 */

package uiDroid;

import java.io.IOException;
import app.IfStmtInstrument;

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
				UiDroidTest.myTestMain(mainArgs);
			} catch (IOException ioExcept) {
				ioExcept.printStackTrace();
			} catch (InterruptedException interruptExcept) {
				interruptExcept.printStackTrace();
			}
		}
	}

}
