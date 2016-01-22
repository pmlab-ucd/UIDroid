package playAppContext;

import java.io.IOException;

/**
 * @ClassName: Main
 * @Description: 1. ifinstrument对所有branch语句生成fake sink
 *	2. factor: 用flowdroid对fakesink的参数做taint analysis (source是natural env),
 * @author: hao
 * @date: Jan 22, 2016 2:07:07 PM
 */
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