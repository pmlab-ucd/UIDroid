package uiDroid;

import java.io.File;
import java.io.IOException;

import app.MyTest;

public class UiDroidTest extends MyTest {
	public static void main(String[] args) 
			throws IOException, InterruptedException {
		myTestMain(args);
		File f = new File(
				"/home/hao/workspace/AppContext/Instrument/InstrumentedApp/ApkSamples/app-debug.apk");
		String apkPath = f.getAbsolutePath();
		permissionAnalysis(apkPath, args[2], args[3]);
	}

}
